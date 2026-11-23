"""Personal words are local, language-scoped and reach both active suggestion paths."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
SCREEN = (ROOT / 'src/main/kotlin/tribixbite/cleverkeys/ui/settings/SettingsScreen.kt').read_text()
MANUAL = (ROOT / 'src/main/kotlin/tribixbite/cleverkeys/swipeui/ManualCompletions.kt').read_text()
CTC = (ROOT / 'src/main/kotlin/tribixbite/cleverkeys/swipe/CtcEngineAdapter.kt').read_text()
GEO = (ROOT / 'src/main/kotlin/tribixbite/cleverkeys/swipe/GeometricEngineAdapter.kt').read_text()


class PersonalWordsRouteTest(unittest.TestCase):
    def test_visible_add_delete_and_language_scope_are_used_by_active_ime(self):
        self.assertIn('viipio_personal_words_title', SCREEN)
        self.assertIn('ViipioPersonalWords(', SCREEN)
        self.assertIn('FilterChip(', SCREEN, 'active language needs an affirmative selected state')
        self.assertNotIn('enabled = language !=', SCREEN, 'disabled buttons look like the wrong language is active')
        self.assertIn('ViipioPersonalWords(', MANUAL)
        self.assertTrue('ViipioPersonalWords(context).json(lang)' in CTC)
        self.assertTrue('ViipioPersonalWords(context).json(lang)' in GEO)
    def test_inline_add_help_is_reachable_after_first_use(self):
        from xml.etree import ElementTree
        section = SCREEN.split('private fun SettingsActivity.PersonalWordsControls()', 1)[1]
        self.assertIn('stringResource(R.string.viipio_inline_word_help)', section)
        strings = ElementTree.parse(ROOT / 'res/values/strings.xml').getroot()
        help_text = next((node.text or '' for node in strings.findall('string')
                          if node.attrib.get('name') == 'viipio_inline_word_help'), '')
        self.assertIn('+', help_text)
        self.assertIn('Undo', help_text)
    def test_disabling_typed_suggestions_keeps_verified_after_space_add(self):
        text = (ROOT / 'src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt').read_text()
        refresh = text.split('private fun refreshManualCompletions() {', 1)[1].split(
            'private fun ownSelectionAfterWrite(', 1)[0]
        self.assertIn('val savedCompleted = personalOffer?.takeIf', refresh)
        disabled_branch = refresh.split('} else {', 1)[0]
        self.assertIn('personalOffer = savedCompleted', disabled_branch,
                      'turning off typing suggestions must not erase the after-Space Add fallback')
    def test_long_inline_label_can_wrap_without_ellipsizing_target_or_language(self):
        text = (ROOT / 'src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt').read_text()
        self.assertIn('maxLines = 3', text)
        self.assertIn('ellipsize = null', text)
        self.assertIn('viipio_inline_word_compact_add', text)


if __name__ == '__main__':
    unittest.main()
