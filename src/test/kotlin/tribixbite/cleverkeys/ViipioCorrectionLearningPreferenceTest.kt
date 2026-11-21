package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ViipioCorrectionLearningPreferenceTest {
    @Test fun correctionLearningIsOptInAndCanBeDisabled() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(ViipioCorrectionLearningPreference.PREFS_FILE,
            Context.MODE_PRIVATE).edit().clear().commit()
        assertFalse(ViipioCorrectionLearningPreference.enabled(context))
        assertTrue(ViipioCorrectionLearningPreference.setEnabled(context, true))
        assertTrue(ViipioCorrectionLearningPreference.enabled(context))
        assertTrue(ViipioCorrectionLearningPreference.setEnabled(context, false))
        assertFalse(ViipioCorrectionLearningPreference.enabled(context))
    }

    private fun contextFor(prefs: SharedPreferences): Context = mockk<Context>().also { context ->
        val manager = mockk<UserManager>()
        every { context.applicationContext } returns context
        every { context.isDeviceProtectedStorage } returns false
        every { context.getSystemService(UserManager::class.java) } returns manager
        every { manager.isUserUnlocked } returns true
        every { context.getSharedPreferences(ViipioCorrectionLearningPreference.PREFS_FILE,
            Context.MODE_PRIVATE) } returns prefs
    }

    @Test fun optOutThenOptInInvalidatesCapturedEpochAcrossInstances() {
        val prefs = TypedFakePreferences()
        val context = contextFor(prefs)
        val captured = ViipioCorrectionLearningPreference.epoch(context)
        assertNotNull(captured)
        assertTrue(ViipioCorrectionLearningPreference.setEnabled(context, true))
        val optedIn = ViipioCorrectionLearningPreference.epoch(context)
        assertNotEquals(captured, optedIn)
        assertTrue(ViipioCorrectionLearningPreference.setEnabled(context, false))
        assertFalse(ViipioCorrectionLearningPreference.enabled(context))
        assertTrue(ViipioCorrectionLearningPreference.setEnabled(context, true))
        assertTrue(ViipioCorrectionLearningPreference.enabled(context))
        assertNotEquals(optedIn, ViipioCorrectionLearningPreference.epoch(context))
        assertEquals(3, prefs.commits)
    }

    @Test fun failedConsentCommitDoesNotClaimAdvancedEpoch() {
        val prefs = TypedFakePreferences()
        val context = contextFor(object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val editor = prefs.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit(): Boolean = false
                }
            }
        })
        val captured = ViipioCorrectionLearningPreference.epoch(context)
        assertFalse(ViipioCorrectionLearningPreference.setEnabled(context, true))
        assertEquals(captured, ViipioCorrectionLearningPreference.epoch(context))
        assertFalse(ViipioCorrectionLearningPreference.enabled(context))
        assertEquals(0, prefs.commits)
    }

    @Test fun corruptEpochFailsClosedAndCannotBeOverwrittenByConsentChange() {
        for (bad in listOf("broken", -1L, Long.MAX_VALUE)) {
            val prefs = TypedFakePreferences(mapOf("epoch" to bad, "enabled" to true))
            val context = contextFor(prefs)
            assertNull(ViipioCorrectionLearningPreference.epoch(context))
            assertFalse(ViipioCorrectionLearningPreference.enabled(context))
            assertFalse(ViipioCorrectionLearningPreference.setEnabled(context, true))
            assertEquals(0, prefs.commits)
        }
    }

    @Test fun maximumValidEpochCanStillOptOutWithoutWrapping() {
        val backing = TypedFakePreferences(mapOf("epoch" to Long.MAX_VALUE - 1, "enabled" to true))
        val context = contextFor(backing)
        assertTrue(ViipioCorrectionLearningPreference.setEnabled(context, false))
        assertFalse(ViipioCorrectionLearningPreference.enabled(context))
        assertNotNull(ViipioCorrectionLearningPreference.epoch(context))
        assertNotEquals(Long.MAX_VALUE - 1, ViipioCorrectionLearningPreference.epoch(context))
    }

    @Test fun epochWriteFailureCannotEnableConsentOrAdvanceEpoch() {
        val backing = TypedFakePreferences()
        val prefs = object : SharedPreferences by backing {
            override fun edit(): SharedPreferences.Editor {
                val editor = backing.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
                        throw IllegalStateException("epoch write")
                }
            }
        }
        val context = contextFor(prefs)
        assertFalse(ViipioCorrectionLearningPreference.setEnabled(context, true))
        assertFalse(ViipioCorrectionLearningPreference.enabled(context))
        assertEquals(0L, ViipioCorrectionLearningPreference.epoch(context))
        assertEquals(0, backing.commits)
    }

    @Test fun corruptConsentIsNeverInterpretedAsTrue() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(ViipioCorrectionLearningPreference.PREFS_FILE,
            Context.MODE_PRIVATE).edit().putString("enabled", "true").commit()
        assertFalse(ViipioCorrectionLearningPreference.enabled(context))
    }
}
