package tribixbite.cleverkeys.swipe

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tribixbite.cleverkeys.PersonalWordRefresh
import tribixbite.cleverkeys.ViipioPersonalWords
import java.security.SecureRandom
import java.util.UUID

/** Exercises the shipping CE-word merge and trie memo without opening production preference files. */
@RunWith(AndroidJUnit4::class)
class ViipioPersonalWordWarmupTest {
    @Test
    fun durablePersonalWordChangesReplaceTheWarmTrieAndRemovalRestoresIt() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val namespace = "personal-warmup-${UUID.randomUUID()}-"
        val device = target.createDeviceProtectedStorageContext()
        val dpName = namespace + "${target.packageName}_preferences"
        val ceName = namespace + ViipioPersonalWords.PREFS_FILE
        // Prevent DirectBootAwarePreferences from migrating the device's real CE defaults
        // into this test's isolated DP file. Neither wrapper resolves the real prefs name.
        assertTrue(device.getSharedPreferences(dpName, Context.MODE_PRIVATE).edit()
            .putBoolean("need_migration", false).commit())
        val scopedDevice = object : ContextWrapper(device) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                device.getSharedPreferences(namespace + name, mode)
        }
        val scoped = object : ContextWrapper(target) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                target.getSharedPreferences(namespace + name, mode)
            override fun createDeviceProtectedStorageContext(): Context = scopedDevice
        }
        val letters = "abcdefghijklmnopqrstuvwxyz"
        val random = SecureRandom()
        val word = buildString { repeat(24) { append(letters[random.nextInt(letters.length)]) } }
        val words = ViipioPersonalWords(scoped)
        val adapter = CtcEngineAdapter(scoped) { UserDictionarySnapshot.EMPTY }
        // The callback models the IME's post-commit background warmup by requesting the
        // production adapter's lexicon. No ONNX session or latency threshold is involved.
        var warmed = 0
        val refresh = PersonalWordRefresh(words) {
            assertTrue("refresh must see the committed CE word state",
                scoped.getSharedPreferences(ViipioPersonalWords.PREFS_FILE, Context.MODE_PRIVATE)
                    .contains(ViipioPersonalWords.key("en")))
            assertNotNull("production trie refresh must succeed", adapter.trieFor("en"))
            warmed++
        }
        try {
            val before = adapter.trieFor("en")
            assertNotNull("bundled English trie must load", before)
            assertFalse("random probe must be absent from the bundled trie", before!!.contains(word))
            assertSame("unchanged lexicon must reuse its memo", before, adapter.trieFor("en"))

            assertEquals(ViipioPersonalWords.AddResult.ADDED, refresh.add("en", word))
            assertEquals(1, warmed)
            assertTrue("CE store must contain only the added word", words.words("en").single() == word)
            val afterAdd = adapter.trieFor("en")!!
            assertNotSame("post-add warmup must invalidate the old trie", before, afterAdd)
            assertTrue("new word must be present in the warm trie", afterAdd.contains(word))
            assertSame("post-add memo should be reused", afterAdd, adapter.trieFor("en"))
            assertEquals(ViipioPersonalWords.AddResult.EXISTS, refresh.add("en", word))
            assertEquals("duplicate add must not trigger another warmup", 1, warmed)

            assertTrue(refresh.remove("en", word))
            assertEquals(2, warmed)
            assertTrue(words.words("en").isEmpty())
            val afterRemove = adapter.trieFor("en")!!
            assertNotSame("post-remove warmup must invalidate the added trie", afterAdd, afterRemove)
            assertFalse("removed word must no longer be in the warm trie", afterRemove.contains(word))
            assertSame("post-remove memo should be reused", afterRemove, adapter.trieFor("en"))
            assertFalse(refresh.remove("en", word))
            assertEquals("missing word must not trigger another warmup", 2, warmed)
        } finally {
            // Even when an assertion fails, never leave the test's personal word behind.
            words.remove("en", word)
            adapter.shutdown()
            target.deleteSharedPreferences(ceName)
            device.deleteSharedPreferences(dpName)
        }
    }
}
