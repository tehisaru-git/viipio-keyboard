"""Offline, synthetic fixtures: no corpus text is checked into the repository."""
import bz2
import hashlib
import importlib.util
import json
import io
from pathlib import Path
import struct
import sys
import tarfile
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location("build_et_lm", ROOT / "scripts/estonian/build_et_lm.py")
assert SPEC and SPEC.loader
et = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = et
SPEC.loader.exec_module(et)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def corpus_fixture(directory):
    leipzig = directory / "news.tar.gz"
    payload = (b"1\tSee on tore.\n2\tKass on tore.\n3\tSee on hea.\n"
               b"4\tSee on tore kass.\n5\tSee on tore hea.\n6\tSee on tore veel.\n")
    with tarfile.open(leipzig, "w:gz") as tar:
        info = tarfile.TarInfo("est_news_2022_100K-sentences.txt")
        info.size = len(payload)
        tar.addfile(info, io.BytesIO(payload))
    tatoeba = directory / "est.tsv.bz2"
    with bz2.open(tatoeba, "wb") as out:
        out.write(b"1\test\tSee on tore.\twriter\n2\test\tUus lause on kena.\t\\N\n")
    return leipzig, tatoeba


class EtBuilderTest(unittest.TestCase):
    def test_bundled_et_model_has_tracked_drift_sidecar(self):
        asset = ROOT / "src/main/assets/lm/et.cklm"
        meta = ROOT / "src/main/assets/lm/et.json"
        info = json.loads(meta.read_text(encoding="utf-8"))
        self.assertEqual(info["sha256"], digest(asset))
        self.assertEqual(info["bytes"], asset.stat().st_size)
        self.assertEqual(info["model"]["lexicon"], "et_enhanced.bin")
        self.assertEqual(info["model"]["contractionFiles"], [])
        self.assertEqual(info["heldout"]["modelTop3Hits"], 14508)
        self.assertEqual(info["corpus"]["weights"], {"leipzig": 1.0, "tatoeba": 1.0})
        self.assertEqual(info["tatoebaContributors"], 78)
        self.assertEqual({s["key"] for s in info["sources"]}, {"leipzig", "tatoeba"})
        self.assertTrue(all(s["pinned"] for s in info["sources"]))
        names = (ROOT / "scripts/data/tatoeba-contributors-et.txt").read_text().splitlines()
        self.assertEqual(len([n for n in names if n and not n.startswith("#")]), 78)

    def test_fixed_pins_and_reject_mismatched_archive(self):
        self.assertEqual(et.LEIPZIG_SHA256, "f07d968d0875b7dc55400b9850974e70a07c2372bbb924643d887944f7ac2096")
        self.assertEqual(et.TATOEBA_SHA256, "e09dfc5f9fd74b7b3385a81ec5cd0a0b6e5ec96aa5a979664b1a763971beeb08")
        with tempfile.TemporaryDirectory() as tmp:
            a, _ = corpus_fixture(Path(tmp))
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                et.verify_archive(a, et.LEIPZIG_SHA256)
            self.assertEqual(et.verify_archive(a, digest(a)), digest(a))

    def test_heldout_metric_uses_same_vocabulary_and_counts_missing_context(self):
        from collections import Counter
        from build_static_lm import Model
        model = Model(table={"see": [("on", 8)], "on": [("tore", 7)]},
                      successors={"see": 8, "on": 7},
                      unigrams={"on": 10, "hea": 9, "tore": 8, "see": 5}, total_tokens=32)
        stats = et.evaluate(model, [["see", "on", "tore"], ["puudu", "hea"]],
                            {"see", "on", "tore", "hea"})
        self.assertEqual(stats["eligible"], 3)
        self.assertEqual(stats["contextCovered"], 2)
        self.assertEqual(stats["modelTop3Hits"], 2)
        self.assertEqual(stats["unigramTop3Hits"], 3)

    def test_candidate_is_deterministic_and_has_valid_et_header_and_word_limit(self):
        with tempfile.TemporaryDirectory() as tmp:
            a, b = corpus_fixture(Path(tmp))
            vocab = {"see", "on", "tore", "hea", "kass"}
            first = et.build_candidate(a, b, digest(a), digest(b), vocab)
            second = et.build_candidate(a, b, digest(a), digest(b), set(reversed(sorted(vocab))))
            self.assertEqual(first.blob, second.blob)
            self.assertEqual(first.metrics, second.metrics)
            magic, version, flags, lang, words, prevs, pairs, idx, cont = struct.unpack_from("<4sHH4sIIIII", first.blob)
            self.assertEqual((magic, version, flags, lang), (b"CKLM", 1, 0, b"et\0\0"))
            self.assertEqual(cont, idx + prevs * 10)
            self.assertGreaterEqual(len(first.blob), cont)
            self.assertEqual(first.metrics["vocabWordLimit"], len(vocab))
            self.assertLessEqual(words, len(vocab))
            self.assertEqual(et.decode_words(first.blob), set(first.model_words))
            self.assertLessEqual(max(map(len, et.decode_words(first.blob))), et.MAX_WORD_LENGTH)
            self.assertEqual(first.metrics["duplicateSentences"]["tatoeba"], 1)
            self.assertEqual(first.metrics["trainSentences"]["tatoeba"], 0)
            self.assertEqual(first.metrics["anonymousTatoebaRowsExcluded"], 1)

    def test_gate_failure_does_not_publish_asset(self):
        with tempfile.TemporaryDirectory() as tmp:
            a, b = corpus_fixture(Path(tmp))
            candidate = et.build_candidate(a, b, digest(a), digest(b), {"see", "on", "tore", "hea", "kass"})
            out = Path(tmp) / "et.cklm"
            self.assertFalse(et.publish_if_passes(candidate, out, Path(tmp) / "NOTICE.txt"))
            self.assertFalse(out.exists())
            self.assertFalse((Path(tmp) / "NOTICE.txt").exists())


if __name__ == "__main__":
    unittest.main()
