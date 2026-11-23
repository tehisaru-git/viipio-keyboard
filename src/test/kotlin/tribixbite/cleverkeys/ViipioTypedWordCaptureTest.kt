package tribixbite.cleverkeys

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tribixbite.cleverkeys.estonianui.KeyboardLanguage

@RunWith(RobolectricTestRunner::class)
class ViipioTypedWordCaptureTest {
    private fun editor(type: Int = InputType.TYPE_CLASS_TEXT) = EditorInfo().apply {
        packageName = "org.example.editor"
        inputType = type
    }
    @Test fun refreshingAfterTheFirstLetterDoesNotDiscardTheWordBeingTyped() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val connection = field.onCreateInputConnection(info)
        for (ch in "siiis") {
            assertTrue(connection.commitText(ch.toString(), 1))
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, connection))
            val beforeSpace = capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, connection)
            if (ch == 's' && field.text.length == 1) assertNull(beforeSpace)
            assertEquals(field.text.toString(), capture.pending)
        }
        assertEquals("siiis", capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, connection))
        assertTrue(connection.commitText(" ", 1))
        assertEquals("siiis", capture.completeAfterSpace(KeyboardLanguage.ESTONIAN, info, connection))
    }

    @Test fun ownedUnknownWordCanBeOfferedBeforeSpaceWithoutChangingEditor() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val connection = field.onCreateInputConnection(info)
        for (ch in "mhm") {
            assertTrue(connection.commitText(ch.toString(), 1))
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, connection))
        }
        assertEquals("mhm", capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, connection))
        assertEquals("mhm", field.text.toString())
        assertNull(capture.offerBeforeSpace(KeyboardLanguage.ENGLISH, info, connection))
    }

    @Test fun partialWordWithChangedCaretOrFollowingLettersCannotBeOffered() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val connection = field.onCreateInputConnection(info)
        for (ch in "mhm") {
            connection.commitText(ch.toString(), 1)
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, connection))
        }
        field.setSelection(2)
        assertNull(capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, connection))
        field.setSelection(3)
        field.setText("mhmh")
        field.setSelection(3)
        assertNull(capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, connection))
    }

    @Test fun truncatedReadbackCannotOfferFragmentOfLongerWord() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val base = field.onCreateInputConnection(info)
        val input = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            var shorten = false
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? =
                super.getTextBeforeCursor(n, flags)?.let { if (shorten) it.drop(1) else it }
        }
        field.setText(" "); field.setSelection(1)
        for (ch in "mhm") {
            input.commitText(ch.toString(), 1)
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, input))
        }
        assertEquals("mhm", capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, input))
        input.shorten = true
        assertNull(capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, input))
    }

    @Test fun editorThrowingDuringInlineProofRevokesOfferWithoutCrashing() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val base = field.onCreateInputConnection(info)
        val input = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            var fault = ""
            override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest?, flags: Int):
                android.view.inputmethod.ExtractedText? {
                if (fault == "extract") throw IllegalStateException("editor detached")
                return super.getExtractedText(request, flags)
            }
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
                if (fault == "before") throw IllegalStateException("editor detached")
                return super.getTextBeforeCursor(n, flags)
            }
            override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
                if (fault == "after") throw IllegalStateException("editor detached")
                return super.getTextAfterCursor(n, flags)
            }
        }
        for (ch in "mhm") {
            input.commitText(ch.toString(), 1)
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, input))
        }
        for (stage in listOf("extract", "before", "after")) {
            input.fault = stage
            assertNull(capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, input))
        }
    }

    @Test fun recoveredEditorReadbackDoesNotReviveRevokedInlineOffer() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val base = field.onCreateInputConnection(info)
        val input = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            var unreadable = false
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? =
                if (unreadable) null else super.getTextBeforeCursor(n, flags)
        }
        for (ch in "mhm") {
            assertTrue(input.commitText(ch.toString(), 1))
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, input))
        }
        assertEquals("mhm", capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, input))
        input.unreadable = true
        assertNull(capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, input))
        input.unreadable = false
        assertNull(capture.offerBeforeSpace(KeyboardLanguage.ESTONIAN, info, input))
        assertEquals("", capture.pending)
    }

    @Test fun throwingLetterReadbackClearsOwnedWordWithoutCrashing() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val base = field.onCreateInputConnection(info)
        val broken = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest?, flags: Int):
                android.view.inputmethod.ExtractedText? = throw IllegalStateException("editor detached")
        }
        assertTrue(broken.commitText("m", 1))
        assertFalse(capture.recordLetter("m", KeyboardLanguage.ESTONIAN, info, broken))
        assertEquals("", capture.pending)
    }

    @Test fun throwingSpaceReadbackCannotCreateAnOffer() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val base = field.onCreateInputConnection(info)
        val broken = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            var throwBefore = false
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
                if (throwBefore) throw IllegalStateException("editor detached")
                return super.getTextBeforeCursor(n, flags)
            }
        }
        for (ch in "mhm") {
            assertTrue(broken.commitText(ch.toString(), 1))
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, broken))
        }
        assertTrue(broken.commitText(" ", 1))
        broken.throwBefore = true
        assertNull(capture.completeAfterSpace(KeyboardLanguage.ESTONIAN, info, broken))
        assertEquals("", capture.pending)
    }

    @Test fun fieldTurningPrivateBeforeSpaceCannotCompleteOwnedWord() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val connection = field.onCreateInputConnection(info)
        for (ch in "mhm") {
            assertTrue(connection.commitText(ch.toString(), 1))
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, connection))
        }
        info.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertTrue(connection.commitText(" ", 1))
        assertNull(capture.completeAfterSpace(KeyboardLanguage.ESTONIAN, info, connection))
        assertEquals("", capture.pending)
    }

    @Test fun throwingBackspaceReadbackClearsOwnedWordWithoutCrashing() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val base = field.onCreateInputConnection(info)
        val broken = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            var throwBefore = false
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
                if (throwBefore) throw IllegalStateException("editor detached")
                return super.getTextBeforeCursor(n, flags)
            }
        }
        for (ch in "mhm") {
            assertTrue(broken.commitText(ch.toString(), 1))
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, broken))
        }
        assertTrue(broken.deleteSurroundingText(1, 0))
        broken.throwBefore = true
        assertFalse(capture.recordBackspace(KeyboardLanguage.ESTONIAN, info, broken))
        assertEquals("", capture.pending)
    }

    @Test fun onlyOwnCompletedWordWithExactReadbackCanBecomeAnOffer() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val connection = field.onCreateInputConnection(editor())
        val info = editor()
        for (ch in "mhm") {
            assertTrue(connection.commitText(ch.toString(), 1))
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, connection))
        }
        assertTrue(connection.commitText(" ", 1))
        assertEquals("mhm", capture.completeAfterSpace(KeyboardLanguage.ESTONIAN, info, connection))
        assertNull(capture.completeAfterSpace(KeyboardLanguage.ESTONIAN, info, connection))
    }
    @Test fun appInsertedPrefixOrChangedCaretCannotBeMistakenForOurWord() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val connection = field.onCreateInputConnection(editor())
        val info = editor()
        field.setText("pre"); field.setSelection(3)
        assertTrue(connection.commitText("f", 1))
        assertFalse(capture.recordLetter("f", KeyboardLanguage.ESTONIAN, info, connection))
        capture.clear()
        field.setText(""); field.setSelection(0)
        assertTrue(connection.commitText("m", 1))
        assertTrue(capture.recordLetter("m", KeyboardLanguage.ESTONIAN, info, connection))
        field.setSelection(0)
        assertTrue(connection.commitText("h", 1))
        assertFalse(capture.recordLetter("h", KeyboardLanguage.ESTONIAN, info, connection))
    }
    @Test fun ownerLanguageAndSensitiveFieldsInvalidate() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val password = editor(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val ordinary = editor()
        val connection = field.onCreateInputConnection(ordinary)
        connection.commitText("m", 1)
        assertFalse(capture.recordLetter("m", KeyboardLanguage.ESTONIAN, password, connection))
        assertTrue(capture.recordLetter("m", KeyboardLanguage.ESTONIAN, ordinary, connection))
        connection.commitText("h", 1)
        assertFalse(capture.recordLetter("h", KeyboardLanguage.ENGLISH, ordinary, connection))
        connection.commitText(" ", 1)
        assertNull(capture.completeAfterSpace(KeyboardLanguage.ESTONIAN, ordinary, connection))
    }
    @Test fun unreadableFollowingTextCannotProveWordBoundary() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val base = field.onCreateInputConnection(info)
        val blind = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? = null
        }
        blind.commitText("m", 1)
        assertFalse(capture.recordLetter("m", KeyboardLanguage.ESTONIAN, info, blind))
    }

    @Test fun ownedBackspaceMayRemoveOnePendingLetterButExternalDeletionCannot() {
        val capture = ViipioTypedWordCapture()
        val field = EditText(RuntimeEnvironment.getApplication())
        val info = editor()
        val connection = field.onCreateInputConnection(info)
        for (ch in "mhmh") {
            connection.commitText(ch.toString(), 1)
            assertTrue(capture.recordLetter(ch.toString(), KeyboardLanguage.ESTONIAN, info, connection))
        }
        assertTrue(connection.deleteSurroundingText(1, 0))
        assertTrue(capture.recordBackspace(KeyboardLanguage.ESTONIAN, info, connection))
        connection.commitText(" ", 1)
        assertEquals("mhm", capture.completeAfterSpace(KeyboardLanguage.ESTONIAN, info, connection))
    }
}
