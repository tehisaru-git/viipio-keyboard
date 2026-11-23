package tribixbite.cleverkeys

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import tribixbite.cleverkeys.estoniantelemetry.TelemetryPolicy
import tribixbite.cleverkeys.estonianui.KeyboardLanguage

/** An in-memory, single-field word assembled only after successful Viipio letter writes. */
internal class ViipioTypedWordCapture {
    private val word = ViipioTypedWordTracker()
    private var editor: EditorInfo? = null
    private var connection: InputConnection? = null
    private var language: KeyboardLanguage? = null
    private var caret: Int? = null
    val pending: String get() = word.pending

    fun clear() {
        word.clear()
        editor = null
        connection = null
        language = null
        caret = null
    }

    private fun caretAt(connection: InputConnection): Int? {
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0) ?: return null
        val position = extracted.startOffset.toLong() + extracted.selectionEnd
        if (extracted.startOffset < 0 || extracted.selectionStart != extracted.selectionEnd ||
            position !in 0..Int.MAX_VALUE.toLong() ||
            connection.getSelectedText(0)?.isNotEmpty() == true) return null
        return position.toInt()
    }

    /** Called only after an observed Viipio key successfully committed [value]. */
    fun recordLetter(value: String, activeLanguage: KeyboardLanguage,
                     info: EditorInfo, input: InputConnection): Boolean {
        return try {
        if (!TelemetryPolicy.editor(info)) { clear(); return false }
        val previous = word.pending
        val position = caretAt(input)
        if (position == null || (previous.isNotEmpty() &&
                (info !== editor || input !== connection || activeLanguage != language ||
                    position != (caret ?: -2) + 1))) { clear(); return false }
        if (!word.letter(value)) { clear(); return false }
        val expected = word.pending
        val before = input.getTextBeforeCursor(expected.length + 1, 0)?.toString()
        val after = if (previous.isEmpty()) input.getTextAfterCursor(1, 0) else ""
        if (before == null || !before.endsWith(expected) ||
            (previous.isEmpty() && before.length > expected.length &&
                before[before.length - expected.length - 1].isLetter()) ||
            (previous.isEmpty() && (after == null || after.firstOrNull()?.isLetter() == true))) {
            clear(); return false
        }
        editor = info
        connection = input
        language = activeLanguage
        caret = position
        return true
    } catch (_: Exception) {
        clear()
        false
        }
    }

    /** Prove the still-in-progress word without requiring Space or finalizing it. */
    fun offerBeforeSpace(activeLanguage: KeyboardLanguage, info: EditorInfo,
                         input: InputConnection): String? {
        // One successful letter is a valid prefix, not an invalid/stale Add offer.
        // The next key rechecks editor, connection, language, caret and left boundary.
        if (word.pending.length == 1) return null
        val offer = runCatching {
        val value = word.pending
        val expectedCaret = caret ?: return@runCatching null
        if (!ViipioPersonalWords.validWord(value) || !TelemetryPolicy.editor(info) ||
            info !== editor || input !== connection || activeLanguage != language ||
            caretAt(input) != expectedCaret) return@runCatching null
        val before = input.getTextBeforeCursor(value.length + 1, 0)?.toString()
            ?: return@runCatching null
        val after = input.getTextAfterCursor(1, 0)?.toString() ?: return@runCatching null
        if (!before.endsWith(value) || before.length <
            value.length + (if (expectedCaret > value.length) 1 else 0) ||
            (expectedCaret > value.length && before[before.length - value.length - 1].isLetter()) ||
            after.firstOrNull()?.isLetter() == true) return@runCatching null
        value
        }.getOrNull()
        if (offer == null) clear()
        return offer
    }

    /** Called only after an observed Viipio Backspace removed one owned letter. */
    fun recordBackspace(activeLanguage: KeyboardLanguage, info: EditorInfo,
                        input: InputConnection): Boolean {
        return try {
        val previous = word.pending
        val position = caretAt(input)
        val expected = previous.dropLast(1)
        if (previous.isEmpty() || info !== editor || input !== connection ||
            activeLanguage != language || position == null || position != (caret ?: -2) - 1 ||
            input.getTextBeforeCursor(expected.length, 0)?.toString() != expected) {
            clear(); return false
        }
        word.backspace()
        caret = position
        if (word.pending.isEmpty()) clear()
        return true
    } catch (_: Exception) {
        clear()
        false
        }
    }

    /** Called only after Viipio successfully committed a literal separating space. */
    fun completeAfterSpace(activeLanguage: KeyboardLanguage, info: EditorInfo,
                           input: InputConnection): String? {
        return try {
        val expected = word.pending
        val result = if (expected.isNotEmpty() && TelemetryPolicy.editor(info) &&
            info === editor && input === connection &&
            activeLanguage == language && caretAt(input) == (caret ?: -2) + 1 &&
            input.getTextBeforeCursor(expected.length + 1, 0)?.toString() == "$expected ")
            word.complete() else null
        clear()
        return result
    } catch (_: Exception) {
        clear()
        null
        }
    }
}
