package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Locale

/**
 * Local, credential-protected evidence of explicit alternate choices, NOT passive typing history.
 * The caller must invoke [record] exactly once per distinct, successfully editor-verified choice;
 * this store cannot infer editor provenance from a word pair. The caller also owns opt-in and
 * field eligibility. A count of 1 is provisional; only a count of 2 is promoted.
 *
 * Only normalized ET/EN word pairs and capped counts are persisted. No editor text, trace, event
 * identifier, or surrounding context is read or saved. The separate file must remain outside
 * Android/cloud backup and any manual settings export (enforced by the integrating caller).
 */
internal class ViipioCorrectionPreference private constructor(
    private val preferences: () -> SharedPreferences,
    private val accessible: () -> Boolean,
) {
    constructor(context: Context) : this(
        { context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE) },
        {
            if (Build.VERSION.SDK_INT < 24) true
            else !context.isDeviceProtectedStorage &&
                !context.applicationContext.isDeviceProtectedStorage &&
                context.getSystemService(UserManager::class.java)?.isUserUnlocked == true
        }
    )

    /** Injected preferences are intended for tests only; production always uses the CE constructor. */
    internal constructor(prefs: SharedPreferences) : this({ prefs }, { true })

    private data class PairChoice(val language: String, val from: String, val to: String, val count: Int)
    private var failed = false

    /** Captured with a provisional choice; null forbids learning until storage is healthy. */
    @Synchronized
    fun revision(): Long? {
        if (failed || !canAccess()) return null
        return try {
            readRevision(preferences()).also { if (it == null) failed = true }
        } catch (_: Exception) {
            failed = true
            null
        }
    }

    private fun readRevision(prefs: SharedPreferences): Long? {
        val value = if (prefs.contains(REVISION_KEY)) prefs.getLong(REVISION_KEY, -1L) else 0L
        return value.takeIf { it in 0 until Long.MAX_VALUE }
    }

    @Synchronized
    fun count(language: String, from: String, to: String): Int {
        val pair = normalized(language, from, to) ?: return 0
        val choices = read() ?: return 0
        return choices.firstOrNull {
            it.language == pair.language && it.from == pair.from && it.to == pair.to
        }?.count ?: 0
    }

    /** One bounded read per swipe; returned net counts are ephemeral, not editor text. */
    @Synchronized
    fun netFor(language: String, head: String): Map<String, Int> {
        if (language !in setOf("et", "en")) return emptyMap()
        val normalizedHead = head.lowercase(Locale.ROOT)
        if (!validWord(normalizedHead)) return emptyMap()
        val choices = read() ?: return emptyMap()
        val net = HashMap<String, Int>()
        for (choice in choices) {
            if (choice.language != language) continue
            if (choice.from == normalizedHead)
                net[choice.to] = (net[choice.to] ?: 0) + choice.count
            if (choice.to == normalizedHead)
                net[choice.from] = (net[choice.from] ?: 0) - choice.count
        }
        return net
    }

    /** Returns true only when a distinct explicit choice was durably recorded. */
    @Synchronized
    fun record(language: String, from: String, to: String): Boolean {
        val pair = normalized(language, from, to) ?: return false
        val choices = read() ?: return false
        val index = choices.indexOfFirst {
            it.language == pair.language && it.from == pair.from && it.to == pair.to
        }
        if (index >= 0) choices[index] = pair.copy(count = nextCount(choices[index].count))
        else {
            if (choices.size == MAX_PAIRS) choices.removeAt(0)
            choices.add(pair.copy(count = 1))
        }
        val payload = JSONObject().apply {
            put("version", SCHEMA_VERSION)
            put("pairs", JSONArray().apply {
                for (choice in choices) put(JSONObject().apply {
                    put("language", choice.language)
                    put("from", choice.from)
                    put("to", choice.to)
                    put("count", choice.count)
                })
            })
        }.toString()
        return try {
            preferences().edit().putString(KEY, payload).commit().also { if (!it) failed = true }
        } catch (_: Exception) {
            failed = true
            false
        }
    }

    /** Explicit erasure is allowed even if stored JSON is corrupt; never repair it on record. */
    @Synchronized
    fun clear(): Boolean = synchronized(GENERATION_LOCK) {
        if (!canAccess()) return@synchronized false
        try {
            val prefs = preferences()
            // A corrupt counter cannot be incremented; explicit erasure starts a fresh,
            // unpredictable generation so an earlier captured value cannot normally match.
            val previous = try { readRevision(prefs) } catch (_: ClassCastException) { null }
            val next = previous?.takeIf { it < Long.MAX_VALUE - 1 }?.plus(1)
                ?: (SecureRandom().nextLong() and Long.MAX_VALUE)
                    .takeIf { it != 0L && it != Long.MAX_VALUE && it != previous } ?: 1L
            val editor = prefs.edit()
            editor.remove(KEY)
            editor.putLong(REVISION_KEY, next)
            editor.commit().also { if (it) failed = false else failed = true }
        } catch (_: Exception) {
            failed = true
            false
        }
    }

    private fun canAccess(): Boolean = try {
        accessible()
    } catch (_: Exception) {
        false
    }

    private fun read(): MutableList<PairChoice>? {
        if (failed || !canAccess()) return null
        return try {
            val prefs = preferences()
            if (readRevision(prefs) == null) return invalid()
            if (!prefs.contains(KEY)) return mutableListOf()
            val raw = prefs.getString(KEY, null) ?: return invalid()
            if (raw.length > MAX_JSON_CHARS) return invalid()
            val json = JSONObject(raw)
            if (json.keys().asSequence().toSet() != setOf("version", "pairs") ||
                json.get("version") !is Int || json.getInt("version") != SCHEMA_VERSION ||
                json.get("pairs") !is JSONArray) return invalid()
            val array = json.getJSONArray("pairs")
            if (array.length() > MAX_PAIRS) return invalid()
            val result = mutableListOf<PairChoice>()
            val seen = HashSet<Triple<String, String, String>>()
            for (i in 0 until array.length()) {
                val item = array.get(i) as? JSONObject ?: return invalid()
                if (item.keys().asSequence().toSet() != setOf("language", "from", "to", "count") ||
                    item.get("language") !is String || item.get("from") !is String ||
                    item.get("to") !is String || item.get("count") !is Int) return invalid()
                val language = item.getString("language")
                val from = item.getString("from")
                val to = item.getString("to")
                val count = item.getInt("count")
                if (count !in 1..4 || normalized(language, from, to) != PairChoice(language, from, to, 0) ||
                    !seen.add(Triple(language, from, to))) return invalid()
                result.add(PairChoice(language, from, to, count))
            }
            result
        } catch (_: Exception) {
            invalid()
        }
    }

    private fun invalid(): MutableList<PairChoice>? {
        failed = true
        return null
    }

    companion object {
        const val PREFS_FILE = "viipio_correction_preferences"
        private const val KEY = "pairs"
        private const val REVISION_KEY = "revision"
        private val GENERATION_LOCK = Any()
        private const val SCHEMA_VERSION = 1
        private const val MAX_PAIRS = 100
        // Bounds parsing of hostile/corrupt preferences before JSONObject allocates objects.
        private const val MAX_JSON_CHARS = 32_768

        /** Pure count transition; promotion requires two separately verified caller invocations. */
        internal fun nextCount(previous: Int): Int = (previous + 1).coerceAtMost(4)

        private fun normalized(language: String, from: String, to: String): PairChoice? {
            if (language != "et" && language != "en") return null
            val source = from.lowercase(Locale.ROOT)
            val target = to.lowercase(Locale.ROOT)
            if (source == target || !validWord(source) || !validWord(target)) return null
            return PairChoice(language, source, target, 0)
        }

        private fun validWord(word: String): Boolean = word.length in 2..48 && word.all {
            it.isLetter() && Character.UnicodeScript.of(it.code) == Character.UnicodeScript.LATIN
        }
    }
}
