package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ViipioPersonalWordsTest {
    private fun store(name: String): Pair<android.content.SharedPreferences, ViipioPersonalWords> {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        return prefs to ViipioPersonalWords(prefs)
    }

    @Test fun mhmIsOnlyInTheExplicitEstonianAppLocalLexiconUntilDeleted() {
        val (prefs, words) = store("viipio-own-mhm")
        assertEquals(ViipioPersonalWords.AddResult.ADDED, words.add("et", "mhm"))
        assertEquals(listOf("mhm"), words.words("et"))
        assertTrue(words.words("en").isEmpty())
        assertEquals(255, JSONObject(prefs.getString("custom_words_et", "{}")!!).getInt("mhm"))
        assertFalse(prefs.contains("custom_words_en"))
        assertEquals(ViipioPersonalWords.AddResult.EXISTS, words.add("et", "MHM"))
        assertTrue(words.remove("et", "mhm"))
        assertTrue(words.words("et").isEmpty())
    }

    @Test fun malformedOrNonLatinInputCannotPolluteEitherLanguage() {
        val (prefs, words) = store("viipio-own-validation")
        for (value in listOf("", "a", " mhm", "lo l", "mhm1", "🙂hi", "тест", "a".repeat(49))) {
            assertEquals(value, ViipioPersonalWords.AddResult.INVALID, words.add("et", value))
        }
        assertEquals(ViipioPersonalWords.AddResult.INVALID, words.add("fr", "mhm"))
        assertFalse(prefs.contains("custom_words_et"))
    }

    @Test fun malformedExistingDataIsNeverSilentlyOverwritten() {
        val (prefs, words) = store("viipio-own-malformed")
        prefs.edit().putString("custom_words_et", "not json").commit()
        assertEquals(ViipioPersonalWords.AddResult.UNAVAILABLE, words.add("et", "mhm"))
        assertEquals("not json", prefs.getString("custom_words_et", null))
        assertFalse(words.remove("et", "mhm"))
    }

    @Test fun existingPersonalEntriesStayIntactAndStoreHasABound() {
        val (prefs, words) = store("viipio-own-bound")
        prefs.edit().putString("custom_words_et", "{\"Tere\":12}").commit()
        assertEquals(ViipioPersonalWords.AddResult.ADDED, words.add("et", "mhm"))
        val stored = JSONObject(prefs.getString("custom_words_et", "{}")!!)
        assertEquals(12, stored.getInt("Tere"))
        assertEquals(255, stored.getInt("mhm"))
        val full = JSONObject()
        for (index in 0 until 200) full.put("entry${'a' + index / 26}${'a' + index % 26}", 50)
        prefs.edit().putString("custom_words_et", full.toString()).commit()
        assertEquals(ViipioPersonalWords.AddResult.FULL, words.add("et", "lisasona"))
    }

    @Test fun userAddedWordsLiveInCredentialProtectedStorageAndAreUnavailableBeforeUnlock() {
        val context: Context = RuntimeEnvironment.getApplication()
        val file = "viipio-ce-isolation"
        val credential = context.getSharedPreferences(file, Context.MODE_PRIVATE)
        credential.edit().clear().commit()
        val words = ViipioPersonalWords(context, fileName = file, isUnlocked = { true })
        assertEquals(ViipioPersonalWords.AddResult.ADDED, words.add("et", "mhm"))
        assertEquals(255, JSONObject(credential.getString("custom_words_et", "{}")!!).getInt("mhm"))
        val locked = ViipioPersonalWords(context, fileName = file, isUnlocked = { false })
        assertTrue(locked.words("et").isEmpty())
        assertNull(locked.containsExact("et", "mhm"))
        assertEquals(ViipioPersonalWords.AddResult.UNAVAILABLE, locked.add("et", "tere"))
        assertFalse(locked.remove("et", "mhm"))
        assertEquals(listOf("mhm"), words.words("et"))
    }

    @Test fun storageWriteExceptionsLeaveExistingWordsIntactAndReturnUnavailable() {
        val (base, _) = store("viipio-write-failure")
        base.edit().putString("custom_words_et", "{\"tere\":12}").commit()
        for (stage in listOf("edit", "putString", "commit")) {
            val failing = object : SharedPreferences by base {
                override fun edit(): SharedPreferences.Editor {
                    if (stage == "edit") throw IllegalStateException("no editor")
                    return object : SharedPreferences.Editor by base.edit() {
                        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                            if (stage == "putString") throw IllegalStateException("no write")
                            return this
                        }
                        override fun commit(): Boolean {
                            if (stage == "commit") throw IllegalStateException("no commit")
                            return false
                        }
                    }
                }
            }
            val words = ViipioPersonalWords(failing)
            assertEquals(stage, ViipioPersonalWords.AddResult.UNAVAILABLE, words.add("et", "mhm"))
            assertFalse(stage, words.remove("et", "tere"))
            assertEquals(stage, "{\"tere\":12}", base.getString("custom_words_et", null))
        }
    }

    /** Simulate Android applying an edit in memory before reporting a failed disk commit. */
    private fun memoryChangedButCommitReportsFailure(base: SharedPreferences): SharedPreferences =
        object : SharedPreferences by base {
            override fun edit(): SharedPreferences.Editor {
                val real = base.edit()
                return object : SharedPreferences.Editor by real {
                    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                        real.putString(key, value)
                        return this
                    }
                    override fun commit(): Boolean {
                        real.commit()
                        return false
                    }
                }
            }
        }

    @Test fun failedAddCommitRestoresMemoryAndRemainsRetryable() {
        val (base, _) = store("viipio-commit-false-add")
        val words = ViipioPersonalWords(memoryChangedButCommitReportsFailure(base))
        assertEquals(ViipioPersonalWords.AddResult.UNAVAILABLE, words.add("et", "mhm"))
        assertTrue(words.words("et").isEmpty())
        assertFalse(base.contains(ViipioPersonalWords.key("et")))
        assertEquals(ViipioPersonalWords.AddResult.ADDED, ViipioPersonalWords(base).add("et", "mhm"))
    }

    @Test fun failedUndoCommitRestoresMemoryAndRemainsRetryable() {
        val (base, _) = store("viipio-commit-false-undo")
        assertEquals(ViipioPersonalWords.AddResult.ADDED, ViipioPersonalWords(base).add("et", "mhm"))
        val words = ViipioPersonalWords(memoryChangedButCommitReportsFailure(base))
        assertFalse(words.remove("et", "mhm"))
        assertEquals(listOf("mhm"), words.words("et"))
        assertTrue(ViipioPersonalWords(base).remove("et", "mhm"))
    }

    @Test fun successfulPersonalMutationRefreshesDecoderOnlyAfterDurableWrite() {
        val (prefs, words) = store("viipio-personal-refresh")
        val observed = mutableListOf<String>()
        val refresh = PersonalWordRefresh(words) { observed.add(prefs.getString("custom_words_et", "")!!) }
        assertEquals(ViipioPersonalWords.AddResult.ADDED, refresh.add("et", "mhm"))
        assertTrue(observed.single().contains("mhm"))
        assertEquals(ViipioPersonalWords.AddResult.EXISTS, refresh.add("et", "mhm"))
        assertEquals(ViipioPersonalWords.AddResult.INVALID, refresh.add("fr", "mhm"))
        assertEquals(1, observed.size)
        assertFalse(refresh.remove("et", "puudub"))
        assertEquals(1, observed.size)
        assertTrue(refresh.remove("et", "mhm"))
        assertEquals("{}", observed.last())
        assertEquals(2, observed.size)
    }

    @Test fun mixedInvalidStoredValuesFailClosedInsteadOfHidingGoodSwipeWords() {
        val (prefs, words) = store("viipio-mixed-invalid")
        prefs.edit().putString("custom_words_et", "{\"tere\":12,\"mhm\":\"bad\"}").commit()
        assertTrue(words.words("et").isEmpty())
        assertEquals("{}", words.json("et"))
        assertEquals(ViipioPersonalWords.AddResult.UNAVAILABLE, words.add("et", "tore"))
        assertEquals("{\"tere\":12,\"mhm\":\"bad\"}", prefs.getString("custom_words_et", null))
    }
}
