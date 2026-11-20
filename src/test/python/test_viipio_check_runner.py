"""Guard the fast Viipio release checks against accidental omissions."""
from pathlib import Path
from contextlib import redirect_stderr, redirect_stdout
import importlib.util
import inspect
from io import StringIO
import os
import sys
import tempfile
import time
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location("viipio_check", ROOT / "scripts/viipio-check.py")
assert SPEC is not None and SPEC.loader is not None
CHECK = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHECK)


class ViipioCheckRunnerTest(unittest.TestCase):
    def test_quick_checks_staged_whitespace_before_python_tests(self):
        commands = []
        with patch.object(CHECK, "run", side_effect=lambda *args: commands.append(args)):
            with patch.object(sys, "argv", ["viipio-check.py", "quick"]):
                CHECK.main()
        self.assertEqual(("git", "diff", "--check"), commands[0])
        self.assertEqual(("git", "diff", "--cached", "--check"), commands[1])
        self.assertEqual("-m", commands[2][1])

    def test_targeted_includes_release_metadata_personal_words_and_frequency_merge(self):
        commands = []
        expected = []
        with patch.object(CHECK, "run", side_effect=lambda *args: commands.append(args)):
            with patch.object(CHECK, "report", side_effect=lambda suites, run_started_ns: expected.extend(suites)):
                with patch.object(sys, "argv", ["viipio-check.py", "targeted"]):
                    CHECK.main()
        for suite in (
            "tribixbite.cleverkeys.ReleaseMetadataDriftTest",
            "tribixbite.cleverkeys.ViipioPersonalWordsTest",
            "tribixbite.cleverkeys.swipe.ctc.CtcLexiconMergeTest",
        ):
            self.assertIn(suite, expected)
            self.assertIn(suite, commands[-1])

    def test_explicit_rerun_forces_gradle_after_up_to_date_result(self):
        commands = []
        with patch.object(CHECK, "run", side_effect=lambda *args: commands.append(args)):
            with patch.object(CHECK, "report"):
                with patch.object(sys, "argv", ["viipio-check.py", "targeted", "--rerun"]):
                    with redirect_stderr(StringIO()):
                        try:
                            CHECK.main()
                        except SystemExit as exc:
                            self.fail(f"--rerun was rejected by the runner: {exc}")
        self.assertIn("--rerun-tasks", commands[-1])

    def test_report_rejects_zero_tests_in_an_expected_suite(self):
        self.assertIn("run_started_ns", inspect.signature(CHECK.report).parameters)
        with tempfile.TemporaryDirectory() as tmp:
            (Path(tmp) / "TEST-empty.xml").write_text(
                '<testsuite name="pkg.Empty" tests="0" failures="0" errors="0" skipped="0"/>')
            with patch.object(CHECK, "REPORTS", Path(tmp)):
                with redirect_stdout(StringIO()), redirect_stderr(StringIO()):
                    with self.assertRaises(SystemExit):
                        CHECK.report(("pkg.Empty",), run_started_ns=0)

    def test_report_rejects_xml_older_than_this_run(self):
        self.assertIn("run_started_ns", inspect.signature(CHECK.report).parameters)
        with tempfile.TemporaryDirectory() as tmp:
            report = Path(tmp) / "TEST-stale.xml"
            report.write_text(
                '<testsuite name="pkg.Stale" tests="1" failures="0" errors="0" skipped="0"/>')
            os.utime(report, ns=(1_000_000_000, 1_000_000_000))
            with patch.object(CHECK, "REPORTS", Path(tmp)):
                with redirect_stdout(StringIO()), redirect_stderr(StringIO()):
                    with self.assertRaises(SystemExit):
                        CHECK.report(("pkg.Stale",), run_started_ns=time.time_ns())


if __name__ == "__main__":
    unittest.main()
