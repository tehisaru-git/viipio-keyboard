"""Guard against clean Git checkouts silently dropping Compose generator inputs."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
COMPOSE = ROOT / "srcs" / "compose"


class ComposeSourceReproducibilityTest(unittest.TestCase):
    def test_every_compose_json_input_is_tracked(self):
        present = {p.relative_to(ROOT).as_posix() for p in COMPOSE.rglob("*.json")}
        self.assertGreaterEqual(len(present), 37)
        self.assertTrue({"srcs/compose/fn.json", "srcs/compose/shift.json",
                         "srcs/compose/numpad_bengali.json",
                         "srcs/compose/compose/extra.json",
                         "srcs/compose/compose/cyrillic.json",
                         "srcs/compose/compose/arabic.json"} <= present)
        tracked = set(subprocess.check_output(
            ["git", "ls-files", "--cached", "--", "srcs/compose"],
            cwd=ROOT, text=True).splitlines())
        tracked = {path for path in tracked if path.endswith(".json")}
        self.assertEqual(present, tracked,
                         "Compose JSON inputs must survive a clean Git checkout")

    def test_generator_reproduces_tracked_entry_points_from_sources(self):
        inputs = sorted(COMPOSE.glob("*.json")) + [COMPOSE / "compose"]
        with tempfile.TemporaryDirectory(prefix="viipio-compose-") as directory:
            output = Path(directory)
            script = output / "scripts/generate_compose_bin.py"
            script.parent.mkdir(parents=True)
            shutil.copy2(ROOT / "scripts/generate_compose_bin.py", script)
            generated_kt = output / "src/main/kotlin/tribixbite/cleverkeys/ComposeKeyData.kt"
            generated_kt.parent.mkdir(parents=True)
            generated_bin = output / "src/main/assets/compose_data.bin"
            generated_bin.parent.mkdir(parents=True)
            subprocess.run([sys.executable, str(script), *(str(p) for p in inputs)],
                           cwd=ROOT, check=True, capture_output=True, text=True)
            tracked_kt = ROOT / "src/main/kotlin/tribixbite/cleverkeys/ComposeKeyData.kt"
            self.assertEqual(tracked_kt.read_bytes(), generated_kt.read_bytes())
            self.assertGreater(generated_bin.stat().st_size, 50_000)

    def test_runtime_compose_entry_points_are_generated(self):
        source = (ROOT / "src/main/kotlin/tribixbite/cleverkeys/ComposeKeyData.kt").read_text()
        for name in ("fn", "shift", "NUMPAD_BENGALI", "NUMPAD_TAMIL"):
            with self.subTest(name=name):
                self.assertIn(f"const val {name} =", source)


if __name__ == "__main__":
    unittest.main()
