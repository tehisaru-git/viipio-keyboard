# Viipio documentation index

Viipio is an experimental offline Android keyboard. Its name is language-neutral; the **current preview supports Estonian (ET) and English (EN)**. Other language references in inherited CleverKeys documents do not describe Viipio's active keyboard.

## Start here

- [README](../README.md) — current install instructions, capabilities, limits and update compatibility.
- [Viipio identity](branding/viipio.md) — name, artwork and branding guidance.
- [v0.10.1 candidate verification](releases/v0.10.1.md) — **not published**; inline ET/EN personal-word Add/Undo, first-use guidance and release gates.
- [v0.10.1 candidate GitHub notes](releases/v0.10.1-github.md) and [inline word-add contract](specs/viipio-inline-personal-word.md) — draft scope, not an installable release.
- [v0.10.0 release verification](releases/v0.10.0.md) — ET exact-word exclusion, guarded EN swipe context, ET/EN typed context, opt-in local corrections, full JVM and signed emulator gates, plus public byte-identical APK readback.
- [v0.10.0 GitHub release notes](releases/v0.10.0-github.md) — published [normal latest release](https://github.com/tehisaru-git/viipio-keyboard/releases/tag/v0.10.0) with one signed ARM64 APK.
- [ET static model provenance and heldout test](research/viipio-et-static-lm.md) — pinned Leipzig/Tatoeba source, attribution, reproducibility and limitation.
- [v0.10.0 ranking protocol](eval/viipio-v010-ranking-design.md) — data limits, privacy policy and planned before/after evaluation.
- [v0.9.1 release verification](releases/v0.9.1.md) — continuous emoji categories, tone choices, kaomoji layout, signed update and physical-device limits.
- [v0.9.1 GitHub release body](releases/v0.9.1-github.md) — historical ARM64 APK, checksum and validation limits.
- [Gboard emoji scroll/tone observation](research/gboard-emoji-scroll-tone-observation.md) — bounded emulator behavior and icon proportions; no copied code or artwork.
- [v0.9.0 release verification](releases/v0.9.0.md) — emoji browse Backspace, separate privacy erasure, signed update and remaining limits; public APK checked after release.
- [v0.9.0 GitHub release body](releases/v0.9.0-github.md) — previous ARM64 APK, checks and update path.
- [v0.8.0 release verification](releases/v0.8.0.md) — edge glide, earlier-word suggestions, personal-word refresh, signed update and remaining limits.
- [Gboard emoji-pane observation](research/gboard-emoji-observation.md) — bounded Android 15 emulator comparison for Browse, Backspace, Search and ABC→Send; not a claim about proprietary internals.
- [ENC 2021 frequent word forms](research/eki-enc21-common-word-forms.md) — CC BY public reference list, not a shipping decoder ranking.
- [v0.7.0 full-suite triage](development/v070-test-triage.md) — post-release regression classification and test progress (not a new published APK).
- [v0.7.0 identity migration and verification](releases/v0.7.0.md) — neutral App ID, separate installation and remaining test gates.
- [v0.7.0 GitHub release body](releases/v0.7.0-github.md) — experimental new-app APK and migration warnings.
- [v0.6.0 release verification](releases/v0.6.0.md) — personal words, consented input evidence and limits.
- [v0.6.0 GitHub release body](releases/v0.6.0-github.md) — experimental APK changes and prominent test warnings.
- [v0.5.1 release verification](releases/v0.5.1.md) — previous update scope and remaining gaps.
- [v0.5.0 release verification](releases/v0.5.0.md) — previous tested scope.
- [Gboard research and prioritized Viipio backlog](plans/viipio-gboard-feature-backlog.md) — cited help pages, read-only emulator observations, acceptance gates and separately testable milestones.
- [Earlier Gboard feature map](plans/viipio-gboard-parity.md) — historical version proposals and active-versus-inherited distinction; use the newer backlog for current priorities.
- [Development quality gates and future autonomous workflow](development/viipio-quality-gates.md) — measured bottlenecks, fast feedback, guarded Gradle, release criteria and proposed bounded recurring work.
- [ET/EN language-mode policy](specs/viipio-language-modes.md) — proposed English-only, Estonian + English and optional Estonian-only choices; **not shipped in v0.8.0**.
- [Language-pack and offline APK distribution plan](plans/viipio-language-distribution.md) — current ET+EN APK; **future alternative** single APK with about six preinstalled, first-run selectable languages, plus optional in-app downloads and later APK variants; plan only.
- [Local input diagnostics](eval/local-swipe-diagnostics.md) — independently consented ET swipe, ET completed words and experimental ET/EN Viipio-owned input events added in v0.6.0, with exclusions, export and erasure.
- [Estonian offline dictionary](specs/estonian-offline-dictionary.md) and [human-swipe research](eval/human-swipe-research.md) — lexicon provenance and outstanding accuracy evidence.

## Inherited documentation

Much of `docs/specs/`, `docs/wiki/`, `docs/history/` and `docs/eval/` originates from [CleverKeys](https://github.com/tribixbite/CleverKeys). Its historical feature lists, language counts, test results, milestones and developer paths are **upstream records, not Viipio release claims**. Use the [inherited wiki index](wiki/TABLE_OF_CONTENTS.md) to navigate them, and check the [README](../README.md) and current Viipio release notes before describing shipped behavior. GPL-3.0 attribution and provenance are retained in [NOTICE](../NOTICE) and the [archived upstream README](../UPSTREAM_README.md).
