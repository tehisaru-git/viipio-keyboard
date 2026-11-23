package tribixbite.cleverkeys

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import tribixbite.cleverkeys.estoniantelemetry.TelemetryPolicy
import tribixbite.cleverkeys.estonianui.KeyboardLanguage

/** One exact, editor-owned Add/Undo target, never a parsed word from arbitrary host text. */
internal data class ViipioInlinePersonalOffer(
    val word: String,
    val language: KeyboardLanguage,
    val info: EditorInfo,
    val connection: InputConnection,
    val caret: Int,
    val completed: Boolean = true,
) {
    fun stillOwned(activeInfo: EditorInfo?, activeConnection: InputConnection?,
                   activeLanguage: KeyboardLanguage, typedPending: String): Boolean {
        if (activeInfo !== info || activeConnection !== connection || activeLanguage != language ||
            !TelemetryPolicy.editor(info) || !ViipioPersonalWords.validWord(word) ||
            (!completed && typedPending != word)) return false
        return runCatching {
            val extracted = connection.getExtractedText(ExtractedTextRequest(), 0)
                ?: return@runCatching false
            val absolute = extracted.startOffset.toLong() + extracted.selectionEnd
            if (extracted.startOffset < 0 || extracted.selectionStart != extracted.selectionEnd ||
                absolute != caret.toLong() || connection.getSelectedText(0)?.isNotEmpty() == true)
                return@runCatching false
            val suffix = if (completed) "$word " else word
            val before = connection.getTextBeforeCursor(suffix.length + 1, 0)?.toString()
                ?: return@runCatching false
            if (!before.endsWith(suffix) || before.length <
                suffix.length + (if (caret > suffix.length) 1 else 0) ||
                (caret > suffix.length && before[before.length - suffix.length - 1].isLetter()))
                return@runCatching false
            if (!completed) {
                val right = connection.getTextAfterCursor(1, 0)?.toString()
                    ?: return@runCatching false
                if (right.firstOrNull()?.isLetter() == true) return@runCatching false
            }
            true
        }.getOrDefault(false)
    }
}
