package tribixbite.cleverkeys

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Fail closed before reading even a transient editor-context window. */
internal object ViipioRankingFieldPolicy {
    fun allowed(info: EditorInfo?): Boolean {
        if (info == null || info.packageName.isNullOrBlank()) return false
        val type = info.inputType
        return type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
            type and InputType.TYPE_MASK_VARIATION == InputType.TYPE_TEXT_VARIATION_NORMAL &&
            type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS == 0 &&
            info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0 &&
            info.privateImeOptions?.contains("incognito", ignoreCase = true) != true &&
            info.privateImeOptions?.contains("private", ignoreCase = true) != true
    }
}
