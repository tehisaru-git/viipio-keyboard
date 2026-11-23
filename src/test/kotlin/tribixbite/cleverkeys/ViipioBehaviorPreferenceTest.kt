package tribixbite.cleverkeys

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ViipioBehaviorPreferenceTest {
    @Test fun activeOptionalBehaviorsAreOnByDefaultAndIndependentlySwitchable() {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("viipio-optional-behaviors", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val keys = listOf(ViipioBehaviorPreference.TYPED_SUGGESTIONS,
            ViipioBehaviorPreference.BACKSPACE_WORD_UNDO,
            ViipioBehaviorPreference.GLIDE_DELETE,
            ViipioBehaviorPreference.CORNER_SYMBOLS)
        keys.forEach { assertTrue(it, ViipioBehaviorPreference.enabled(prefs, it)) }
        prefs.edit().putBoolean(ViipioBehaviorPreference.GLIDE_DELETE, false).commit()
        assertFalse(ViipioBehaviorPreference.enabled(prefs, ViipioBehaviorPreference.GLIDE_DELETE))
        assertTrue(ViipioBehaviorPreference.enabled(prefs, ViipioBehaviorPreference.CORNER_SYMBOLS))
        assertTrue("Key pop-up must be enabled on an upgrade and a fresh install",
            ViipioBehaviorPreference.enabled(prefs, "viipio_key_popup"))
        prefs.edit().putBoolean("viipio_key_popup", false).commit()
        assertFalse(ViipioBehaviorPreference.enabled(prefs, "viipio_key_popup"))
        assertTrue(ViipioBehaviorPreference.enabled(prefs, ViipioBehaviorPreference.CORNER_SYMBOLS))
    }

    @Test fun wrongTypeOrUnknownKeyDoesNotSilentlyEnableAnUnknownAction() {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("viipio-optional-invalid", Context.MODE_PRIVATE)
        prefs.edit().putString(ViipioBehaviorPreference.CORNER_SYMBOLS, "bad").commit()
        assertTrue(ViipioBehaviorPreference.enabled(prefs, ViipioBehaviorPreference.CORNER_SYMBOLS))
        assertFalse(ViipioBehaviorPreference.enabled(prefs, "not_a_viipio_setting"))
    }
}
