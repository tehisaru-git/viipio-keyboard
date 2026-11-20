"""The removed emoji-pane Clear action still needs a deliberate privacy route."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
SCREEN = (ROOT / "src/main/kotlin/tribixbite/cleverkeys/ui/settings/SettingsScreen.kt").read_text()
VIEW = (ROOT / "src/main/kotlin/tribixbite/cleverkeys/estonianemoji/EmojiPanelView.kt").read_text()


class EmojiPrivacyRouteTest(unittest.TestCase):
    def test_recent_emoji_erasure_lives_in_privacy_with_confirmation(self):
        privacy = SCREEN.split('"privacy" -> {', 1)[1].split('\n            }', 1)[0]
        self.assertIn('R.string.viipio_clear_recent_emoji_title', privacy)
        self.assertIn('showClearEmojiConfirmation = true', privacy)
        self.assertIn('EmojiPanelView.clearSavedRecents', SCREEN)
        self.assertIn('AlertDialog(', SCREEN)
        self.assertNotIn('action("Clear"', VIEW)


if __name__ == "__main__":
    unittest.main()
