package tribixbite.cleverkeys

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tribixbite.cleverkeys.estoniantelemetry.*

@RunWith(RobolectricTestRunner::class)
class ViipioInputEvidenceTest {
    @Test fun activeServiceWiresOnlyOwnActionPathsNotSelectionOrEditorObservations() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        val selection = source.substringAfter("override fun onUpdateSelection(")
            .substringBefore("override fun onDestroy()")
        assertFalse(selection.contains("recordInputEvent("))
        assertFalse(selection.contains("inputChanged("))
        assertTrue(source.contains("inputChanged(inputPrior, InputEventKind.COMMIT"))
        assertTrue(source.contains("inputChanged(inputPrior, InputEventKind.DELETE"))
        assertTrue(source.contains("inputChanged(inputPrior, InputEventKind.CANDIDATE"))
        assertTrue(source.contains("InputEventKind.SWIPE, committed"))
        assertTrue(source.contains("inputLanguageSwitched()"))
        assertTrue(source.contains("manualCandidate.startsWith(manualTypedPrefix"))
        val emojiRoute = source.substringAfter("if (panel != null && panel.visibility == View.VISIBLE)")
            .substringBefore("return // Swipes search nothing")
        assertTrue(emojiRoute.contains("inputLanguageSwitched()"))
        assertTrue(source.contains("inputEvidence.reset()"))
    }

    private fun info(type: Int = InputType.TYPE_CLASS_TEXT) = EditorInfo().apply {
        inputType = type; packageName = "test.editor"
    }
    private fun field() = EditText(RuntimeEnvironment.getApplication())
    private val rows = mutableListOf<ViipioInputEvent>()
    private val sink: (ViipioInputEvent, EditorInfo) -> Unit = { row, _ -> rows.add(row) }

    @Test fun etAndEnOwnWritesCorrectionsDeletionAndLanguageHaveBoundedSequence() {
        val evidence = ViipioInputEvidence({ 100L }, { "session-a" })
        val field = field()
        val connection = field.onCreateInputConnection(EditorInfo())
        val editor = info()
        fun before(lang: String) = evidence.capture(editor, connection, lang, true)
        val first = before("et")
        assertTrue(connection.commitText("õ", 1))
        assertTrue(evidence.changed(first, editor, connection, "et", true, InputEventKind.COMMIT,
            "õ", InputEventSource.KEY, emit = sink))
        val swipe = before("et")
        assertTrue(connection.commitText("tere", 1))
        assertTrue(evidence.changed(swipe, editor, connection, "et", true, InputEventKind.SWIPE,
            "tere", InputEventSource.SWIPE, emit = sink))
        assertTrue(evidence.language(editor, connection, true, "en", sink))
        val english = before("en")
        assertTrue(connection.commitText("a", 1))
        assertTrue(evidence.changed(english, editor, connection, "en", true, InputEventKind.COMMIT,
            "a", InputEventSource.KEY, emit = sink))
        val replacement = before("en")
        assertTrue(connection.setSelection(field.length() - 1, field.length()))
        assertTrue(connection.commitText("b", 1))
        assertTrue(evidence.changed(replacement, editor, connection, "en", true,
            InputEventKind.CANDIDATE, "b", InputEventSource.CANDIDATE,
            InputEventOutcome.REPLACED, "a", sink))
        val deletion = before("en")
        assertTrue(connection.deleteSurroundingTextInCodePoints(1, 0))
        assertTrue(evidence.changed(deletion, editor, connection, "en", true,
            InputEventKind.DELETE, "", InputEventSource.KEY, emit = sink))
        assertEquals(listOf("commit", "swipe", "language", "commit", "candidate", "delete"),
            rows.map { it.kind.wire })
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), rows.map { it.seq })
        assertEquals(listOf("et", "et", "en", "en", "en", "en"), rows.map { it.language })
        assertEquals(1, rows.last().textLength)
        evidence.reset()
        val again = before("et")
        assertTrue(connection.commitText("x", 1))
        assertTrue(evidence.changed(again, editor, connection, "et", true,
            InputEventKind.COMMIT, "x", InputEventSource.KEY, emit = sink))
        assertEquals(1L, rows.last().seq)
    }

    @Test fun sensitiveFieldsAndIndependentConsentNeverEmit() {
        val evidence = ViipioInputEvidence()
        val field = field()
        val connection = field.onCreateInputConnection(EditorInfo())
        val password = info(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val url = info(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        val noLearning = info().apply { imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING }
        val pin = info(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val phone = info(InputType.TYPE_CLASS_PHONE)
        val date = info(InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE)
        for (editor in listOf(password, url, noLearning, pin, phone, date)) {
            assertNull(evidence.capture(editor, connection, "et", true))
            assertFalse(evidence.language(editor, connection, true, "et", sink))
        }
        assertNull(evidence.capture(info(), connection, "et", false))
        assertTrue(rows.isEmpty())
    }

    @Test fun rejectedWritesExternalChangeAndWrongConnectionNeverEmit() {
        val evidence = ViipioInputEvidence()
        val field = field()
        val original = field.onCreateInputConnection(EditorInfo())
        val rejecting = object : InputConnectionWrapper(original, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int) = false
        }
        val editor = info()
        val before = evidence.capture(editor, rejecting, "et", true)
        assertFalse(rejecting.commitText("a", 1))
        assertFalse(evidence.changed(before, editor, rejecting, "et", true,
            InputEventKind.COMMIT, "a", InputEventSource.KEY, emit = sink))
        val prior = evidence.capture(editor, original, "et", true)
        field.setText("external")
        field.setSelection(field.length())
        assertFalse(evidence.changed(prior, editor, original, "et", true,
            InputEventKind.COMMIT, "x", InputEventSource.KEY, emit = sink))
        assertFalse(evidence.changed(prior, editor, rejecting, "et", true,
            InputEventKind.COMMIT, "x", InputEventSource.KEY, emit = sink))
        assertTrue(rows.isEmpty())
    }

    @Test fun stagedSeparatorIsRecordedOnlyAsItsVerifiedPayloadAndConsentRevocationDropsIt() {
        val evidence = ViipioInputEvidence({ 20L }, { "session-b" })
        val field = field().apply { setText("tere"); setSelection(length()) }
        val connection = field.onCreateInputConnection(EditorInfo())
        val editor = info()
        val prior = evidence.capture(editor, connection, "en", true)
        assertTrue(connection.commitText(" world", 1))
        assertFalse(evidence.changed(prior, editor, connection, "en", true,
            InputEventKind.SWIPE, "world", InputEventSource.SWIPE, emit = sink))
        assertTrue(evidence.changed(prior, editor, connection, "en", true,
            InputEventKind.SWIPE, " world", InputEventSource.SWIPE, emit = sink))
        assertEquals(" world", rows.single().text)
        val next = evidence.capture(editor, connection, "en", true)
        assertTrue(connection.commitText("!", 1))
        assertFalse(evidence.changed(next, editor, connection, "en", false,
            InputEventKind.COMMIT, "!", InputEventSource.KEY, emit = sink))
        assertEquals(1, rows.size)
    }
}
