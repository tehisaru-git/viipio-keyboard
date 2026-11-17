package tribixbite.cleverkeys

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Pins v1.2.6 / v1.2.8's "Practice typing inside settings" (#1134) — the Test Keyboard
 * panel in Settings.
 *
 * [legacyPanelStateIsHeldInViewModel] exercises the old panel's state holder and Activity
 * delegation. It does **not** recreate an Activity, and it does not cover Viipio's active
 * LauncherActivity practice text, which is transient `remember` state rather than saved state.
 *
 * The legacy panel's state/search/resource contracts below remain historical checks. Viipio's
 * active Settings home now routes to the separate LauncherActivity practice editor, so
 * [practiceIsReachableFromActiveSettingsHome] pins the current source route and field instead
 * of falsely demanding the dormant TestKeyboardSection be composed into the new home. This is
 * not an on-device navigation or practice-state-lifetime test.
 *
 * Mock tier: `SettingsViewModel`'s Compose state needs android.jar (the Android Compose
 * runtime's state factory implements `Parcelable`). Run with
 * `scripts/gradle-guard.sh runMockTests -PtestClass=TestKeyboardSectionTest`.
 */
class TestKeyboardSectionTest {

    private fun read(path: String): String {
        val file = File(path)
        check(file.isFile) { "${file.path} not found — run with the project root as CWD." }
        return file.readText()
    }

    private val sectionSource by lazy {
        read("src/main/kotlin/tribixbite/cleverkeys/ui/settings/sections/TestKeyboardSection.kt")
    }

    // =========================================================================
    // State ownership (behavioural)
    // =========================================================================

    @Test
    fun legacyPanelStateIsHeldInViewModel() {
        val viewModel = SettingsViewModel()

        assertWithMessage("the practice field starts empty")
            .that(viewModel.testKeyboardText).isEmpty()
        assertWithMessage("the panel starts collapsed")
            .that(viewModel.testKeyboardExpanded).isFalse()

        viewModel.testKeyboardText = "the quick brown fox"
        viewModel.testKeyboardExpanded = true

        // This only verifies state holder delegation; it does not perform a rotation.
        assertWithMessage("typed practice text must be held outside the Activity")
            .that(viewModel.testKeyboardText).isEqualTo("the quick brown fox")
        assertWithMessage("panel expansion must be held outside the Activity")
            .that(viewModel.testKeyboardExpanded).isTrue()

        val activitySource = read("src/main/kotlin/tribixbite/cleverkeys/activities/SettingsActivity.kt")
        for (property in listOf("testKeyboardText", "testKeyboardExpanded")) {
            assertWithMessage("SettingsActivity.$property must delegate to the ViewModel, not own state")
                .that(
                    Regex("""var $property[\s\S]{0,80}?get\(\)\s*=\s*settingsViewModel\.$property""")
                        .containsMatchIn(activitySource)
                ).isTrue()
        }
    }

    // =========================================================================
    // The panel itself (source/resource)
    // =========================================================================

    @Test
    fun practiceIsReachableFromActiveSettingsHome() {
        val screen = read("src/main/kotlin/tribixbite/cleverkeys/ui/settings/SettingsScreen.kt")
        assertWithMessage("active settings must have a setup/practice destination and route")
            .that(Regex("""SettingsDestination\(stringResource\(R.string.viipio_setup_and_try_title\),[\s\S]{0,240}?startActivity\(Intent\(this@SettingsScreen, LauncherActivity::class.java\)\)""")
                .containsMatchIn(screen)).isTrue()
        val practice = read("src/main/kotlin/tribixbite/cleverkeys/activities/LauncherActivity.kt")
        assertWithMessage("setup/practice must render an editable field")
            .that(practice).contains("OutlinedTextField(")
        assertWithMessage("typed practice text must remain editable")
            .that(practice).contains("value = testText, onValueChange = { testText = it }")
        assertWithMessage("practice field must request sentence capitalization")
            .that(practice).contains("keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)")
    }

    @Test
    fun panelIsFindableInSearch() {
        val search = read("src/main/kotlin/tribixbite/cleverkeys/ui/settings/SettingsSearch.kt")
        assertWithMessage("settings search must know the panel's display name (its localized title)")
            .that(search).contains("\"testKeyboard\" -> R.string.test_keyboard_section_title")
        assertWithMessage("navigating to a search hit must expand the panel")
            .that(search).contains("\"testKeyboard\" -> testKeyboardExpanded = true")
        assertWithMessage("collapsing all sections must include this one, or search leaves it open")
            .that(search).contains("testKeyboardExpanded = false")
    }

    @Test
    fun panelStringsExist() {
        val strings = read("res/values/strings.xml")
        for (name in listOf("test_keyboard_section_title", "test_keyboard_hint", "common_clear")) {
            val value = Regex("""<string name="$name"[^>]*>([^<]*)</string>""").find(strings)
                ?.groupValues?.get(1)
            assertWithMessage("res/values/strings.xml must define $name")
                .that(value).isNotNull()
            assertWithMessage("$name must not be blank — it labels the panel")
                .that(value!!.trim()).isNotEmpty()
        }
        assertWithMessage("the section title must name the feature the release note announced")
            .that(strings).contains("<string name=\"test_keyboard_section_title\">⌨️ Test Keyboard</string>")
    }
}
