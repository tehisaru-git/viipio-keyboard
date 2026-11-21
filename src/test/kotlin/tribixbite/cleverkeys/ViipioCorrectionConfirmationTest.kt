package tribixbite.cleverkeys

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ViipioCorrectionConfirmationTest {
    @Test fun successfulSpaceConfirmsOnlyTheExactOwnedSuffix() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("tore")
        field.setSelection(4)
        val connection = field.onCreateInputConnection(EditorInfo())
        val receipt = ViipioCorrectionConfirmation.beforeSpace(connection, "tore")!!
        assertFalse(ViipioCorrectionConfirmation.afterSpace(connection, receipt))
        assertTrue(connection.commitText(" ", 1))
        assertTrue(ViipioCorrectionConfirmation.afterSpace(connection, receipt))
        assertEquals("tore ", field.text.toString())
    }

    @Test fun repeatedIdenticalWordAtAnotherCaretHasDifferentReceipt() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("tore tore")
        field.setSelection(field.text.length)
        val connection = field.onCreateInputConnection(EditorInfo())
        val chosen = ViipioCorrectionConfirmation.beforeSpace(connection, "tore")!!
        field.setSelection(4)
        val earlier = ViipioCorrectionConfirmation.beforeSpace(connection, "tore")!!
        assertNotEquals(chosen.caret, earlier.caret)
        assertFalse(ViipioCorrectionConfirmation.afterSpace(connection, chosen))
        val service = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        assertTrue(service.contains("receipt.caret != pending.chosenCaret"))
    }

    @Test fun failedNextWriteAndMovedCursorDoNotConfirm() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("tore tore")
        field.setSelection(field.text.length)
        val base = field.onCreateInputConnection(EditorInfo())
        val connection = object : InputConnectionWrapper(base, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean =
                if (text?.toString() == " ") false else super.commitText(text, newCursorPosition)
        }
        val receipt = ViipioCorrectionConfirmation.beforeSpace(connection, "tore")!!
        assertFalse(connection.commitText(" ", 1))
        assertFalse(ViipioCorrectionConfirmation.afterSpace(connection, receipt))
        field.setSelection(4)
        assertFalse(ViipioCorrectionConfirmation.afterSpace(connection, receipt))
        assertNull(ViipioCorrectionConfirmation.beforeSpace(connection, "tore" + "x"))
    }
}
