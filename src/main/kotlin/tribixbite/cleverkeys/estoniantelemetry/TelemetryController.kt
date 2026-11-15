package tribixbite.cleverkeys.estoniantelemetry

import android.content.Context
import android.view.inputmethod.EditorInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.UUID
import tribixbite.cleverkeys.BuildConfig

/** Opt-in, local-only telemetry. Invoke on one serial worker, never on the IME main thread.
 * Every event is separately AES-GCM authenticated; only length framing and segment names are cleartext.
 * Exports are plaintext ONLY to a stream explicitly selected by the user via ACTION_CREATE_DOCUMENT.
 */
class TelemetryController internal constructor(
    context: Context, internal val storeName: String, private val cipher: CipherBox,
    private val replaySizeObserver: (Int) -> Unit = {},
) {
    constructor(context: Context) : this(context, "estonian-swipe", AndroidKeystoreBox("estonian-swipe-telemetry-v1"))

    companion object {
        const val MAX_SWIPES = 5_000
        const val MAX_INPUT_EVENTS = 10_000
        const val MAX_BYTES = 48L * 1024 * 1024
        private const val SEGMENT_BYTES = 1024 * 1024L
        private const val MAX_EVENT_BYTES = 128 * 1024
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }

    init { require(storeName.matches(Regex("[a-zA-Z0-9_-]{1,64}"))) }
    private val directory = context.applicationContext.noBackupFilesDir
    private val file = File(directory, "$storeName.segments")
    private val legacy = File(directory, "$storeName.aes")
    private val consent = File(directory, "$storeName.enabled")
    private val manualConsent = File(directory, "$storeName.manual.enabled")
    private val inputConsent = File(directory, "$storeName.input.enabled")
    private val revoked = File(directory, "$storeName.revoked")
    private val clearing = File(directory, "$storeName.clearing")
    private val records = LinkedHashMap<String, JSONObject>()
    private val owners = HashMap<String, Long>()
    private val manualRecords = LinkedHashMap<String, JSONObject>()
    private val manualOwners = HashMap<String, Long>()
    private val inputRecords = ArrayDeque<JSONObject>()
    private val inputOwners = ArrayDeque<Long>()
    private var healthy = true
    var isEnabled: Boolean = false
        private set
    var isManualEnabled: Boolean = false
        private set
    var isInputEnabled: Boolean = false
        private set

    init {
        try {
            check(!clearing.exists() && !revoked.exists() && !legacy.exists()) {
                "Incomplete deletion, revoked consent, or legacy store"
            }
            if (file.exists()) {
                check(file.isDirectory)
                val segments = segments()
                check(segments.sumOf { it.length() } <= MAX_BYTES)
                segments.forEachIndexed { index, segment -> replay(segment, index == segments.lastIndex) }
                trim()
            }
            isEnabled = consent.exists()
            isManualEnabled = manualConsent.exists()
            isInputEnabled = inputConsent.exists()
        } catch (_: Exception) {
            healthy = false
            isEnabled = false
            isManualEnabled = false
            isInputEnabled = false
            records.clear()
            owners.clear()
            manualRecords.clear()
            manualOwners.clear()
            inputRecords.clear(); inputOwners.clear()
        }
    }

    @Synchronized fun enable(): Boolean {
        if (!healthy || clearing.exists() || revoked.exists() || legacy.exists()) return false
        return try {
            cipher.seal(byteArrayOf()).fill(0)
            if (!consent.exists()) consent.writeText("1")
            isEnabled = true
            true
        } catch (_: Exception) { isEnabled = false; false }
    }

    @Synchronized fun disable(): Boolean {
        isEnabled = false
        isManualEnabled = false
        isInputEnabled = false
        // Persist revocation BEFORE deleting either consent. A failed deletion must not
        // resurrect collection when a new process sees an old consent marker.
        val tombstoned = try {
            RandomAccessFile(revoked, "rw").use { out ->
                out.setLength(0)
                out.writeByte(1)
                out.fd.sync()
            }
            true
        } catch (_: Exception) { false }
        val swipeRemoved = runCatching { !consent.exists() || consent.delete() }.getOrDefault(false)
        val manualRemoved = runCatching { !manualConsent.exists() || manualConsent.delete() }.getOrDefault(false)
        val inputRemoved = runCatching { !inputConsent.exists() || inputConsent.delete() }.getOrDefault(false)
        if (swipeRemoved && manualRemoved && inputRemoved) {
            val cleared = !revoked.exists() || revoked.delete()
            if (!tombstoned || !cleared) healthy = false
            return tombstoned && cleared
        }
        healthy = false
        return false // The durable tombstone, when writable, blocks collection on restart.
    }

    /** Independent affirmative opt-in; never turns on swipe collection. */
    @Synchronized fun enableManual(): Boolean {
        if (!healthy || clearing.exists() || revoked.exists() || legacy.exists()) return false
        return try {
            cipher.seal(byteArrayOf()).fill(0)
            if (!manualConsent.exists()) manualConsent.writeText("1")
            isManualEnabled = true
            true
        } catch (_: Exception) { isManualEnabled = false; false }
    }

    /** Third independent opt-in, OFF until explicitly enabled. */
    @Synchronized fun enableInputEvents(): Boolean {
        if (!healthy || clearing.exists() || revoked.exists() || legacy.exists()) return false
        return try {
            cipher.seal(byteArrayOf()).fill(0)
            if (!inputConsent.exists()) inputConsent.writeText("1")
            isInputEnabled = true
            true
        } catch (_: Exception) { isInputEnabled = false; false }
    }

    /** One encrypted frame and one fsync per validated batch. */
    @Synchronized fun recordInputBatch(events: List<ViipioInputEvent>, editor: EditorInfo?, dropped: Long = 0): Boolean {
        if (!isInputEnabled || !healthy || !TelemetryPolicy.editor(editor) ||
            events.isEmpty() || events.size > 64 || dropped < 0 || dropped > 1_000_000_000 ||
            events.any { !InputEventPolicy.valid(it) } ||
            events.zipWithNext().any { (a, b) -> a.sessionId != b.sessionId ||
                a.seq >= b.seq || a.relativeTimeMs > b.relativeTimeMs }) return false
        val rows = JSONArray()
        val now = System.currentTimeMillis()
        events.forEach { e -> rows.put(JSONObject().put("schema", "viipio-input-v1")
            .put("sessionId", e.sessionId).put("seq", e.seq).put("timeMs", e.relativeTimeMs)
            .put("lang", e.language.lowercase(java.util.Locale.ROOT))
            .put("kind", e.kind.wire).put("text", e.text).put("textLength", e.textLength)
            .put("source", e.source.wire).put("outcome", e.outcome.wire).put("createdMs", now)) }
        return append(JSONObject().put("type", "input-batch").put("rows", rows).put("dropped", dropped))
    }

    /** Caller must verify complete ET word readback from its own key taps. A direct undo
     * link requires both consents and a same-field swipe with verified undo.
     */
    @Synchronized fun recordManualWord(
        word: String, language: String, editor: EditorInfo?, source: ManualWordSource,
        directUndoSwipeId: String? = null,
    ): Boolean {
        if (!isManualEnabled || !healthy || source != ManualWordSource.VERIFIED_KEY_TYPED ||
            !TelemetryPolicy.language(language) || !TelemetryPolicy.word(word) ||
            !TelemetryPolicy.editor(editor)) return false
        val provenSwipeId = directUndoSwipeId?.takeIf { candidateId ->
            val swipe = matchingRow(candidateId, "et", editor)
            if (swipe == null || !swipe.optBoolean("undoObserved") ||
                swipe.optString("finalWord") != word || swipe.has("deleted") ||
                manualRecords.values.any { it.optString("swipeId") == candidateId }) false
            else {
                val corrections = swipe.getJSONArray("corrections")
                corrections.length() > 0 && corrections.getJSONObject(corrections.length() - 1)
                    .let { it.optString("kind") == "manual" && it.optString("to") == word }
            }
        }
        val row = JSONObject().put("schema", "estonian-manual-v1")
            .put("id", UUID.randomUUID().toString()).put("lang", "et")
            .put("word", word).put("createdMs", System.currentTimeMillis())
        if (provenSwipeId != null) row.put("swipeId", provenSwipeId)
        return append(JSONObject().put("type", "manual").put("row", row))
    }

    @Synchronized fun recordSwipe(trace: SwipeTrace): Boolean {
        if (!isEnabled || !healthy || !TelemetryPolicy.trace(trace) || records.containsKey(trace.id)) return false
        val row = JSONObject().apply {
            put("schema", if (trace.decoderSource == null) "estonian-swipe-v1" else "estonian-swipe-v2")
            put("id", trace.id)
            put("lang", "et")
            put("pkg", trace.editor!!.packageName)
            put("field", trace.editor.fieldId)
            put("createdMs", System.currentTimeMillis())
            put("widthPx", trace.geometry.widthPx)
            put("heightPx", trace.geometry.heightPx)
            put("keys", JSONArray().apply { trace.geometry.keys.forEach { k ->
                put(JSONObject().put("label", k.label).put("left", k.left).put("top", k.top)
                    .put("right", k.right).put("bottom", k.bottom))
            } })
            put("points", JSONArray().apply { trace.points.forEach { p ->
                put(JSONObject().put("x", p.x.toDouble()).put("y", p.y.toDouble())
                    .put("timeMs", p.uptimeMs - trace.startedUptimeMs))
            } })
            put("durationMs", trace.committedUptimeMs - trace.startedUptimeMs)
            put("candidates", JSONArray(trace.candidates))
            if (trace.decoderSource != null && trace.candidateScores != null) {
                put("decoder", trace.decoderSource.wireName)
                put("candidateScores", JSONArray(trace.candidateScores))
                put("appVersion", BuildConfig.VERSION_NAME)
            }
            put("committed", trace.committed)
            put("corrections", JSONArray())
        }
        if (!append(JSONObject().put("type", "swipe").put("row", row))) return false
        trim()
        return true
    }

    /** A verified owned-word deletion. This is not yet a final deletion: a replacement may follow. */
    @Synchronized fun recordUndo(id: String, language: String, editor: EditorInfo?): Boolean {
        val row = matchingRow(id, language, editor) ?: return false
        if (row.has("finalWord") || row.has("deleted") || row.optBoolean("undoObserved")) return false
        return append(JSONObject().put("type", "undo").put("id", id))
    }

    @Synchronized fun recordCorrection(
        id: String, language: String, editor: EditorInfo?, from: String, to: String, kind: String,
    ): Boolean {
        val row = matchingRow(id, language, editor) ?: return false
        if (!TelemetryPolicy.word(from) || !TelemetryPolicy.word(to) ||
            kind !in setOf("candidate", "autocorrect", "manual", "backspace") ||
            row.has("finalWord") || row.has("deleted")) return false
        val corrections = row.getJSONArray("corrections")
        val current = if (corrections.length() == 0) row.getString("committed")
            else corrections.getJSONObject(corrections.length() - 1).getString("to")
        if (current != from || from == to || corrections.length() >= 20) return false
        return append(JSONObject().put("type", "correction").put("id", id)
            .put("from", from).put("to", to).put("kind", kind))
    }

    @Synchronized fun recordFinalization(id: String, language: String, editor: EditorInfo?, finalWord: String?): Boolean {
        val row = matchingRow(id, language, editor) ?: return false
        if (row.has("finalWord") || row.has("deleted") ||
            (finalWord != null && !TelemetryPolicy.word(finalWord))) return false
        val corrections = row.getJSONArray("corrections")
        val current = if (corrections.length() == 0) row.getString("committed")
            else corrections.getJSONObject(corrections.length() - 1).getString("to")
        if (finalWord != null && current != finalWord) return false
        return append(JSONObject().put("type", "finalization").put("id", id)
            .put("deleted", finalWord == null).apply { if (finalWord != null) put("finalWord", finalWord) })
    }

    @Synchronized fun stats(): TelemetryStats {
        trim()
        val rows = records.values
        return TelemetryStats(isEnabled, rows.size, rows.count { it.has("finalWord") || it.has("deleted") },
            rows.sumOf { it.getJSONArray("corrections").length() }, isManualEnabled, manualRecords.size,
            isInputEnabled, inputRecords.size)
    }

    @Synchronized fun exportJsonl(output: OutputStream) {
        check(healthy) { "Encrypted telemetry unavailable" }
        trim()
        records.values.forEach { stored ->
            val exported = JSONObject(stored.toString())
            exported.remove("pkg")
            exported.remove("field")
            exported.remove("createdMs")
            output.write(exported.toString().toByteArray(StandardCharsets.UTF_8))
            output.write('\n'.code)
        }
        manualRecords.values.forEach { stored ->
            val exported = JSONObject(stored.toString())
            exported.remove("createdMs")
            output.write(exported.toString().toByteArray(StandardCharsets.UTF_8))
            output.write('\n'.code)
        }
        inputRecords.forEach { stored ->
            val exported = JSONObject(stored.toString())
            exported.remove("createdMs")
            output.write(exported.toString().toByteArray(StandardCharsets.UTF_8))
            output.write('\n'.code)
        }
        output.flush()
    }

    /** Tombstone first: failed deletion cannot silently resume collection after restart. */
    @Synchronized fun clear(): Boolean {
        isEnabled = false
        isManualEnabled = false
        isInputEnabled = false
        return try {
            if (!clearing.exists()) clearing.writeText("1")
            healthy = false
            check(disable())
            if (file.exists()) {
                check(file.isDirectory)
                file.listFiles()?.forEach { check(it.isFile && it.delete()) } ?: error("Cannot list telemetry")
                check(file.delete())
            }
            if (legacy.exists()) check(legacy.delete())
            cipher.destroy()
            check(clearing.delete())
            records.clear()
            owners.clear()
            manualRecords.clear()
            manualOwners.clear()
            healthy = true
            inputRecords.clear(); inputOwners.clear()
            true
        } catch (_: Exception) { healthy = false; false }
    }

    internal fun encryptedFileForTest(): File = file

    private fun matchingRow(id: String, language: String, editor: EditorInfo?): JSONObject? {
        if (!isEnabled || !healthy || !TelemetryPolicy.id(id) || !TelemetryPolicy.language(language) ||
            !TelemetryPolicy.editor(editor)) return null
        trim()
        return records[id]?.takeIf { it.optString("pkg") == editor!!.packageName && it.optInt("field") == editor.fieldId }
    }

    private fun trim() {
        val now = System.currentTimeMillis()
        val iterator = records.entries.iterator()
        while (iterator.hasNext()) {
            val row = iterator.next().value
            val age = now - row.optLong("createdMs", 0)
            if (age !in 0L..RETENTION_MS) { owners.remove(row.getString("id")); iterator.remove() }
        }
        while (records.size > MAX_SWIPES) {
            val first = records.keys.first()
            records.remove(first)
            owners.remove(first)
        }
        val manuals = manualRecords.entries.iterator()
        while (manuals.hasNext()) {
            val row = manuals.next().value
            if (now - row.optLong("createdMs", 0) !in 0L..RETENTION_MS) {
                manualOwners.remove(row.getString("id")); manuals.remove()
            }
        }
        while (manualRecords.size > MAX_SWIPES) {
            val first = manualRecords.keys.first()
            manualRecords.remove(first)
            manualOwners.remove(first)
        }
        while (inputRecords.isNotEmpty() && now - inputRecords.first().optLong("createdMs", 0) !in 0L..RETENTION_MS) {
            inputRecords.removeFirst(); inputOwners.removeFirst()
        }
    }

    private fun segments(): List<File> {
        val entries = file.listFiles() ?: error("Cannot list telemetry segments")
        check(entries.all { it.isFile && it.name.matches(Regex("[0-9]{16}\\.seg")) })
        return entries.sortedBy { it.name }
    }

    private fun replay(segment: File, newest: Boolean) {
        val sequence = segment.name.substringBefore('.').toLong()
        RandomAccessFile(segment, "rw").use { input ->
            check(input.length() <= SEGMENT_BYTES && input.length() >= 0)
            while (input.filePointer < input.length()) {
                val offset = input.filePointer
                if (input.length() - offset < 4) {
                    check(newest) { "Incomplete historical segment" }
                    input.setLength(offset)
                    input.fd.sync()
                    break
                }
                val length = input.readInt()
                check(length in 30..MAX_EVENT_BYTES)
                if (input.length() - input.filePointer < length) {
                    check(newest) { "Incomplete historical event" }
                    input.setLength(offset)
                    input.fd.sync()
                    break
                }
                val encrypted = ByteArray(length)
                input.readFully(encrypted)
                val plaintext = cipher.open(encrypted)
                try { apply(JSONObject(String(plaintext, StandardCharsets.UTF_8)), sequence) }
                finally { plaintext.fill(0); encrypted.fill(0) }
            }
        }
    }

    private fun apply(event: JSONObject, sequence: Long) {
        when (event.getString("type")) {
            "input-batch" -> {
                val rows = event.getJSONArray("rows")
                check(rows.length() in 1..64 && event.getLong("dropped") in 0..1_000_000_000)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    check(row.getString("schema") == "viipio-input-v1" && row.length() == 11 &&
                        row.getLong("createdMs") > 0 && TelemetryPolicy.id(row.getString("sessionId")) &&
                        row.getLong("seq") in 1..1_000_000_000 && row.getLong("timeMs") in 0..86_400_000 &&
                        row.getString("lang") in setOf("et", "et-ee", "en", "en-us", "en-gb") &&
                        row.getString("kind") in InputEventKind.entries.map { it.wire } &&
                        row.getString("source") in InputEventSource.entries.map { it.wire } &&
                        row.getString("outcome") in InputEventOutcome.entries.map { it.wire } &&
                        row.getInt("textLength") in 0..64)
                    val parsed = ViipioInputEvent(row.getString("sessionId"), row.getLong("seq"),
                        row.getLong("timeMs"), row.getString("lang"),
                        InputEventKind.entries.first { it.wire == row.getString("kind") }, row.getString("text"),
                        InputEventSource.entries.first { it.wire == row.getString("source") },
                        InputEventOutcome.entries.first { it.wire == row.getString("outcome") }, row.getInt("textLength"))
                    check(InputEventPolicy.valid(parsed))
                    inputRecords.addLast(row); inputOwners.addLast(sequence)
                    if (inputRecords.size > MAX_INPUT_EVENTS) {
                        inputRecords.removeFirst(); inputOwners.removeFirst()
                    }
                }
            }
            "manual" -> {
                val row = event.getJSONObject("row")
                val id = row.getString("id")
                check(row.getString("schema") == "estonian-manual-v1" &&
                    TelemetryPolicy.id(id) && !manualRecords.containsKey(id) &&
                    row.getString("lang") == "et" && TelemetryPolicy.word(row.getString("word")) &&
                    row.getLong("createdMs") > 0 &&
                    (!row.has("swipeId") || TelemetryPolicy.id(row.getString("swipeId"))) &&
                    row.length() == (if (row.has("swipeId")) 6 else 5))
                manualRecords[id] = row
                manualOwners[id] = sequence
                // Replay is bounded too: do not transiently hold the entire 48 MiB log.
                if (manualRecords.size > MAX_SWIPES) {
                    val oldest = manualRecords.keys.first()
                    manualRecords.remove(oldest)
                    manualOwners.remove(oldest)
                }
            }
            "swipe" -> {
                val row = event.getJSONObject("row")
                val id = row.getString("id")
                val schema = row.getString("schema")
                check(TelemetryPolicy.id(id) && schema in setOf("estonian-swipe-v1", "estonian-swipe-v2") &&
                    !records.containsKey(id))
                if (schema == "estonian-swipe-v2") {
                    val candidates = row.getJSONArray("candidates")
                    val scores = row.getJSONArray("candidateScores")
                    check(row.getString("decoder") in SwipeDecoderSource.entries.map { it.wireName } &&
                        row.getString("appVersion").length in 1..40 &&
                        candidates.length() in 1..5 && scores.length() == candidates.length())
                    for (i in 0 until scores.length()) check(scores.get(i) is Int && scores.getInt(i) in 0..1000)
                }
                if (records.size >= MAX_SWIPES) {
                    val oldest = records.keys.first()
                    records.remove(oldest)
                    owners.remove(oldest)
                }
                records[id] = row
                owners[id] = sequence
                replaySizeObserver(records.size)
            }
            "undo" -> {
                val row = records[event.getString("id")] ?: return
                check(!row.has("finalWord") && !row.has("deleted") && !row.optBoolean("undoObserved"))
                row.put("undoObserved", true)
            }
            "correction" -> {
                val row = records[event.getString("id")] ?: return // parent segment was evicted
                check(!row.has("finalWord") && !row.has("deleted"))
                val corrections = row.getJSONArray("corrections")
                check(corrections.length() < 20)
                val current = if (corrections.length() == 0) row.getString("committed")
                    else corrections.getJSONObject(corrections.length() - 1).getString("to")
                check(current == event.getString("from") && TelemetryPolicy.word(event.getString("to")))
                corrections.put(JSONObject().put("from", current).put("to", event.getString("to"))
                    .put("kind", event.getString("kind")))
            }
            "finalization" -> {
                val row = records[event.getString("id")] ?: return
                check(!row.has("finalWord") && !row.has("deleted"))
                if (event.getBoolean("deleted")) row.put("deleted", true)
                else row.put("finalWord", event.getString("finalWord"))
            }
            else -> error("Unknown telemetry event")
        }
    }

    private fun append(event: JSONObject): Boolean {
        val plaintext = event.toString().toByteArray(StandardCharsets.UTF_8)
        val encrypted = try { cipher.seal(plaintext) }
            catch (_: Exception) { plaintext.fill(0); return false }
        plaintext.fill(0)
        try {
            check(encrypted.size in 30..MAX_EVENT_BYTES && encrypted.size + 4 <= SEGMENT_BYTES)
            if (!file.exists()) check(file.mkdir())
            val segments = segments()
            val last = segments.lastOrNull()
            val sequence = last?.name?.substringBefore('.')?.toLong() ?: 0L
            val next = if (last == null || last.length() + encrypted.size + 4 > SEGMENT_BYTES) sequence + 1 else sequence
            val target = File(file, "%016d.seg".format(java.util.Locale.ROOT, next))
            val existing = segments.sumOf { it.length() }
            // Reserve room before writing; whole oldest segments are evicted, never rewritten.
            var used = existing
            for (old in segments) {
                if (used + encrypted.size + 4 <= MAX_BYTES) break
                val bytes = old.length()
                check(old != target && old.delete())
                used -= bytes
                val evicted = old.name.substringBefore('.').toLong()
                val iterator = owners.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value == evicted) { records.remove(entry.key); iterator.remove() }
                }
                val manualIterator = manualOwners.entries.iterator()
                while (manualIterator.hasNext()) {
                    val entry = manualIterator.next()
                    if (entry.value == evicted) { manualRecords.remove(entry.key); manualIterator.remove() }
                }
                while (inputOwners.isNotEmpty() && inputOwners.first() == evicted) {
                    inputOwners.removeFirst(); inputRecords.removeFirst()
                }
            }
            check(segments().sumOf { it.length() } + encrypted.size + 4 <= MAX_BYTES)
            // Append with one fsync. On an uncertain write failure, fail closed until explicit clear.
            RandomAccessFile(target, "rw").use { out ->
                out.seek(out.length())
                out.writeInt(encrypted.size)
                out.write(encrypted)
                out.fd.sync()
            }
            apply(event, next)
            trim()
            return true
        } catch (_: Exception) {
            healthy = false
            isEnabled = false
            isManualEnabled = false
            isInputEnabled = false
            return false
        } finally { encrypted.fill(0) }
    }
}
