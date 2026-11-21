"""Public ET word probes only; no user traces or published ZIP mutation."""
import hashlib
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from audit_langpack import parse_ckdt
from remove_sis import (OLD_SHA256, canonical_source, decode_v2, encode_v2,
                        remove_exact_sis, build)

ROOT = Path(__file__).resolve().parents[3]
ASSET = ROOT / 'src/main/assets/dictionaries/et_enhanced.bin'
MANIFEST = ROOT / 'src/main/assets/estonian/manifest.json'


class RemoveSisTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = canonical_source(ROOT)
        cls.old = decode_v2(cls.original)

    def test_source_is_pinned_and_noop_is_byte_identical(self):
        self.assertEqual(hashlib.sha256(self.original).hexdigest(), OLD_SHA256)
        self.assertEqual(encode_v2(self.old), self.original)
        self.assertEqual(len(self.old.canonical), 95000)
        self.assertEqual([rank for word, rank in self.old.canonical if word == 'sis'], [36])
        self.assertEqual([rank for word, rank in self.old.canonical if word == 'siis'], [6])

    def test_only_exact_sis_removed_and_all_lookup_indices_preserved(self):
        changed = remove_exact_sis(self.old)
        old_words = self.old.canonical
        removed_index = next(i for i, (word, _) in enumerate(old_words) if word == 'sis')
        self.assertEqual(changed.canonical, old_words[:removed_index] + old_words[removed_index + 1:])
        self.assertEqual(len(changed.canonical), 94999)
        self.assertIn(('siis', 6), changed.canonical)
        self.assertNotIn('sis', [w for w, _ in changed.canonical])
        self.assertNotIn('sis', changed.normalized)
        expected_normalized = tuple(w for w in self.old.normalized if w != 'sis')
        self.assertEqual(changed.normalized, expected_normalized)
        for word in expected_normalized:
            expected = tuple(i - (i > removed_index) for i in self.old.lookup[word] if i != removed_index)
            self.assertEqual(changed.lookup[word], expected, word)
            self.assertTrue(all(0 <= i < len(changed.canonical) for i in changed.lookup[word]))
        self.assertEqual(decode_v2(encode_v2(changed)), changed)
        self.assertEqual(parse_ckdt(encode_v2(changed)), list(changed.canonical))
        self.assertEqual(remove_exact_sis(changed), changed)

    def test_bundled_output_matches_deterministic_build_and_manifest(self):
        binary, manifest_bytes = build(self.original, MANIFEST.read_bytes())
        self.assertEqual(ASSET.read_bytes(), binary)
        self.assertEqual(MANIFEST.read_bytes(), manifest_bytes)
        manifest = json.loads(manifest_bytes)
        self.assertEqual(manifest['wordCount'], 94999)
        self.assertEqual(manifest['dictionarySha256'], 'e5b6bc41df85a636d6892e51e2e0ef45a1a79990a0e0c9c326244b76b64d51a1')
        self.assertEqual(hashlib.sha256(binary).hexdigest(), manifest['dictionarySha256'])
        self.assertEqual(manifest['code'], 'et')
        self.assertEqual(manifest['license'], 'CC-BY-4.0')
        self.assertIn('Leipzig Corpora Collection', manifest['attribution'])

    def test_wrong_source_checksum_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'source SHA-256'):
            build(self.original + b'X', MANIFEST.read_bytes())


if __name__ == '__main__':
    unittest.main()
