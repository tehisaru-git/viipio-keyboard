"""Ensure all JSON assets bundled by this fork come from Git, not a dirty checkout."""
import json
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[3]
REQUIRED = (
    *(f"src/main/assets/bigrams/{lang}_bigrams.json" for lang in ("de", "en", "es", "fr", "it", "pt")),
    "src/main/assets/url_rules/clearurls.json",
    "src/main/assets/url_rules/embed_enrich.json",
)


class ViipioPackagedSourcesTest(unittest.TestCase):
    def test_bundled_json_assets_are_valid_and_tracked(self):
        tracked = set(subprocess.check_output(
            ["git", "ls-files", "--", "src/main/assets/bigrams", "src/main/assets/url_rules"],
            cwd=ROOT, text=True).splitlines())
        for name in REQUIRED:
            with self.subTest(asset=name):
                self.assertTrue((ROOT / name).is_file(), f"missing packaged asset {name}")
                with (ROOT / name).open(encoding="utf-8") as asset:
                    json.load(asset)
                self.assertIn(name, tracked, f"{name} is ignored; fresh checkout would omit it")


if __name__ == "__main__":
    unittest.main()
