# Contributing to Viipio

Viipio is an experimental, offline Android keyboard with Estonian and English input. Contributions should preserve working tap/glide typing, sensitive-field protections, local-only operation and keyboard accessibility.

## Report a problem or suggest a change

- Use the [bug report template](https://github.com/tehisaru-git/viipio-keyboard/issues/new?template=bug_report.yml) or [feature request template](https://github.com/tehisaru-git/viipio-keyboard/issues/new?template=feature_request.yml).
- Include the Android version, device type, installed Viipio release, exact steps and what you expected. Use a **synthetic** example for typing or swipe problems. Never post real passwords, personal text, raw keyboard diagnostics or credentials in a public issue.
- Check the [release notes](https://github.com/tehisaru-git/viipio-keyboard/releases/latest) and [current feature overview](README.md) before reporting a mismatch.

## Change the source

1. Read [build instructions](docs/development/build-viipio.md) and [Viipio quality gates](docs/development/viipio-quality-gates.md).
2. Add a focused regression test for behavioral changes. Run the Python quick checks, targeted Android tests and the full suite before proposing a release.
3. Keep source and data licences compatible with [GPL-3.0](LICENSE). Preserve existing copyright and third-party attributions in [NOTICE](NOTICE); document any new bundled data source and its licence.
4. In a pull request, describe what changed, how it was tested and what remains unverified on a physical device. Do not present emulator-only results as measured human swipe accuracy.