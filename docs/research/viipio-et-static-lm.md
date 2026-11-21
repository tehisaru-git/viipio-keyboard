# Estonian offline static LM (typed suggestions only)

## Inputs and rights

- Leipzig Corpora Collection `est_news_2022_100K.tar.gz`, Estonian **news 2022**, `https://downloads.wortschatz-leipzig.de/corpora/est_news_2022_100K.tar.gz`, SHA-256 `f07d968d0875b7dc55400b9850974e70a07c2372bbb924643d887944f7ac2096`. Leipzig's download-specific terms state **CC BY** (as archived 2023-10-31); unlike the separately restricted website query data, download corpora are not described as CC BY-NC. Those terms do not identify a BY version, so this notice does not infer one.[1]
- Tatoeba `est_sentences_detailed.tsv.bz2`, `https://downloads.tatoeba.org/exports/per_language/est/est_sentences_detailed.tsv.bz2`, SHA-256 `e09dfc5f9fd74b7b3385a81ec5cd0a0b6e5ec96aa5a979664b1a763971beeb08`. Tatoeba marks its sentence exports CC BY 2.0 FR (some individual sentences CC0), and its terms require author attribution; the generated `src/main/assets/estonian/ET_CKLM_NOTICE.txt` lists all **78 contributors of counted training sentences** and links the source and rights.[2][3]
- Both compressed source files remain in `$TMPDIR/et-lm-probe`, never Git. The builder verifies both **whole compressed archive** digests before processing and rejects a changed rotating Tatoeba export. The 394 Tatoeba rows without a recoverable author are excluded *before* either train or heldout selection, rather than using CC BY text without attributable authors. No Wikipedia archive, training text, or private/user input enters this asset. The lexicon is the app's existing `et_enhanced.bin` canonical dictionary (94,999 words). This asset contains only aggregated counts in quantized form.

## Predeclared evaluation and build

Run `python3 -m unittest scripts.estonian.tests.test_build_et_lm -v` then `python3 scripts/estonian/build_et_lm.py --cache "$TMPDIR/et-lm-probe"`. The script is Python stdlib only. Missing archives can be requested with `--fetch`, but the hash must still match; a rotating export usually will not. Rebuild from the pinned local snapshot instead. Fixed Tatoeba weight **1.0**, `MIN_COUNT=3`, top **20** pairs per context, ≤**512 KiB** asset; no heldout tuning. Unicode NFC and same sentence/token rules as the existing CKLM builder; digit tokens break pairs. A sentence-normalized SHA-1 partition reserves ~10% heldout *before* counts; both corpora deduplicated across archives (news first). Only dictionary words of at most 64 Unicode characters may be named. Decoding the generated front-coded vocabulary validates the `CKLM` v1 header, language `et`, offsets and word bounds.

The predeclared asset gate: **at least 1,000 eligible heldout next-word positions**, model top-3 **strictly greater** than unigram top-3 on precisely the same **encoded CKLM vocabulary**, and model **≤524,288 bytes**. All in-vocabulary successor positions count, including missing/unknown contexts (a missing row gets no LM hit). The unigram always predicts its three most frequent training words in that same vocabulary. This is a next-word proxy, not a swipe replay or IME integration.

| Heldout result (pinned inputs, current 94,999-word dictionary) | Value |
|---|---:|
| Train sentences, Leipzig / Tatoeba | 92,099 / 5,490 |
| Heldout sentences, Leipzig / Tatoeba | 10,439 / 607 |
| Duplicate sentences skipped, Leipzig / Tatoeba | 127 / 32 |
| Unattributed Tatoeba rows excluded | 394 |
| All heldout next-word positions / target in encoded vocabulary | 131,268 / 91,211 (69.48%) |
| Contexts with a pruned pair row, among eligible | 65,829 / 91,211 (72.17%) |
| CKLM top-3 hits / eligible | **14,508 / 91,211 (15.91%)** |
| Unigram top-3 hits / eligible | **10,298 / 91,211 (11.29%)** |
| Difference | +4,210 hits, +4.62 percentage points |
| CKLM words / context rows / pairs | 7,712 / 6,646 / 22,916 |
| Bytes / SHA-256 | 173,629 / `f560e7424f867818692a0a2f574ae4c1155e8b3a6983f25795c6109cb3006712` |
| Gate | **PASS**: 91,211 ≥ 1,000; 14,508 > 10,298; 173,629 ≤ 524,288 |

No wordfreq(ET) call or cross-corpus artefact-ratio filter is applied: `wordfreq` may raise `LookupError` for ET, while comparing a small Tatoeba corpus against news falsely flags common Estonian terms. A transparent **no-filter** policy preserves them instead of silently dropping them. Sentence length, deduplication, dictionary gating and count threshold remain in effect. This choice can retain repeated names/news entities; inspect with real-device replay before enabling.

## Limitations

Heldout is drawn from these **same source types**, not conversational text; Leipzig's much larger news sample dominates both training and evaluation, so the gain is not evidence of swipe-ranking benefit or broad casual-language quality. Sentence hash deduplication does not detect near-duplicates or newswire paraphrases. Top-3 accuracy excludes OOV targets (40,057 of 131,268 positions), and the row coverage shows contexts left to fallback. Viipio loads this model off the IME thread and applies its listed evidence only to **already offered ET typed suggestions**, never to automatic ET swipe top-1. That integration needs separate JVM and installed-editor validation; this corpus test alone does not prove it.

## Sources

[1] https://web.archive.org/web/20231031111216/https://wortschatz-leipzig.de/en/usage — Leipzig usage terms, archived 2023-10-31
[2] https://tatoeba.org/en/downloads — Tatoeba downloads
[3] https://tatoeba.org/en/terms_of_use — Tatoeba terms of use
