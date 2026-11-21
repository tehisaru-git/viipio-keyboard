package tribixbite.cleverkeys

import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/** Only a successful, editor-confirmed Space after a selected alternative can teach a pair. */
internal object ViipioCorrectionConfirmation {
    data class Receipt(val word: String, val caret: Int)

    private fun caret(connection: InputConnection): Int? {
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0) ?: return null
        if (extracted.startOffset < 0 || extracted.selectionStart != extracted.selectionEnd ||
            connection.getSelectedText(0)?.isNotEmpty() == true) return null
        val absolute = extracted.startOffset.toLong() + extracted.selectionEnd
        return if (absolute in 0..Int.MAX_VALUE.toLong()) absolute.toInt() else null
    }

    fun beforeSpace(connection: InputConnection, word: String): Receipt? = runCatching {
        if (word.length !in 2..48 || !word.all(Char::isLetter) ||
            connection.getTextBeforeCursor(word.length, 0)?.toString() != word) return null
        val at = caret(connection) ?: return null
        if (at < word.length) return null
        Receipt(word, at)
    }.getOrNull()

    fun afterSpace(connection: InputConnection, receipt: Receipt): Boolean = runCatching {
        caret(connection) == receipt.caret + 1 &&
            connection.getTextBeforeCursor(receipt.word.length + 1, 0)?.toString() ==
                receipt.word + " "
    }.getOrDefault(false)
}
