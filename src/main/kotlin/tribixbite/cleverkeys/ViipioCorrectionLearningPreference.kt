package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import java.security.SecureRandom

/** Separate, default-off consent for Viipio-owned explicit candidate corrections only. */
internal object ViipioCorrectionLearningPreference {
    const val PREFS_FILE = "viipio_correction_learning"
    private const val KEY = "enabled"
    private const val EPOCH_KEY = "epoch"

    private fun available(context: Context): Boolean =
        Build.VERSION.SDK_INT < 24 || (!context.applicationContext.isDeviceProtectedStorage &&
            context.getSystemService(UserManager::class.java)?.isUserUnlocked == true)

    private fun readEpoch(prefs: SharedPreferences): Long? {
        val value = if (prefs.contains(EPOCH_KEY)) prefs.getLong(EPOCH_KEY, -1L) else 0L
        return value.takeIf { it in 0 until Long.MAX_VALUE }
    }

    /** Captured at choice time; null means no correction may be learned. */
    @Synchronized
    fun epoch(context: Context): Long? = runCatching {
        if (!available(context)) null else readEpoch(context.applicationContext
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE))
    }.getOrNull()

    @Synchronized
    fun enabled(context: Context): Boolean = runCatching {
        available(context) && context.applicationContext
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).let { prefs ->
                readEpoch(prefs) != null && prefs.getBoolean(KEY, false)
            }
    }.getOrDefault(false)

    /** Synchronous, atomic consent + epoch persistence; every change invalidates old choices. */
    @Synchronized
    fun setEnabled(context: Context, value: Boolean): Boolean = runCatching {
        if (!available(context)) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val previous = readEpoch(prefs) ?: return false
        prefs.getBoolean(KEY, false) // malformed consent cannot silently turn into opt-in
        val next = if (previous < Long.MAX_VALUE - 1) previous + 1
            else (SecureRandom().nextLong() and Long.MAX_VALUE)
                .takeIf { it != 0L && it != Long.MAX_VALUE && it != previous } ?: 1L
        val editor = prefs.edit()
        editor.putBoolean(KEY, value)
        editor.putLong(EPOCH_KEY, next)
        editor.commit()
    }.getOrDefault(false)
}
