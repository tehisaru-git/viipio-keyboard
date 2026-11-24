"""Update compatibility and public artifact identity for the Viipio release."""
from pathlib import Path
import json
import re
import unittest
from urllib.parse import parse_qs, unquote, urlsplit

ROOT = Path(__file__).resolve().parents[3]
GRADLE = (ROOT / "build.gradle").read_text()
README = (ROOT / "README.md").read_text()


class ViipioReleaseMetadataTest(unittest.TestCase):
    def test_version_and_apk_name_are_branded(self):
        versions = dict(re.findall(r"ext\.(VERSION_MAJOR|VERSION_MINOR|VERSION_PATCH)\s*=\s*(\d+)", GRADLE))
        self.assertEqual(versions, {"VERSION_MAJOR": "0", "VERSION_MINOR": "10", "VERSION_PATCH": "3"})
        self.assertIn('ViipioKeyboard-v${variant.versionName}-${abi}.apk', GRADLE)
        self.assertIn('applicationId "io.github.tehisaru.viipio"', GRADLE)

    def test_build_variants_do_not_override_branded_app_label(self):
        self.assertNotIn('Estonian Swipe (Test)', GRADLE)
        self.assertNotIn('Estonian Swipe (Debug)', GRADLE)
        self.assertIn('resValue "string", "app_name", "Viipio keyboard (Debug)"', GRADLE)

    def test_english_dictionary_is_reproduced_from_tracked_ckdt_source(self):
        source = ROOT / 'scripts/dictionaries/en/en_enhanced.bin'
        self.assertTrue(source.is_file())
        with source.open('rb') as dictionary:
            self.assertEqual(dictionary.read(4), b'CKDT')
        self.assertTrue('scripts/dictionaries/en/en_enhanced.bin' in GRADLE,
                        'build must copy the tracked CKDT dictionary')
        self.assertTrue('en_enhanced.bin' in GRADLE, 'English asset must be packaged')

    def test_homepage_stays_evergreen_and_puts_obtainium_first(self):
        self.assertIn("## Install with Obtainium", README)
        self.assertIn("## Features", README)
        self.assertLess(README.index("## Install with Obtainium"), README.index("## Features"))
        self.assertIn("https://github.com/tehisaru-git/viipio-keyboard", README)
        self.assertIn("https://github.com/tehisaru-git/viipio-keyboard/releases/latest", README)
        self.assertIn("io.github.tehisaru.viipio", README)
        self.assertIn("arm64-v8a", README)
        self.assertIn("source code is distributed under [GPL-3.0](LICENSE)", README)
        self.assertIn("[NOTICE](NOTICE)", README)
        self.assertNotRegex(README, r"\bv\d+\.\d+\.\d+\b")
        for obsolete in ("previously called", "Estonian Swipe", "No Gboard code",
                         "Pixel 8", "old package", "new Android app, not an in-place update"):
            self.assertNotIn(obsolete, README)

    def test_obtainium_shortcut_and_scannable_qr_have_correct_app_source(self):
        match = re.search(r"\[Add Viipio to Obtainium\]\((https://apps\.obtainium\.imranr\.dev/redirect\?r=[^)]+)\)", README)
        assert match is not None, 'README needs a clickable Obtainium shortcut'
        deep_link = parse_qs(urlsplit(match.group(1)).query)['r'][0]
        self.assertTrue(deep_link.startswith('obtainium://app/'))
        config = json.loads(unquote(deep_link.removeprefix('obtainium://app/')))
        self.assertEqual(config, {
            'id': 'io.github.tehisaru.viipio',
            'url': 'https://github.com/tehisaru-git/viipio-keyboard',
            'author': 'tehisaru-git',
            'name': 'Viipio keyboard',
        })
        qr = ROOT / 'docs/assets/viipio-obtainium-qr.png'
        self.assertIn(f'<a href="{match.group(1)}"><img', README)
        self.assertIn('src="docs/assets/viipio-obtainium-qr.png"', README)
        self.assertIn('alt="Scan to add Viipio in Obtainium"', README)
        self.assertTrue(qr.is_file())
        self.assertEqual(qr.read_bytes()[:8], b'\x89PNG\r\n\x1a\n')

    def test_obtainium_first_install_links_to_latest_arm64_apk(self):
        direct = ('[Download the latest Obtainium APK (ARM64)]('
                  'https://github.com/ImranR98/Obtainium/releases/latest/download/'
                  'app-arm64-v8a-release.apk)')
        self.assertIn(direct, README)
        self.assertLess(README.index(direct), README.index('[Add Viipio to Obtainium]'))
        self.assertIn('[Obtainium release page](https://github.com/ImranR98/Obtainium/releases/latest)', README)
        self.assertIn('Open the downloaded APK', README)


    def test_operational_install_scripts_target_new_app(self):
        termux = (ROOT / 'build-on-termux.sh').read_text()
        ci = (ROOT / '.github/scripts/emulator-ci.sh').read_text()
        self.assertIn('PKG_BASE="io.github.tehisaru.viipio"', termux)
        self.assertIn("Viipio keyboard (Debug)", termux)
        self.assertIn('PKG=io.github.tehisaru.viipio.debug', ci)
        self.assertIn('tribixbite.cleverkeys.ViipioInputMethodService', ci)

    def test_release_signer_uses_separate_viipio_key(self):
        script = (ROOT / 'scripts/sign-viipio-release.py').read_text()
        self.assertIn('.config/viipio-keyboard', script)
        self.assertIn("'RELEASE_KEY_ALIAS': 'viipio'", script)
        self.assertNotIn('estonian-swipe', script)
        self.assertIn('scripts/gradle-guard.sh', script)

    def test_export_filename_is_language_neutral(self):
        activity = (ROOT / 'src/main/kotlin/tribixbite/cleverkeys/estoniantelemetry/TelemetryActivity.kt').read_text()
        self.assertIn('putExtra(Intent.EXTRA_TITLE, "viipio-diagnostics.jsonl")', activity)
        self.assertNotIn('estonian-swipe-diagnostics.jsonl', activity)

if __name__ == "__main__":
    unittest.main()
