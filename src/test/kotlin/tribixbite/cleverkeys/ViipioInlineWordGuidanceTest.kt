package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ViipioInlineWordGuidanceTest {
    @Test fun firstHintPersistsOnlyAFlagAndDoesNotRepeatAfterRestart() {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("viipio_inline_word_guidance_test", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        assertTrue(ViipioInlineWordGuidance(prefs).claimFirstHint())
        assertFalse(ViipioInlineWordGuidance(prefs).claimFirstHint())
        assertEquals(1, prefs.all.size)
        assertTrue(prefs.all.values.single() is Boolean)
    }

    @Test fun failedCommitThatChangesMemoryLeavesHintRetryable() {
        val context: Context = RuntimeEnvironment.getApplication()
        val real = context.getSharedPreferences("viipio_inline_word_guidance_failure", Context.MODE_PRIVATE)
        assertTrue(real.edit().clear().commit())
        var fail = true
        val prefs = object : SharedPreferences by real {
            override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor by real.edit() {
                private val actual = real.edit()
                override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                    actual.putBoolean(key, value)
                    return this
                }
                override fun commit(): Boolean {
                    val wrote = actual.commit()
                    assertTrue(wrote)
                    return if (fail) false else wrote
                }
            }
        }
        val guidance = ViipioInlineWordGuidance(prefs)
        assertFalse(guidance.claimFirstHint())
        assertFalse(real.getBoolean("shown", false))
        fail = false
        assertTrue(guidance.claimFirstHint())
        assertFalse(guidance.claimFirstHint())
    }
}
