package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import org.json.JSONObject
import java.util.Locale

/** New user-entered words are CE-only; legacy DP custom words are read separately by swipe. */
internal class ViipioPersonalWords private constructor(
    private val preferences: () -> SharedPreferences,
    private val isUnlocked: () -> Boolean,
) {
    constructor(context: Context, fileName: String = PREFS_FILE,
                isUnlocked: () -> Boolean = { userUnlocked(context) }) : this(
        { context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE) }, isUnlocked)

    /** Isolated storage injection for unit tests, never used in the active app path. */
    @androidx.annotation.VisibleForTesting
    constructor(prefs: SharedPreferences) : this({ prefs }, { true })

    private val prefs: SharedPreferences by lazy(preferences)
    enum class AddResult { ADDED, INVALID, EXISTS, FULL, UNAVAILABLE }

    fun words(language: String): List<String> = read(language)?.let { json ->
        json.keys().asSequence().filter(::validWord).sortedBy { it.lowercase(Locale.ROOT) }.toList()
    }.orEmpty()

    /** Null means unreadable CE storage; unknown must not be inferred from an empty UI list. */
    fun containsExact(language: String, word: String): Boolean? =
        if (!validLanguage(language) || !validWord(word)) null
        else read(language)?.keys()?.asSequence()?.any { it.equals(word, ignoreCase = true) }

    /** Validated raw JSON for the shared legacy + CE swipe-lexicon merge. */
    fun json(language: String): String = read(language)?.toString() ?: "{}"

    fun add(language: String, word: String): AddResult {
        if (!validLanguage(language) || !validWord(word)) return AddResult.INVALID
        val json = read(language) ?: return AddResult.UNAVAILABLE
        if (json.keys().asSequence().any { it.equals(word, ignoreCase = true) }) return AddResult.EXISTS
        if (json.length() >= MAX_WORDS) return AddResult.FULL
        json.put(word, 255)
        val storageKey = key(language)
        return try {
            val previous = prefs.getString(storageKey, null)
            if (commitOrRestore(storageKey, json.toString(), previous)) AddResult.ADDED
            else AddResult.UNAVAILABLE
        } catch (_: Exception) { AddResult.UNAVAILABLE }
    }

    fun remove(language: String, word: String): Boolean {
        if (!validLanguage(language) || !validWord(word)) return false
        val json = read(language) ?: return false
        val matching = json.keys().asSequence().filter { it.equals(word, ignoreCase = true) }.toList()
        if (matching.isEmpty()) return false
        matching.forEach { json.remove(it) }
        val storageKey = key(language)
        return try {
            val previous = prefs.getString(storageKey, null)
            commitOrRestore(storageKey, json.toString(), previous)
        } catch (_: Exception) { false }
    }

    /** A failed commit may already have changed SharedPreferences' in-memory map. */
    private fun commitOrRestore(storageKey: String, next: String, previous: String?): Boolean {
        val committed = try { prefs.edit().putString(storageKey, next).commit() }
            catch (_: Exception) { false }
        if (!committed) {
            // Revert only our attempted value; never overwrite a newer concurrent edit.
            try {
                if (prefs.getString(storageKey, null) == next)
                    prefs.edit().putString(storageKey, previous).commit()
            } catch (_: Exception) { /* No durable result may be claimed on failure. */ }
        }
        return committed
    }

    private fun read(language: String): JSONObject? {
        if (!validLanguage(language)) return null
        if (!runCatching { isUnlocked() }.getOrDefault(false)) return null
        return try {
            val json = JSONObject(prefs.getString(key(language), "{}") ?: "{}")
            if (json.length() > MAX_WORDS) return null
            for (word in json.keys()) {
                val frequency = json.get(word)
                if (!validWord(word) || frequency !is Number ||
                    frequency.toDouble() !in 1.0..1000.0 ||
                    frequency.toDouble() != frequency.toInt().toDouble()) return null
            }
            json
        } catch (_: Exception) { null } // Never overwrite malformed or wrong-typed storage.
    }

    companion object {
        const val PREFS_FILE = "viipio_personal_words"
        private const val MAX_WORDS = 200
        private fun userUnlocked(context: Context): Boolean = Build.VERSION.SDK_INT < 24 ||
            (!context.applicationContext.isDeviceProtectedStorage &&
                context.getSystemService(UserManager::class.java)?.isUserUnlocked == true)
        fun key(language: String): String = "custom_words_$language"
        fun validLanguage(language: String): Boolean = language == "et" || language == "en"
        fun validWord(word: String): Boolean = word.length in 2..48 && word.all { ch ->
            ch.isLetter() && Character.UnicodeScript.of(ch.code) == Character.UnicodeScript.LATIN
        }
    }
}
