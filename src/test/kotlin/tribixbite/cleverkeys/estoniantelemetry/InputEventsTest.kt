package tribixbite.cleverkeys.estoniantelemetry

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class InputEventsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val box = TestCipherBox()
    private fun controller() = TelemetryController(context, "input-${UUID.randomUUID()}", box)
    private fun editor(type: Int = InputType.TYPE_CLASS_TEXT) = EditorInfo().apply {
        packageName = "com.example.notes"
        inputType = type
    }
    private fun event(seq: Long = 1, text: String = "tere") = ViipioInputEvent(
        sessionId = "session1", seq = seq, relativeTimeMs = seq * 10,
        language = "et", kind = InputEventKind.COMMIT, text = text,
        source = InputEventSource.KEY, outcome = InputEventOutcome.APPLIED)
    private fun rows(c: TelemetryController) = ByteArrayOutputStream().also { c.exportJsonl(it) }
        .toString("UTF-8").trim().lines().filter { it.isNotBlank() }.map(::JSONObject)

    @Test fun noUnverifiableLifetimeDropTotalAfterSegmentEvictionAndRestart() {
        val c = controller()
        assertTrue(c.enableInputEvents())
        assertTrue(c.recordInputBatch(listOf(event()), editor(), dropped = 7))
        // The old batch's frame is evicted; any lifetime drop total would now be unverifiable.
        val segment = c.encryptedFileForTest().listFiles()!!.single()
        assertTrue(segment.delete())
        assertTrue(c.recordInputBatch(listOf(event(2)), editor()))
        val resumed = TelemetryController(context, c.storeName, box)
        assertEquals(1, resumed.stats().inputEvents)
        assertFalse(rows(resumed).any { it.optString("schema") == "viipio-input-drops-v1" })
    }

    @Test fun separateConsentAndBatchExportReplay() {
        val c = controller()
        assertFalse(c.recordInputBatch(listOf(event()), editor()))
        assertTrue(c.enableInputEvents())
        assertFalse(c.isEnabled)
        assertFalse(c.isManualEnabled)
        assertTrue(c.recordInputBatch(listOf(event(), event(2, " world")), editor()))
        val restored = TelemetryController(context, c.storeName, box)
        assertTrue(restored.isInputEnabled)
        assertEquals(2, restored.stats().inputEvents)
        val exported = rows(restored)
        assertEquals(listOf(1L, 2L), exported.map { it.getLong("seq") })
        assertEquals("viipio-input-v1", exported[0].getString("schema"))
        assertEquals("tere", exported[0].getString("text"))
        assertEquals(4, exported[0].getInt("textLength"))
        assertFalse(exported[0].has("pkg"))
        assertFalse(exported[0].has("field"))
        assertFalse(exported[0].has("createdMs"))
    }

    @Test fun rejectsSensitiveFieldsAndMalformedEventsAtomically() {
        val c = controller()
        assertTrue(c.enableInputEvents())
        assertFalse(c.recordInputBatch(listOf(event()), editor(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)))
        assertFalse(c.recordInputBatch(listOf(event()), editor(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)))
        assertFalse(c.recordInputBatch(listOf(event()), editor(InputType.TYPE_CLASS_NUMBER)))
        assertFalse(c.recordInputBatch(listOf(event()), editor(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)))
        assertFalse(c.recordInputBatch(listOf(event()), editor(InputType.TYPE_CLASS_PHONE)))
        assertFalse(c.recordInputBatch(listOf(event()), editor(InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME)))
        assertFalse(c.recordInputBatch(listOf(event()), editor().apply { imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING }))
        assertFalse(c.recordInputBatch(listOf(event()), editor().apply { privateImeOptions = "incognito" }))
        assertFalse(c.recordInputBatch(listOf(event(text = "a@b.com")), editor()))
        assertFalse(c.recordInputBatch(listOf(event(text = "📋")), editor()))
        assertFalse(c.recordInputBatch(listOf(event(), event(1, "again")), editor()))
        assertEquals(0, c.stats().inputEvents)
    }

    @Test fun etAndEnCommitDeleteCandidateSwipeAndLanguageCanBeReconstructed() {
        val c = controller()
        assertTrue(c.enableInputEvents())
        val batch = listOf(event(), event(2, "").copy(kind = InputEventKind.DELETE, textLength = 4),
            event(3, "hello").copy(language = "en", kind = InputEventKind.SWIPE, source = InputEventSource.SWIPE),
            event(4, "hullo").copy(language = "en", kind = InputEventKind.CANDIDATE, source = InputEventSource.CANDIDATE),
            event(5, "").copy(language = "en", kind = InputEventKind.LANGUAGE, source = InputEventSource.SYSTEM))
        assertTrue(c.recordInputBatch(batch, editor()))
        assertEquals(listOf("commit", "delete", "swipe", "candidate", "language"),
            rows(c).map { it.getString("kind") })
        assertEquals(4, rows(c)[1].getInt("textLength"))
        assertTrue(c.disable())
        assertFalse(TelemetryController(context, c.storeName, box).isInputEnabled)
    }
}
