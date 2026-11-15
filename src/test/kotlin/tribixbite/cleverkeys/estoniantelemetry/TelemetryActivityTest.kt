package tribixbite.cleverkeys.estoniantelemetry

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class TelemetryActivityTest {

    @Test fun failedPlaintextExportAttemptsCleanupAfterPartialWrite() {
        var cleanupCalled = false
        val partial = java.io.ByteArrayOutputStream()
        val failing = object : OutputStream() {
            override fun write(value: Int) {
                partial.write(value)
                if (partial.size() >= 4) throw java.io.IOException("disk full")
            }
        }
        val ok = TelemetryRuntime.writeExport(open = { failing },
            cleanup = { cleanupCalled = true },
            export = { out -> out.write("private word".toByteArray()) })
        assertFalse(ok)
        assertTrue(cleanupCalled)
        assertEquals("priv", partial.toString("UTF-8"))
    }

    @Test fun exportFailureWarningDoesNotPromisePlaintextWasRemoved() {
        val activity = Robolectric.buildActivity(TelemetryActivity::class.java).setup().get()
        assertTrue(activity.getString(tribixbite.cleverkeys.R.string.telemetry_export_failed)
            .contains("may remain", ignoreCase = true))
        assertTrue(activity.getString(tribixbite.cleverkeys.R.string.telemetry_input_off, 0)
            .contains("loss total unavailable"))
    }
    private fun all(root: View): List<View> = listOf(root) +
        (root as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } }.orEmpty()

    @Test fun separateManualConsentIsVisibleAndDoesNotStartOnOpen() {
        val controller = Robolectric.buildActivity(TelemetryActivity::class.java).setup()
        val views = all(controller.get().window.decorView)
        assertTrue(views.filterIsInstance<Button>().any { it.text.toString().contains("manual", ignoreCase = true) })
        assertTrue(views.filterIsInstance<TextView>().any { it.text.toString().contains("Manual: OFF") })
        controller.pause().stop().destroy()
    }

    @Test fun disabledByDefaultAndExportRequiresUserChosenDocument() {
        val controller = Robolectric.buildActivity(TelemetryActivity::class.java).setup()
        val activity = controller.get()
        val views = all(activity.window.decorView)
        val buttons = views.filterIsInstance<Button>()
        assertTrue(buttons.any { it.text.toString().contains("Start") })
        assertTrue(buttons.any { it.text.toString().contains("Export") })
        assertTrue(views.filterIsInstance<TextView>().any {
            it.text.toString().contains("OFF") || it.text.toString().contains("Off")
        })
        buttons.first { it.text.toString().contains("Export") }.performClick()
        val started = Shadows.shadowOf(activity).nextStartedActivityForResult.intent
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, started.action)
        assertTrue(started.type!!.contains("json"))
        controller.pause().stop().destroy()
    }
}
