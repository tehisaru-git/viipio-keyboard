package tribixbite.cleverkeys

import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import tribixbite.cleverkeys.estoniantelemetry.InputEventKind
import tribixbite.cleverkeys.estoniantelemetry.InputEventOutcome
import tribixbite.cleverkeys.estoniantelemetry.InputEventSource
import tribixbite.cleverkeys.estoniantelemetry.TelemetryPolicy
import tribixbite.cleverkeys.estoniantelemetry.ViipioInputEvent
import java.util.UUID

/** Transient, bounded evidence of this IME's own writes; never observes unrelated editor changes. */
internal class ViipioInputEvidence(
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val id: () -> String = { UUID.randomUUID().toString() },
) {
    internal data class Before(val editor: EditorInfo, val connection: InputConnection,
                               val language: String, val caret: Int, val suffix: String)
    private var session = id()
    private var started = clock()
    private var seq = 0L

    fun reset() { session = id(); started = clock(); seq = 0L }

    private fun caret(connection: InputConnection): Int? = runCatching {
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0) ?: return@runCatching null
        val absolute = extracted.startOffset.toLong() + extracted.selectionEnd
        if (extracted.startOffset < 0 || extracted.selectionStart != extracted.selectionEnd ||
            absolute !in 0..Int.MAX_VALUE.toLong() ||
            !connection.getSelectedText(0).isNullOrEmpty()) null else absolute.toInt()
    }.getOrNull()

    fun capture(info: EditorInfo?, connection: InputConnection?, language: String,
                consent: Boolean): Before? {
        if (!consent || !TelemetryPolicy.editor(info) || connection == null || info == null) return null
        val caret = caret(connection) ?: return null
        val suffix = runCatching { connection.getTextBeforeCursor(64, 0)?.toString() }.getOrNull() ?: return null
        if (suffix.length > 64) return null
        return Before(info, connection, language, caret, suffix)
    }

    /** Record only exact append, replacement, or one-code-point deletion proved by readback. */
    fun changed(before: Before?, info: EditorInfo?, connection: InputConnection?, language: String,
                consent: Boolean, kind: InputEventKind, text: String, source: InputEventSource,
                outcome: InputEventOutcome = InputEventOutcome.APPLIED,
                replaced: String = "", emit: (ViipioInputEvent, EditorInfo) -> Unit): Boolean {
        if (before == null || !consent || info !== before.editor || connection !== before.connection ||
            language != before.language || !TelemetryPolicy.editor(info)) return false
        val removed = when (kind) {
            InputEventKind.DELETE -> {
                if (text.isNotEmpty() || before.suffix.isEmpty() ||
                    Character.isLowSurrogate(before.suffix.last()) && before.suffix.length == 1) return false
                Character.charCount(Character.codePointBefore(before.suffix, before.suffix.length))
            }
            InputEventKind.CANDIDATE -> {
                if (replaced.isEmpty() || !before.suffix.endsWith(replaced)) return false
                replaced.length
            }
            else -> 0
        }
        if (removed > before.suffix.length || kind == InputEventKind.DELETE && before.suffix.length == 64) return false
        val expected = before.suffix.dropLast(removed) + text
        val afterCaret = caret(connection) ?: return false
        if (afterCaret != before.caret - removed + text.length ||
            runCatching { connection!!.getTextBeforeCursor(expected.length.coerceAtMost(64), 0)?.toString() }
                .getOrNull() != expected.takeLast(64)) return false
        return emit(kind, text, source, outcome, if (kind == InputEventKind.DELETE) removed else text.length,
            info, emit, language)
    }

    fun language(info: EditorInfo?, connection: InputConnection?, consent: Boolean,
                 language: String, emit: (ViipioInputEvent, EditorInfo) -> Unit): Boolean {
        if (!consent || !TelemetryPolicy.editor(info) || connection == null || info == null ||
            caret(connection) == null) return false
        return emit(InputEventKind.LANGUAGE, "", InputEventSource.SYSTEM, InputEventOutcome.APPLIED,
            0, info, emit, language)
    }

    private fun emit(kind: InputEventKind, text: String, source: InputEventSource,
                     outcome: InputEventOutcome, length: Int, info: EditorInfo,
                     sink: (ViipioInputEvent, EditorInfo) -> Unit, language: String = ""): Boolean {
        val elapsed = clock() - started
        if (seq >= 1_000_000_000 || elapsed !in 0..86_400_000) return false
        val event = ViipioInputEvent(session, seq + 1, elapsed, language, kind, text, source, outcome, length)
        // The storage boundary independently validates the row and editor.
        if (!tribixbite.cleverkeys.estoniantelemetry.InputEventPolicy.valid(event)) return false
        seq++
        sink(event, info)
        return true
    }
}
