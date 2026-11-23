# Viipio GitHub repository configuration

This is suggested public-facing copy and a checklist, **not** a claim that GitHub settings or integrations are enabled. Verify repository settings in GitHub before announcing them.

## About

**Description:** Experimental offline Android keyboard for Estonian and English

**Website:** https://github.com/tehisaru-git/viipio-keyboard/releases/latest

**Topics:** `android-keyboard`, `offline`, `open-source`, `swipe-typing`.

The active Android package ID is `io.github.tehisaru.viipio`. Viipio currently supports ET/EN input; do not present inherited modules or plans as shipped capabilities. Keep [GPL-3.0](../LICENSE), [NOTICE](../NOTICE) and third-party attribution available.

## Social preview (optional)

Use Viipio's own artwork and an experimental/offline keyboard description. If languages are mentioned, say “Currently supports Estonian (ET) and English (EN)”; do not advertise a language count, French support, voice input, online services, measured swipe accuracy or unverified accessibility coverage.

## Repository settings to review

- Enable issues if the [bug and feature templates](ISSUE_TEMPLATE/) are to be used; confirm their labels exist before relying on automatic labeling.
- Review vulnerability reporting, dependency alerts, branch rules and workflow permissions for the actual repository. Grant Actions only the permissions each workflow needs; do not assume inherited upstream CI or release workflows are active.
- Link downloads through the [latest release](https://github.com/tehisaru-git/viipio-keyboard/releases/latest); put version-specific APK hashes and test results in release notes rather than the homepage.
- Keep GPL-3.0 licensing, upstream attribution and the archived upstream README visible. Do not replace the inherited project's history with Viipio claims.
