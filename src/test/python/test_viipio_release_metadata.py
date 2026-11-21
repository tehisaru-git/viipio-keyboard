"""Update compatibility and public artifact identity for the Viipio release."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
GRADLE = (ROOT / "build.gradle").read_text()
README = (ROOT / "README.md").read_text()


class ViipioReleaseMetadataTest(unittest.TestCase):
    def test_version_and_apk_name_are_branded(self):
        versions = dict(re.findall(r"ext\.(VERSION_MAJOR|VERSION_MINOR|VERSION_PATCH)\s*=\s*(\d+)", GRADLE))
        self.assertEqual(versions, {"VERSION_MAJOR": "0", "VERSION_MINOR": "9", "VERSION_PATCH": "1"})
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

    def test_public_links_and_previous_name_migration_are_documented(self):
        self.assertIn("https://github.com/tehisaru-git/viipio-keyboard", README)
        self.assertIn("v0.7.0 is an experimental public test release", README)
        self.assertNotIn("APK is not available", README)
        self.assertIn("ViipioKeyboard-v0.5.1-arm64-v8a.apk", README)
        self.assertIn("releases/tag/v0.5.1", README)
        self.assertIn("releases/tag/v0.6.0", README)
        self.assertIn("releases/tag/v0.7.0", README)
        self.assertIn("new Android app, not an in-place update", README)
        self.assertNotIn("local, unpublished candidate", README)
        self.assertIn("previously called **Estonian Swipe**", README)
        self.assertIn("io.github.tehisaru.viipio", README)


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
