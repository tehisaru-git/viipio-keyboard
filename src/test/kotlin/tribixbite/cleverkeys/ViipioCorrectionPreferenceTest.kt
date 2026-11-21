package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import io.mockk.every
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViipioCorrectionPreferenceTest {
    private val key = "pairs"
    private val source = "tere"
    private val target = "terve"

    private fun pair(language: String = "et", from: String = source, to: String = target,
                     count: Any = 1): JSONObject = JSONObject().apply {
        put("language", language); put("from", from); put("to", to); put("count", count)
    }

    private fun payload(vararg pairs: JSONObject): String = JSONObject().apply {
        put("version", 1); put("pairs", JSONArray().apply { pairs.forEach(::put) })
    }.toString()

    @Test fun twoExplicitChoicesPromoteOnlyTheirOwnLanguageAndPairAndCountIsBounded() {
        val prefs = TypedFakePreferences()
        val store = ViipioCorrectionPreference(prefs)
        assertEquals(0, store.count("et", source, target))
        assertTrue(store.record("et", source, target))
        assertEquals(1, store.count("et", source, target))
        assertEquals(0, store.count("en", source, target))
        assertEquals(0, store.count("et", target, source))
        // A new instance sees the persisted observation, not an in-memory promotion.
        val reloaded = ViipioCorrectionPreference(prefs)
        assertTrue(reloaded.record("et", source, target))
        assertEquals(2, reloaded.count("et", source, target))
        assertTrue(reloaded.record("et", source, target))
        assertEquals(3, reloaded.count("et", source, target))
        assertTrue(reloaded.record("et", source, target))
        assertTrue(reloaded.record("et", source, target))
        assertEquals(4, reloaded.count("et", source, target))
        assertTrue(reloaded.record("en", source, target))
        assertEquals(1, reloaded.count("en", source, target))
        assertEquals(setOf("version", "pairs"), JSONObject(prefs.getString(key, "")!!).keys().asSequence().toSet())
    }

    @Test fun pureCountTransitionRequiresTwoOccurrencesAndAllowsReversalWithinCap() {
        assertEquals(1, ViipioCorrectionPreference.nextCount(0))
        assertEquals(2, ViipioCorrectionPreference.nextCount(1))
        assertEquals(3, ViipioCorrectionPreference.nextCount(2))
        assertEquals(4, ViipioCorrectionPreference.nextCount(4))
    }

    @Test fun caseIsCanonicalAndSameWordOrMalformedPairsCannotBeRecorded() {
        val prefs = TypedFakePreferences()
        val store = ViipioCorrectionPreference(prefs)
        for ((language, from, to) in listOf(
            Triple("de", source, target), Triple("ET", source, target),
            Triple("et", "Tere", "tere"), Triple("et", "hi there", target),
            Triple("en", "🙂", target), Triple("et", "x", target),
            Triple("en", "a".repeat(49), target), Triple("en", source, "foo1")
        )) assertFalse(store.record(language, from, to))
        assertEquals(0, prefs.commits)
        assertTrue(store.record("et", "Tere", "Terve"))
        assertEquals(1, store.count("et", "tere", "TERVE"))
        assertTrue(store.record("et", "tere", "terve"))
        assertEquals(2, store.count("et", "TERE", "terve"))
        assertEquals(0, store.count("xx", source, target))
        assertEquals(0, store.count("et", source, source))
    }

    @Test fun netPreferenceSnapshotRespectsReverseChoicesAndLanguage() {
        val store = ViipioCorrectionPreference(TypedFakePreferences())
        assertTrue(store.record("et", "tere", "tore"))
        assertTrue(store.record("et", "tere", "tore"))
        assertEquals(2, store.netFor("et", "tere")["tore"])
        assertTrue(store.record("et", "tore", "tere"))
        assertEquals(1, store.netFor("et", "tere")["tore"])
        assertEquals(-1, store.netFor("et", "tore")["tere"])
        assertTrue(store.netFor("en", "tere").isEmpty())
        assertTrue(store.netFor("et", "bad word").isEmpty())
    }

    @Test fun atMostOneHundredPairsAreRetainedAndOldestIsEvicted() {
        val prefs = TypedFakePreferences()
        val store = ViipioCorrectionPreference(prefs)
        fun word(index: Int): String = "w" + ('a' + index / 26) + ('a' + index % 26)
        for (i in 0..100) assertTrue("pair $i", store.record("en", word(i), "result"))
        assertEquals(0, store.count("en", word(0), "result"))
        assertEquals(1, store.count("en", word(100), "result"))
        assertEquals(100, JSONObject(prefs.getString(key, "")!!).getJSONArray("pairs").length())
    }

    @Test fun corruptStorageFailsClosedAndRecordNeverOverwritesIt() {
        val corrupt = listOf(
            "{", "[]", "null", "{}", """{"version":2,"pairs":[]}""",
            """{"version":"1","pairs":[]}""", """{"version":1,"pairs":{}}""",
            """{"version":1,"pairs":[],"context":"secret"}""",
            payload(pair(count = "2")), payload(pair(count = 0)), payload(pair(count = 5)),
            payload(pair(count = 1.5)), payload(pair(language = "fr")),
            payload(pair(from = "hello world")), payload(pair(from = "Tere")),
            payload(pair(to = source)), payload(pair(), pair()),
            payload(*Array(101) { i -> pair(from = "w" + ('a' + i / 26) + ('a' + i % 26)) }),
            """{"version":1,"pairs":[{"language":"et","from":"tere","to":"terve","count":2,"trace":[] }]}"""
        )
        for (raw in corrupt) {
            val prefs = TypedFakePreferences(mapOf(key to raw))
            val store = ViipioCorrectionPreference(prefs)
            assertEquals(raw, 0, store.count("et", source, target))
            assertFalse(raw, store.record("et", source, target))
            assertEquals(raw, raw, prefs.getString(key, null))
            assertEquals(raw, 0, prefs.commits)
        }
        val wrongType = TypedFakePreferences(mapOf(key to 3))
        assertFalse(ViipioCorrectionPreference(wrongType).record("et", source, target))
        assertEquals(3, wrongType.values[key])
    }

    @Test fun readFailureFailsClosedWithoutWriting() {
        val backing = TypedFakePreferences(mapOf(key to payload(pair(count = 2))))
        val broken = object : SharedPreferences by backing {
            override fun getString(k: String?, defValue: String?): String? = throw IllegalStateException("read")
        }
        val store = ViipioCorrectionPreference(broken)
        assertEquals(0, store.count("et", source, target))
        assertFalse(store.record("et", source, target))
        assertEquals(0, backing.commits)
    }

    @Test fun editPutAndCommitFailuresDoNotPromoteOrDamageOldValue() {
        for (failure in listOf("edit", "put", "commitThrow", "commitFalse")) {
            val original = payload(pair())
            val backing = TypedFakePreferences(mapOf(key to original))
            val faulty = object : SharedPreferences by backing {
                override fun edit(): SharedPreferences.Editor {
                    if (failure == "edit") throw IllegalStateException("edit")
                    val editor = backing.edit()
                    return object : SharedPreferences.Editor by editor {
                        override fun putString(k: String?, value: String?): SharedPreferences.Editor {
                            if (failure == "put") throw IllegalStateException("put")
                            editor.putString(k, value)
                            return this // preserve wrapper for commit fault injection
                        }
                        override fun commit(): Boolean = when (failure) {
                            "commitThrow" -> throw IllegalStateException("commit")
                            "commitFalse" -> false
                            else -> editor.commit()
                        }
                    }
                }
            }
            val store = ViipioCorrectionPreference(faulty)
            assertFalse(failure, store.record("et", source, target))
            assertEquals(failure, original, backing.getString(key, null))
            assertEquals(failure, 0, store.count("et", source, target)) // fail closed after a write fault
            assertFalse(failure, store.record("et", source, target))
            assertEquals(failure, 0, backing.commits)
        }
    }

    @Test fun clearErasesOnlyItsOwnFileAndReturnsDurableResult() {
        val backing = TypedFakePreferences(mapOf(key to payload(pair(count = 2)), "unrelated" to "keep"))
        val store = ViipioCorrectionPreference(backing)
        assertTrue(store.clear())
        assertEquals(0, store.count("et", source, target))
        assertFalse(backing.values.containsKey(key))
        assertEquals("keep", backing.values["unrelated"])
        assertTrue(store.clear())
        // Explicit erase can remove corrupt data, but normal record never repairs it.
        backing.edit().putString(key, "corrupt").commit()
        assertTrue(store.clear())
        assertFalse(backing.values.containsKey(key))
    }

    @Test fun clearAdvancesDurableRevisionEvenWhenNoPairsRemain() {
        val prefs = TypedFakePreferences()
        val first = ViipioCorrectionPreference(prefs)
        val captured = first.revision()
        assertNotNull(captured)
        assertTrue(first.record("et", source, target))
        assertEquals(captured, ViipioCorrectionPreference(prefs).revision())
        assertTrue(first.clear())
        val erased = ViipioCorrectionPreference(prefs).revision()
        assertNotNull(erased)
        assertNotEquals(captured, erased)
        assertEquals(0, ViipioCorrectionPreference(prefs).count("et", source, target))
        assertTrue(first.clear())
        assertNotEquals(erased, ViipioCorrectionPreference(prefs).revision())
        assertEquals(3, prefs.commits) // one record and two erasures
    }

    @Test fun failedClearCannotClaimAnAdvancedRevision() {
        val prefs = TypedFakePreferences()
        val before = ViipioCorrectionPreference(prefs).revision()
        val faulty = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val editor = prefs.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit(): Boolean = false
                }
            }
        }
        val store = ViipioCorrectionPreference(faulty)
        assertFalse(store.clear())
        assertNull(store.revision())
        assertEquals(before, ViipioCorrectionPreference(prefs).revision())
        assertEquals(0, prefs.commits)
    }

    @Test fun corruptRevisionFailsClosedUntilExplicitEraseRecovers() {
        for (bad in listOf("bad", -1L, Long.MAX_VALUE)) {
            val prefs = TypedFakePreferences(mapOf("revision" to bad))
            val store = ViipioCorrectionPreference(prefs)
            assertNull(store.revision())
            assertFalse(store.record("et", source, target))
            assertEquals(0, prefs.commits)
            assertTrue(store.clear())
            assertNotNull(ViipioCorrectionPreference(prefs).revision())
            assertTrue(store.record("et", source, target))
        }
    }

    @Test fun maximumValidRevisionCanStillBeErasedWithoutWrapping() {
        val prefs = TypedFakePreferences(mapOf("revision" to Long.MAX_VALUE - 1))
        val store = ViipioCorrectionPreference(prefs)
        assertTrue(store.clear())
        assertNotNull(store.revision())
        assertNotEquals(Long.MAX_VALUE - 1, store.revision())
    }

    @Test fun clearFailurePreservesExistingValueAndReportsFailure() {
        for (failure in listOf("edit", "remove", "putLong", "commitFalse", "commitThrow")) {
            val original = payload(pair(count = 2))
            val backing = TypedFakePreferences(mapOf(key to original, "revision" to 7L))
            val faulty = object : SharedPreferences by backing {
                override fun edit(): SharedPreferences.Editor {
                    if (failure == "edit") throw IllegalStateException("edit")
                    val editor = backing.edit()
                    return object : SharedPreferences.Editor by editor {
                        override fun remove(k: String?): SharedPreferences.Editor {
                            if (failure == "remove") throw IllegalStateException("remove")
                            editor.remove(k)
                            return this
                        }
                        override fun putLong(k: String?, value: Long): SharedPreferences.Editor {
                            if (failure == "putLong") throw IllegalStateException("putLong")
                            editor.putLong(k, value)
                            return this
                        }
                        override fun commit(): Boolean = when (failure) {
                            "commitThrow" -> throw IllegalStateException("commit")
                            "commitFalse" -> false
                            else -> editor.commit()
                        }
                    }
                }
            }
            assertFalse(failure, ViipioCorrectionPreference(faulty).clear())
            assertEquals(failure, original, backing.getString(key, null))
            assertEquals(failure, 7L, ViipioCorrectionPreference(backing).revision())
            assertEquals(failure, 0, backing.commits)
        }
    }

    @Test fun contextConstructorNeverOpensDeviceProtectedOrLockedPreferences() {
        for ((protected, unlocked) in listOf(true to true, false to false)) {
            val context = mockk<Context>()
            val userManager = mockk<UserManager>()
            every { context.applicationContext } returns context
            every { context.isDeviceProtectedStorage } returns protected
            every { context.getSystemService(UserManager::class.java) } returns userManager
            every { userManager.isUserUnlocked } returns unlocked
            every { context.getSharedPreferences(any(), any()) } throws AssertionError("opened preferences")
            val store = ViipioCorrectionPreference(context)
            assertNull(store.revision())
            assertEquals(0, store.count("et", source, target))
            assertFalse(store.record("et", source, target))
            assertFalse(store.clear())
        }
    }

    @Test fun contextConstructorUsesPrivateNamedFileOnlyAfterUnlock() {
        val context = mockk<Context>()
        val userManager = mockk<UserManager>()
        val prefs = TypedFakePreferences()
        every { context.applicationContext } returns context
        every { context.isDeviceProtectedStorage } returns false
        every { context.getSystemService(UserManager::class.java) } returns userManager
        every { userManager.isUserUnlocked } returns true
        every { context.getSharedPreferences(ViipioCorrectionPreference.PREFS_FILE, Context.MODE_PRIVATE) } returns prefs
        assertTrue(ViipioCorrectionPreference(context).record("et", source, target))
        assertEquals(1, ViipioCorrectionPreference(context).count("et", source, target))
    }
}
