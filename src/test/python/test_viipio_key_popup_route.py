"""The active Viipio keyboard, not inherited CleverKeys looks, owns Key pop-up."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / 'src/main/kotlin/tribixbite/cleverkeys'
SCREEN = (BASE / 'ui/settings/SettingsScreen.kt').read_text()
ACTIVITY = (BASE / 'activities/SettingsActivity.kt').read_text()
PERSISTENCE = (BASE / 'ui/settings/SettingsPersistence.kt').read_text()
SERVICE = (BASE / 'ViipioInputMethodService.kt').read_text()
STRINGS = (ROOT / 'res/values/strings.xml').read_text()
CHECK = (ROOT / 'scripts/viipio-check.py').read_text()


class KeyPopupRouteTest(unittest.TestCase):
    def test_key_popup_switch_reaches_the_active_keyboard(self):
        self.assertIn('R.string.viipio_key_popup_title', SCREEN)
        self.assertIn('saveSetting(ViipioBehaviorPreference.KEY_POPUP, it)', SCREEN)
        self.assertIn('viipioKeyPopup', ACTIVITY)
        self.assertIn('viipioKeyPopup = ViipioBehaviorPreference.enabled(prefs, key)', PERSISTENCE)
        self.assertIn('viipioKeyPopup = ViipioBehaviorPreference.enabled(prefs, ViipioBehaviorPreference.KEY_POPUP)', PERSISTENCE)
        self.assertIn('it.showKeyPreview = viipioOption(ViipioBehaviorPreference.KEY_POPUP)', SERVICE)
        self.assertIn('surface?.showKeyPreview = viipioOption(ViipioBehaviorPreference.KEY_POPUP)', SERVICE)
        self.assertIn('<string name="viipio_key_popup_title"', STRINGS)
        self.assertIn('<string name="viipio_key_popup_desc"', STRINGS)

    def test_optional_behavior_preference_is_in_the_targeted_gate(self):
        self.assertIn('"tribixbite.cleverkeys.ViipioBehaviorPreferenceTest"', CHECK)


if __name__ == '__main__':
    unittest.main()
