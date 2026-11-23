package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager

/** One nonpersonal CE flag for a nonblocking, first-use inline-add hint. */
internal class ViipioInlineWordGuidance(private val prefs: SharedPreferences) {
    companion object {
        private const val FILE = "viipio_inline_word_guidance"
        private const val SHOWN = "shown"

        fun from(context: Context): ViipioInlineWordGuidance? = runCatching {
            val app = context.applicationContext
            if (app.isDeviceProtectedStorage ||
                app.getSystemService(UserManager::class.java)?.isUserUnlocked != true) null
            else ViipioInlineWordGuidance(app.getSharedPreferences(FILE, Context.MODE_PRIVATE))
        }.getOrNull()
    }

    /** Return true only after the flag is durably recorded; do not repeat on each prefix. */
    fun claimFirstHint(): Boolean = runCatching {
        if (prefs.getBoolean(SHOWN, false)) false
        else {
            val saved = prefs.edit().putBoolean(SHOWN, true).commit()
            if (!saved && prefs.getBoolean(SHOWN, false)) {
                // A failed disk write can still change the in-memory preference map.
                prefs.edit().putBoolean(SHOWN, false).commit()
            }
            saved
        }
    }.getOrDefault(false)
}
