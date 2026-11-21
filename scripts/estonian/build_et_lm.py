#!/usr/bin/env python3
"""Reproducible offline Estonian CKLM candidate; no Wikipedia or user text.

Usage: python3 scripts/estonian/build_et_lm.py --cache "$TMPDIR/et-lm-probe"
The two pinned archives must be present; use --fetch to retrieve missing ones.
Only a passing candidate is written to the app's asset directory. A candidate
is NOT enabled in the active IME by this script.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path
import struct
import sys
import tempfile
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from build_static_lm import (  # noqa: E402
    count_corpus, leipzig_lines, tatoeba_lines, ckdt_words, combine, prune,
    encode, tokenize, split_sentences, SIZE_CAP_BYTES,
)

REPO = Path(__file__).resolve().parents[2]
LEIPZIG_NAME = "est_news_2022_100K.tar.gz"
TATOEBA_NAME = "est_sentences_detailed.tsv.bz2"
LEIPZIG_URL = f"https://downloads.wortschatz-leipzig.de/corpora/{LEIPZIG_NAME}"
TATOEBA_URL = f"https://downloads.tatoeba.org/exports/per_language/est/{TATOEBA_NAME}"
LEIPZIG_SHA256 = "f07d968d0875b7dc55400b9850974e70a07c2372bbb924643d887944f7ac2096"
TATOEBA_SHA256 = "e09dfc5f9fd74b7b3385a81ec5cd0a0b6e5ec96aa5a979664b1a763971beeb08"
MAX_WORD_LENGTH = 64
TATOEBA_WEIGHT = 1.0  # fixed before heldout evaluation; never tuned on heldout
MIN_EVAL_COUNT = 1000


def verify_archive(path: Path, expected: str) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for block in iter(lambda: fh.read(1 << 20), b""):
            digest.update(block)
    actual = digest.hexdigest()
    if actual != expected:
        raise ValueError(f"{path}: SHA-256 {actual} != pinned {expected}; refusing unpinned input")
    return actual


def obtain(path: Path, url: str, sha: str, fetch: bool) -> None:
    if not path.exists():
        if not fetch:
            raise FileNotFoundError(f"{path} missing; put the pinned archive there or pass --fetch")
        path.parent.mkdir(parents=True, exist_ok=True)
        temp = path.with_suffix(path.suffix + ".part")
        try:
            with urllib.request.urlopen(url, timeout=120) as source, temp.open("wb") as out:
                for block in iter(lambda: source.read(1 << 20), b""):
                    out.write(block)
            verify_archive(temp, sha)
            temp.replace(path)
        finally:
            temp.unlink(missing_ok=True)
    verify_archive(path, sha)


def decode_words(blob: bytes) -> set[str]:
    """Independent front-code readback: validates header, offsets and word length."""
    if len(blob) < 32:
        raise ValueError("truncated CKLM")
    magic, version, flags, lang, count, prevs, _pairs, idx, cont = struct.unpack_from("<4sHH4sIIIII", blob)
    if (magic, version, flags, lang) != (b"CKLM", 1, 0, b"et\0\0") or cont != idx + prevs * 10 or cont > len(blob):
        raise ValueError("invalid ET CKLM header")
    pos, old, words = 32, b"", set()
    for _ in range(count):
        common, suffix = struct.unpack_from("<BB", blob, pos)
        pos += 2
        word = old[:common] + blob[pos:pos + suffix]
        pos += suffix + 1  # unigram quantised byte
        if common > len(old) or pos > idx:
            raise ValueError("invalid ET CKLM vocabulary")
        decoded = word.decode("utf-8")
        if len(decoded) > MAX_WORD_LENGTH:
            raise ValueError("ET CKLM word too long")
        words.add(decoded)
        old = word
    if pos != idx or len(words) != count:
        raise ValueError("invalid ET CKLM vocabulary boundary")
    return words


def evaluate(model, heldout: list[list[str | None]], vocab: set[str]) -> dict[str, int]:
    """All eligible in-vocab next words, including unknown/missing prev rows.

    Both baselines offer precisely the encoded candidate vocabulary. The unigram
    predicts the same three global words at each opportunity; ties lexical UTF-8.
    """
    baseline = {word for word in sorted(vocab, key=lambda w: (-model.unigrams.get(w, 0), w.encode("utf-8")))[:3]}
    n = contexts = hits = base = all_targets = 0
    for sentence in heldout:
        prev = None
        for word in sentence:
            if word is None:
                prev = None
                continue
            if prev is not None:
                all_targets += 1
                if word in vocab:
                    n += 1
                    row = model.table.get(prev, [])
                    contexts += bool(row)
                    hits += any(target == word for target, _ in row[:3])
                    base += word in baseline
            prev = word
    return {"eligible": n, "allNextWords": all_targets, "contextCovered": contexts,
            "modelTop3Hits": hits, "unigramTop3Hits": base}


@dataclass
class Candidate:
    blob: bytes
    metrics: dict
    contributors: set[str]
    model_words: set[str]


def build_candidate(leipzig_path: Path, tatoeba_path: Path, leipzig_pin: str,
                    tatoeba_pin: str, vocabulary: set[str]) -> Candidate:
    verify_archive(leipzig_path, leipzig_pin)
    verify_archive(tatoeba_path, tatoeba_pin)
    vocab = {w for w in vocabulary if 0 < len(w) <= MAX_WORD_LENGTH}
    seen: set[str] = set()
    heldout: list[list[str | None]] = []

    def collect(seg: str) -> None:
        # count_corpus calls the sink for heldout only, without contributing counts.
        heldout.extend(tokenize(part) for part in split_sentences(seg, nfc=True))

    news, _ = count_corpus("leipzig", leipzig_lines(leipzig_path), vocab, seen, collect, nfc=True)
    anonymous = 0

    def attributed_tatoeba():
        nonlocal anonymous
        for sentence, author in tatoeba_lines(tatoeba_path):
            if not author:
                # No author is recoverable from this export row: do not count it
                # in a CC BY derived asset without attribution.
                anonymous += 1
                continue
            yield sentence, author

    tatoeba, _ = count_corpus("tatoeba", attributed_tatoeba(), vocab, seen, collect, nfc=True)
    # Deliberately NO artefact filter: wordfreq has no ET frequencies (LookupError),
    # while cross-corpus ratios wrongly delete ordinary ET words in small Tatoeba.
    # Keep chain-break handling for digits and exclude duplicate/heldout sentences.
    model = prune(*combine([(news, 1.0), (tatoeba, TATOEBA_WEIGHT)]))
    blob, stats = encode(model, "et")
    model_words = decode_words(blob)
    if not model_words <= vocab:
        raise ValueError("encoded word outside shipped ET dictionary")
    score = evaluate(model, heldout, model_words)
    metrics = {
        "archiveSha256": {"leipzig": leipzig_pin, "tatoeba": tatoeba_pin},
        "vocabWordLimit": len(vocab), "maxWordLength": MAX_WORD_LENGTH,
        "trainSentences": {"leipzig": news.sentences, "tatoeba": tatoeba.sentences},
        "heldoutSentences": {"leipzig": news.heldout, "tatoeba": tatoeba.heldout},
        "duplicateSentences": {"leipzig": news.duplicates, "tatoeba": tatoeba.duplicates},
        "anonymousTatoebaRowsExcluded": anonymous,
        "trainTokens": {"leipzig": news.tokens, "tatoeba": tatoeba.tokens},
        "model": stats, "bytes": len(blob), "sha256": hashlib.sha256(blob).hexdigest(),
        "tatoebaWeight": TATOEBA_WEIGHT, "artefactFilter": "disabled; no trustworthy ET frequency reference",
        "heldout": score,
        "gate": {"minEligible": MIN_EVAL_COUNT, "maxBytes": SIZE_CAP_BYTES,
                 "mustBeatUnigramTop3": True},
    }
    return Candidate(blob, metrics, tatoeba.contributors, model_words)


def passes(candidate: Candidate) -> bool:
    s = candidate.metrics["heldout"]
    return (s["eligible"] >= MIN_EVAL_COUNT and
            s["modelTop3Hits"] > s["unigramTop3Hits"] and
            candidate.metrics["bytes"] <= SIZE_CAP_BYTES)


def publish_if_passes(candidate: Candidate, asset: Path, notice: Path,
                      sidecar: Path | None = None, contributors: Path | None = None) -> bool:
    if not passes(candidate):
        return False
    asset.parent.mkdir(parents=True, exist_ok=True)
    notice.parent.mkdir(parents=True, exist_ok=True)
    # Tatoeba CC BY attribution: all contributors whose TRAIN sentences were counted.
    names = sorted(candidate.contributors, key=lambda x: (x.casefold(), x))
    notice.write_text(
        "Estonian static LM (et.cklm): derived from Leipzig Corpora Collection, "
        "est_news_2022_100K, CC BY (the Leipzig download terms do not specify a version).\n"
        "© Universität Leipzig / Sächsische Akademie der Wissenschaften / InfAI.\n"
        f"{LEIPZIG_URL}\nhttps://wortschatz-leipzig.de/en/usage\n"
        "And from Tatoeba Estonian sentences, CC BY 2.0 FR.\n"
        f"{TATOEBA_URL}\nhttps://tatoeba.org/en/downloads\n"
        "Transformation: deduplicated and tokenized sentences; hashed 10% heldout; "
        "weighted bigram counts and quantized/pruned CKLM v1; no source text shipped.\n"
        "Tatoeba contributors whose sentences contributed to the model:\n"
        + "\n".join(names) + "\n", encoding="utf-8")
    asset.write_bytes(candidate.blob)
    if contributors is not None:
        contributors.parent.mkdir(parents=True, exist_ok=True)
        contributors.write_text("\n".join(names) + "\n", encoding="utf-8")
    if sidecar is not None:
        sidecar.parent.mkdir(parents=True, exist_ok=True)
        stats = candidate.metrics["model"]
        sidecar.write_text(json.dumps({
            "format": "CKLM", "version": 1, "language": "et",
            "vocab": stats["vocab"], "prevs": stats["prevs"], "pairs": stats["pairs"],
            "bytes": candidate.metrics["bytes"], "sha256": candidate.metrics["sha256"],
            "model": {"lexicon": "et_enhanced.bin", "contractionFiles": [],
                      "order": 2, "minCount": 3, "topK": 20},
            "corpus": {"weights": {"leipzig": 1.0, "tatoeba": TATOEBA_WEIGHT}},
            "tatoebaContributors": len(names),
            "sources": [
                {"key": "leipzig", "url": LEIPZIG_URL, "sha256": LEIPZIG_SHA256,
                 "pinned": True, "license": "CC BY (download terms; version unspecified)",
                 "shipped": True},
                {"key": "tatoeba", "url": TATOEBA_URL, "sha256": TATOEBA_SHA256,
                 "pinned": True, "license": "CC BY 2.0 FR", "shipped": True},
            ],
            "heldout": candidate.metrics["heldout"],
            "attribution": "estonian/ET_CKLM_NOTICE.txt",
        }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return True


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--cache", type=Path, default=Path(tempfile.gettempdir()) / "et-lm-probe")
    p.add_argument("--fetch", action="store_true", help="download missing archives, reject changed rotating exports")
    p.add_argument("--output", type=Path, default=REPO / "src/main/assets/lm/et.cklm")
    p.add_argument("--notice", type=Path, default=REPO / "src/main/assets/estonian/ET_CKLM_NOTICE.txt")
    p.add_argument("--sidecar", type=Path, help="default: <output basename>.json")
    p.add_argument("--contributors", type=Path, default=REPO / "scripts/data/tatoeba-contributors-et.txt")
    a = p.parse_args()
    news, tatoeba = a.cache / LEIPZIG_NAME, a.cache / TATOEBA_NAME
    obtain(news, LEIPZIG_URL, LEIPZIG_SHA256, a.fetch)
    obtain(tatoeba, TATOEBA_URL, TATOEBA_SHA256, a.fetch)
    vocab = set(ckdt_words(REPO / "src/main/assets/dictionaries/et_enhanced.bin"))
    candidate = build_candidate(news, tatoeba, LEIPZIG_SHA256, TATOEBA_SHA256, vocab)
    ok = passes(candidate)
    print(json.dumps({**candidate.metrics, "gatePassed": ok}, ensure_ascii=False, indent=2))
    if ok:
        publish_if_passes(candidate, a.output, a.notice,
                          a.sidecar or a.output.with_suffix(".json"), a.contributors)
        print(f"Published {a.output} and {a.notice}")
    else:
        # An old candidate must not survive a failed gate for these inputs.
        a.output.unlink(missing_ok=True)
        a.notice.unlink(missing_ok=True)
        (a.sidecar or a.output.with_suffix(".json")).unlink(missing_ok=True)
        a.contributors.unlink(missing_ok=True)
        print("Gate failed: candidate not published")
    return 0


if __name__ == "__main__":
    sys.exit(main())
