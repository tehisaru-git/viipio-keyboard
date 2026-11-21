#!/usr/bin/env python3
"""Rebuild the bundled ET CKDT V2 from pinned Git bytes, removing only exact 'sis'.

Run from any directory: python3 scripts/estonian/remove_sis.py
No published ZIP, EN dictionary, corpus, rank quantization, or user traces involved.
"""
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
import struct
import subprocess

ROOT = Path(__file__).resolve().parents[2]
SOURCE_COMMIT = '4c8d3b9a22cb806566aeed973e8325e5621ce028'
ASSET_REL = 'src/main/assets/dictionaries/et_enhanced.bin'
MANIFEST_REL = 'src/main/assets/estonian/manifest.json'
OLD_SHA256 = '9b4e79ad7ebefb59d7a23b3d719fcc62d8d8b33afa5d2638461e8a7fdfe9abe2'


@dataclass(eq=True)
class V2:
    canonical: tuple[tuple[str, int], ...]
    normalized: tuple[str, ...]
    lookup: dict[str, tuple[int, ...]]
    header_suffix: bytes
    lang: bytes


def _sha(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def canonical_source(root: Path = ROOT) -> bytes:
    """Read the immutable pre-edit Git blob; never use a possibly regenerated ZIP."""
    data = subprocess.check_output(['git', '-C', str(root), 'show',
                                    f'{SOURCE_COMMIT}:{ASSET_REL}'])
    if _sha(data) != OLD_SHA256:
        raise ValueError('source SHA-256 mismatch')
    return data


def decode_v2(data: bytes) -> V2:
    if not 48 <= len(data) <= 16 * 1024 * 1024:
        raise ValueError('CKDT size outside bounds')
    magic, version, lang, count, start, norm, accent = struct.unpack_from('<4sI4sIIII', data)
    if (magic, version, lang, start) != (b'CKDT', 2, b'et\0\0', 48):
        raise ValueError('expected ET CKDT V2')
    if count > 100_000 or not 48 <= norm <= accent <= len(data):
        raise ValueError('invalid section offsets or count')

    def u16(pos: int, end: int) -> tuple[int, int]:
        if pos + 2 > end:
            raise ValueError('truncated CKDT uint16')
        return struct.unpack_from('<H', data, pos)[0], pos + 2

    def word_at(pos: int, end: int) -> tuple[str, int]:
        n, pos = u16(pos, end)
        if not n or pos + n > end:
            raise ValueError('truncated CKDT word')
        return data[pos:pos + n].decode('utf-8'), pos + n

    pos = start
    canonical = []
    for _ in range(count):
        word, pos = word_at(pos, norm)
        if pos >= norm:
            raise ValueError('missing canonical rank')
        canonical.append((word, data[pos]))
        pos += 1
    if pos != norm or len({w for w, _ in canonical}) != count:
        raise ValueError('invalid canonical section')
    pos = norm
    if pos + 4 > accent:
        raise ValueError('missing normalized count')
    normalized_count = struct.unpack_from('<I', data, pos)[0]
    pos += 4
    if normalized_count > count:
        raise ValueError('invalid normalized count')
    normalized = []
    for _ in range(normalized_count):
        word, pos = word_at(pos, accent)
        normalized.append(word)
    if pos != accent or len(set(normalized)) != normalized_count:
        raise ValueError('invalid normalized section')
    pos = accent
    lookup = {}
    for word in normalized:
        if pos >= len(data):
            raise ValueError('truncated lookup count')
        n = data[pos]
        pos += 1
        if not n or pos + 4 * n > len(data):
            raise ValueError('invalid lookup indices')
        indices = struct.unpack_from('<' + 'I' * n, data, pos)
        if any(i >= count for i in indices):
            raise ValueError('lookup index outside canonical section')
        lookup[word] = indices
        pos += 4 * n
    if pos != len(data):
        raise ValueError('trailing CKDT bytes')
    return V2(tuple(canonical), tuple(normalized), lookup, data[28:48], lang)


def encode_v2(v: V2) -> bytes:
    canonical = bytearray()
    for word, rank in v.canonical:
        encoded = word.encode('utf-8')
        canonical += struct.pack('<H', len(encoded)) + encoded + struct.pack('<B', rank)
    normalized = bytearray(struct.pack('<I', len(v.normalized)))
    lookup = bytearray()
    for word in v.normalized:
        encoded = word.encode('utf-8')
        normalized += struct.pack('<H', len(encoded)) + encoded
        indices = v.lookup[word]
        lookup += struct.pack('<B', len(indices))
        for index in indices:
            lookup += struct.pack('<I', index)
    norm_offset = 48 + len(canonical)
    accent_offset = norm_offset + len(normalized)
    header = struct.pack('<4sI4sIIII', b'CKDT', 2, v.lang, len(v.canonical),
                         48, norm_offset, accent_offset) + v.header_suffix
    return bytes(header + canonical + normalized + lookup)


def remove_exact_sis(v: V2) -> V2:
    indices = [i for i, (word, _) in enumerate(v.canonical) if word == 'sis']
    if not indices:
        return v  # idempotent only when already removed
    if len(indices) != 1:
        raise ValueError('expected one exact sis')
    removed = indices[0]
    updated = {}
    for word in v.normalized:
        mapped = tuple(i - (i > removed) for i in v.lookup[word] if i != removed)
        if mapped:
            updated[word] = mapped
    return V2(v.canonical[:removed] + v.canonical[removed + 1:],
              tuple(w for w in v.normalized if w in updated),
              updated, v.header_suffix, v.lang)


def build(source: bytes, manifest_bytes: bytes) -> tuple[bytes, bytes]:
    if _sha(source) != OLD_SHA256:
        raise ValueError('source SHA-256 mismatch')
    original = decode_v2(source)
    if encode_v2(original) != source:
        raise ValueError('source no-op CKDT V2 roundtrip mismatch')
    if len(original.canonical) != 95000 or sum(w == 'sis' for w, _ in original.canonical) != 1:
        raise ValueError('unexpected canonical source vocabulary')
    if 'sis' not in original.normalized:
        raise ValueError('missing normalized sis lookup')
    changed = remove_exact_sis(original)
    if 'sis' in changed.normalized or any(i >= len(changed.canonical) for ids in changed.lookup.values() for i in ids):
        raise ValueError('invalid remapped lookup')
    binary = encode_v2(changed)
    if decode_v2(binary) != changed:
        raise ValueError('output CKDT V2 roundtrip mismatch')
    manifest = json.loads(manifest_bytes)
    if manifest.get('code') != 'et' or manifest.get('wordCount') not in (95000, 94999):
        raise ValueError('unexpected ET manifest')
    manifest['wordCount'] = len(changed.canonical)
    manifest['dictionarySha256'] = _sha(binary)
    return binary, (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode('utf-8')


def main() -> None:
    source = canonical_source()
    asset = ROOT / ASSET_REL
    manifest = ROOT / MANIFEST_REL
    binary, meta = build(source, manifest.read_bytes())
    if asset.read_bytes() not in (source, binary):
        raise ValueError('bundled ET asset drift; refusing overwrite')
    asset.write_bytes(binary)
    manifest.write_bytes(meta)
    print(f'ET CKDT V2: 95000 -> 94999; sha256={_sha(binary)}; exact sis removed, siis retained')


if __name__ == '__main__':
    main()
