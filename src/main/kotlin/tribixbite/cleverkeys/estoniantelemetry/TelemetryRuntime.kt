package tribixbite.cleverkeys.estoniantelemetry

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Serial, non-main-thread owner of the encrypted store. No background upload or export. */
object TelemetryRuntime {
    private val worker = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "estonian-swipe-local-telemetry").apply { isDaemon = true }
    }
    private val inputLock = Any()
    private val pendingInput = ArrayDeque<Pair<ViipioInputEvent, EditorInfo>>()
    private var pendingDropped = 0L
    private val flushSignals = InputFlushSchedule()
    private val main = Handler(Looper.getMainLooper())
    private var controller: TelemetryController? = null // worker thread only
    @Volatile var collecting: Boolean = false
        private set
    @Volatile var collectingManual: Boolean = false
        private set
    @Volatile var collectingInput: Boolean = false
        private set

    private fun store(context: Context): TelemetryController = controller
        ?: TelemetryController(context.applicationContext).also {
            controller = it
            collecting = it.isEnabled
            collectingManual = it.isManualEnabled
            collectingInput = it.isInputEnabled
        }
    private fun editorSnapshot(info: EditorInfo?): EditorInfo? = info?.let { source ->
        EditorInfo().apply {
            packageName = source.packageName
            fieldId = source.fieldId
            inputType = source.inputType
            imeOptions = source.imeOptions
            privateImeOptions = source.privateImeOptions
        }
    }
    private fun <T> deliver(callback: ((T) -> Unit)?, result: T) {
        if (callback != null) main.post { callback(result) }
    }

    /** Loads persistent consent off the IME thread on process startup. */
    fun initialize(context: Context, callback: ((TelemetryStats?) -> Unit)? = null) {
        val app = context.applicationContext
        worker.execute {
            val result = try { store(app).stats() } catch (_: Exception) {
                collecting = false; collectingManual = false; collectingInput = false; null
            }
            deliver(callback, result)
        }
    }

    fun enable(context: Context, callback: (Boolean) -> Unit) {
        val app = context.applicationContext
        worker.execute {
            val ok = try { store(app).enable() } catch (_: Exception) { false }
            collecting = ok
            deliver(callback, ok)
        }
    }

    fun enableManual(context: Context, callback: (Boolean) -> Unit) {
        val app = context.applicationContext
        worker.execute {
            val ok = try { store(app).enableManual() } catch (_: Exception) { false }
            collectingManual = ok
            deliver(callback, ok)
        }
    }

    fun enableInputEvents(context: Context, callback: (Boolean) -> Unit) {
        val app = context.applicationContext
        worker.execute {
            val ok = try { store(app).enableInputEvents() } catch (_: Exception) { false }
            collectingInput = ok
            deliver(callback, ok)
        }
    }

    private fun dropPendingInput() = synchronized(inputLock) {
        pendingInput.clear(); pendingDropped = 0
    }

    /** Timer ownership stays held until its callback finishes, even if an immediate flush drains
     * the queue first. A new event racing callback completion is re-armed below.
     */
    private fun scheduleInputTimer(context: Context) {
        worker.schedule({
            try { flushInput(context) } finally {
                synchronized(inputLock) {
                    flushSignals.finishDelayed()
                    if (collectingInput && pendingInput.isNotEmpty() && flushSignals.requestDelayed())
                        scheduleInputTimer(context)
                }
            }
        }, 750, TimeUnit.MILLISECONDS)
    }

    private fun scheduleInputImmediate(context: Context) {
        worker.execute {
            try { flushInput(context) } finally {
                synchronized(inputLock) {
                    flushSignals.finishImmediate()
                    if (collectingInput && pendingInput.isNotEmpty() && flushSignals.requestDelayed())
                        scheduleInputTimer(context)
                }
            }
        }
    }

    /** Bounded on the IME thread; only the keyboard's confirmed action may be passed here. */
    fun recordInputEvent(context: Context, event: ViipioInputEvent, info: EditorInfo?) {
        if (!collectingInput || !InputEventPolicy.valid(event) || !TelemetryPolicy.editor(info)) return
        val editor = editorSnapshot(info) ?: return
        val app = context.applicationContext
        synchronized(inputLock) {
            if (!collectingInput) return
            if (pendingInput.size >= 128) { pendingDropped++; return }
            pendingInput.addLast(event to editor)
            if (flushSignals.requestDelayed()) scheduleInputTimer(app)
            if (flushSignals.requestImmediate(pendingInput.size)) scheduleInputImmediate(app)
        }
    }

    private fun flushInput(context: Context) {
        if (!collectingInput) { dropPendingInput(); return }
        while (collectingInput) {
            val batch: List<ViipioInputEvent>
            val editor: EditorInfo
            val dropped: Long
            synchronized(inputLock) {
                val first = pendingInput.firstOrNull() ?: return
                editor = first.second
                val selected = ArrayList<ViipioInputEvent>(64)
                while (selected.size < 64 && pendingInput.isNotEmpty() &&
                    pendingInput.first().second.packageName == editor.packageName &&
                    pendingInput.first().second.fieldId == editor.fieldId &&
                    pendingInput.first().second.inputType == editor.inputType &&
                    pendingInput.first().first.sessionId == first.first.sessionId) {
                    selected.add(pendingInput.removeFirst().first)
                }
                batch = selected
                dropped = pendingDropped.coerceAtMost(1_000_000_000)
                pendingDropped -= dropped
            }
            val ok = try { collectingInput && store(context).recordInputBatch(batch, editor, dropped) }
                catch (_: Exception) { false }
            if (!ok) { collectingInput = false; dropPendingInput(); return }
        }
    }

    fun disable(context: Context, callback: (Boolean) -> Unit) {
        collecting = false
        collectingManual = false
        collectingInput = false
        dropPendingInput()
        val app = context.applicationContext
        worker.execute {
            val ok = try { store(app).disable() } catch (_: Exception) { false }
            collecting = false
            collectingManual = false
            collectingInput = false
            deliver(callback, ok)
        }
    }

    fun clear(context: Context, callback: (Boolean) -> Unit) {
        collecting = false
        collectingManual = false
        collectingInput = false
        dropPendingInput()
        val app = context.applicationContext
        worker.execute {
            val ok = try { store(app).clear() } catch (_: Exception) { false }
            collecting = false
            collectingManual = false
            collectingInput = false
            deliver(callback, ok)
        }
    }

    fun stats(context: Context, callback: (TelemetryStats?) -> Unit) = initialize(context, callback)

    /** A provider may commit bytes before write/flush/close throws. Cleanup is best effort only. */
    internal fun writeExport(open: () -> OutputStream?, cleanup: () -> Unit,
        export: (OutputStream) -> Unit): Boolean = try {
        val out = open() ?: error("No output stream")
        out.use(export)
        true
    } catch (_: Exception) {
        runCatching { cleanup() }
        false
    }

    /** The user must have selected this URI in ACTION_CREATE_DOCUMENT first. */
    fun export(context: Context, chosenUri: Uri, callback: (Boolean) -> Unit) {
        val app = context.applicationContext
        worker.execute {
            val ok = writeExport(open = {
                flushInput(app)
                app.contentResolver.openOutputStream(chosenUri, "wt")
            }, cleanup = {
                val removed = runCatching { app.contentResolver.delete(chosenUri, null, null) > 0 }
                    .getOrDefault(false)
                if (!removed) app.contentResolver.openOutputStream(chosenUri, "wt")?.use { }
            }, export = { store(app).exportJsonl(it) })
            deliver(callback, ok)
        }
    }

    fun recordSwipe(context: Context, trace: SwipeTrace) {
        if (!collecting) return
        val app = context.applicationContext
        val snapshot = trace.copy(editor = editorSnapshot(trace.editor),
            points = trace.points.toList(), candidates = trace.candidates.toList(),
            candidateScores = trace.candidateScores?.toList(),
            geometry = trace.geometry.copy(keys = trace.geometry.keys.toList()))
        worker.execute { if (collecting) try { store(app).recordSwipe(snapshot) } catch (_: Exception) { collecting = false } }
    }

    /** Caller attests to a completed ET word typed entirely with Viipio keys and read back
     * in this same field. Never derive a word from arbitrary editor context here.
     */
    fun recordManualWord(
        context: Context, word: String, language: String, info: EditorInfo?,
        source: ManualWordSource,
        directUndoSwipeId: String? = null,
    ) {
        if (!collectingManual) return
        val app = context.applicationContext
        val editor = editorSnapshot(info)
        worker.execute {
            if (collectingManual) try {
                store(app).recordManualWord(word, language, editor, source, directUndoSwipeId)
            } catch (_: Exception) { collectingManual = false }
        }
    }

    fun recordUndo(context: Context, id: String, info: EditorInfo?) {
        if (!collecting) return
        val app = context.applicationContext
        val editor = editorSnapshot(info)
        worker.execute { if (collecting) try { store(app).recordUndo(id, "et", editor) } catch (_: Exception) { collecting = false } }
    }

    fun recordCorrection(context: Context, id: String, info: EditorInfo?, from: String, to: String, kind: String) {
        if (!collecting) return
        val app = context.applicationContext
        val editor = editorSnapshot(info)
        worker.execute { if (collecting) try { store(app).recordCorrection(id, "et", editor, from, to, kind) } catch (_: Exception) { collecting = false } }
    }

    fun recordFinalization(context: Context, id: String, info: EditorInfo?, finalWord: String?) {
        if (!collecting) return
        val app = context.applicationContext
        val editor = editorSnapshot(info)
        worker.execute { if (collecting) try { store(app).recordFinalization(id, "et", editor, finalWord) } catch (_: Exception) { collecting = false } }
    }
}
