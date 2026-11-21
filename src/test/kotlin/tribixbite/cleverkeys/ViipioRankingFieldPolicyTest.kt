package tribixbite.cleverkeys

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test

class ViipioRankingFieldPolicyTest {
    private fun field(type: Int = InputType.TYPE_CLASS_TEXT, options: Int = 0,
                      privateOptions: String? = null) = EditorInfo().apply {
        inputType = type
        imeOptions = options
        packageName = "example.editor"
        privateImeOptions = privateOptions
    }

    @Test fun onlyOrdinaryTextPermitsTransientContextAndPersonalization() {
        assertTrue(ViipioRankingFieldPolicy.allowed(field()))
        assertFalse(ViipioRankingFieldPolicy.allowed(null))
        assertFalse(ViipioRankingFieldPolicy.allowed(field(InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD)))
        assertFalse(ViipioRankingFieldPolicy.allowed(field(InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)))
        assertFalse(ViipioRankingFieldPolicy.allowed(field(InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_URI)))
        assertFalse(ViipioRankingFieldPolicy.allowed(field(InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)))
        assertFalse(ViipioRankingFieldPolicy.allowed(field(options =
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)))
        assertFalse(ViipioRankingFieldPolicy.allowed(field(privateOptions = "incognito=true")))
        assertFalse(ViipioRankingFieldPolicy.allowed(field(privateOptions = "private=true")))
    }
}
