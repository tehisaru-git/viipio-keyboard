package tribixbite.cleverkeys

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tribixbite.cleverkeys.estonianui.KeyboardLanguage

@RunWith(RobolectricTestRunner::class)
class ViipioInlinePersonalOfferTest {
    private fun info(type: Int = InputType.TYPE_CLASS_TEXT) = EditorInfo().apply {
        packageName = "org.example.synthetic"
        inputType = type
    }

    @Test fun unfinishedOfferRequiresSameEditorWordCaretLanguageAndRightBoundary() {
        val editor = EditText(RuntimeEnvironment.getApplication())
        val info = info()
        val connection = editor.onCreateInputConnection(info)
        editor.setText("mhm"); editor.setSelection(3)
        val offer = ViipioInlinePersonalOffer("mhm", KeyboardLanguage.ESTONIAN,
            info, connection, 3, completed = false)
        assertTrue(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, "mhm"))
        assertFalse(offer.stillOwned(info, connection, KeyboardLanguage.ENGLISH, "mhm"))
        assertFalse(offer.stillOwned(info(), connection, KeyboardLanguage.ESTONIAN, "mhm"))
        assertFalse(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, "mh"))
        editor.setSelection(1)
        assertFalse(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, "mhm"))
        editor.setText("mhx"); editor.setSelection(3)
        assertFalse(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, "mhm"))
        editor.setText("mhmh"); editor.setSelection(3)
        assertFalse(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, "mhm"))
    }

    @Test fun completedWordCannotBeAddedWhenHostTurnsItsLeftBoundaryIntoALetter() {
        val editor = EditText(RuntimeEnvironment.getApplication())
        val info = info()
        val connection = editor.onCreateInputConnection(info)
        editor.setText(" mhm "); editor.setSelection(5)
        val offer = ViipioInlinePersonalOffer("mhm", KeyboardLanguage.ESTONIAN,
            info, connection, 5, completed = true)
        assertTrue(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, ""))
        editor.setText("xmhm "); editor.setSelection(5)
        assertFalse(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, ""))
    }

    @Test fun shortenedLeftReadbackCannotProveBoundaryEvenAtSameAbsoluteCaret() {
        val editor = EditText(RuntimeEnvironment.getApplication())
        val info = info()
        val base = editor.onCreateInputConnection(info)
        val truncated = object : android.view.inputmethod.InputConnectionWrapper(base, false) {
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? =
                super.getTextBeforeCursor(n, flags)?.drop(1)
        }
        editor.setText(" mhm"); editor.setSelection(4)
        val unfinished = ViipioInlinePersonalOffer("mhm", KeyboardLanguage.ESTONIAN,
            info, truncated, 4, completed = false)
        assertFalse(unfinished.stillOwned(info, truncated, KeyboardLanguage.ESTONIAN, "mhm"))
        editor.setText(" mhm "); editor.setSelection(5)
        val completed = ViipioInlinePersonalOffer("mhm", KeyboardLanguage.ESTONIAN,
            info, truncated, 5, completed = true)
        assertFalse(completed.stillOwned(info, truncated, KeyboardLanguage.ESTONIAN, ""))
    }

    @Test fun completedOfferRequiresExactWordSpaceAndPrivateFieldsNeverQualify() {
        val editor = EditText(RuntimeEnvironment.getApplication())
        val info = info()
        val connection = editor.onCreateInputConnection(info)
        editor.setText("mhm "); editor.setSelection(4)
        val offer = ViipioInlinePersonalOffer("mhm", KeyboardLanguage.ESTONIAN,
            info, connection, 4, completed = true)
        assertTrue(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, ""))
        editor.append(" ")
        assertFalse(offer.stillOwned(info, connection, KeyboardLanguage.ESTONIAN, ""))
        val secret = info(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val secretConnection = editor.onCreateInputConnection(secret)
        val unsafe = ViipioInlinePersonalOffer("mhm", KeyboardLanguage.ESTONIAN,
            secret, secretConnection, 5, completed = true)
        assertFalse(unsafe.stillOwned(secret, secretConnection, KeyboardLanguage.ESTONIAN, ""))
    }
}
