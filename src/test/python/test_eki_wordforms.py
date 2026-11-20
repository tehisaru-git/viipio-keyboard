"""Regression checks for the CC-BY EKI word-form reference list builder."""
import importlib.util
import io
import sqlite3
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[3] / "scripts" / "build_eki_wordforms.py"


class EkiWordformsTest(unittest.TestCase):
    def test_aggregates_forms_across_lemmas_without_names_or_punctuation(self):
        spec = importlib.util.spec_from_file_location("build_eki_wordforms", SCRIPT)
        assert spec is not None and spec.loader is not None
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        sample = io.StringIO(
            "### oli (100)\n\tV\ttalvel\t12\n\tV\tTALVEL\t90\n"
            "### talv (20)\n\tS.sg.adv\ttalvel\t7\n"
            "\tS.sg.n\tkass\t11\n\tS.sg.n\t123\t800\n"
            "\tS.sg.n\tnaïve\t50\n\tS.sg.n\tx\t300\n"
        )
        with sqlite3.connect(":memory:") as db:
            self.assertEqual(module.top_forms(sample, db, limit=2),
                             [("talvel", 19), ("kass", 11)])


if __name__ == "__main__":
    unittest.main()
