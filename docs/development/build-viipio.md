# Build Viipio from source

This guide concerns the active Viipio Android app (`io.github.tehisaru.viipio`), not the inherited CleverKeys release pipeline.

## Requirements

- Java 17 and Android SDK platform 34. Point `ANDROID_HOME` (and, if needed, `ANDROID_SDK_ROOT`) at the SDK.
- Use the repository's guarded Gradle entry point on Linux; it serializes builds on the shared host:

  ```sh
  ANDROID_HOME=/path/to/android-sdk bash scripts/gradle-guard.sh assembleDebug
  ```

- The tracked English dictionary source at `scripts/dictionaries/en/en_enhanced.bin` is copied to the ignored production asset during a clean build. The Estonian dictionary is in `src/main/assets/dictionaries/`. Keep the tracked inputs present when reproducing an APK.

## Checks

```sh
python3 scripts/viipio-check.py quick
ANDROID_HOME=/path/to/android-sdk python3 scripts/viipio-check.py targeted
ANDROID_HOME=/path/to/android-sdk python3 scripts/viipio-check.py full
```

`quick` checks the Viipio Python tests and Git whitespace; `targeted` and `full` run guarded Android JVM tests. A debug APK is not the published release APK.

## Release signing

The official release uses a long-lived signing certificate and package ID so that Android can update an installed copy in place. Keep the private signing key and its password outside Git in mode-0600 files under `~/.config/viipio-keyboard/` (directory mode 0700); securely back them up outside the build host. With the SDK configured, `python3 scripts/sign-viipio-release.py` runs the guarded release build. Inspect the signed APK's package, version, ABI, permissions and signing certificate and verify it on a device before distribution. Never add the key or passwords to source, logs or build artifacts.

The source is available under [GPL-3.0](../../LICENSE). Upstream and third-party attributions remain in [NOTICE](../../NOTICE). Project-specific quality limits and historical test evidence are in [Viipio quality gates](viipio-quality-gates.md) and the [release records](../TABLE_OF_CONTENTS.md).
