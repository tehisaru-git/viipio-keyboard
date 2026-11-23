# Viipio keyboard

Viipio is an experimental, offline Android keyboard for **Estonian and English**. Type by tapping or gliding; word suggestions and emoji work on the device.

## Install with Obtainium

Install [Obtainium](https://github.com/ImranR98/Obtainium) on your Android device if you do not have it yet.

**On your phone:** [Add Viipio to Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2522io.github.tehisaru.viipio%2522%252C%2522url%2522%253A%2522https%253A%252F%252Fgithub.com%252Ftehisaru-git%252Fviipio-keyboard%2522%252C%2522author%2522%253A%2522tehisaru-git%2522%252C%2522name%2522%253A%2522Viipio%2520keyboard%2522%257D) — opens a confirmation in Obtainium, not an automatic APK install. **On another screen:** scan this QR code with your phone; it opens the same shortcut.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2522io.github.tehisaru.viipio%2522%252C%2522url%2522%253A%2522https%253A%252F%252Fgithub.com%252Ftehisaru-git%252Fviipio-keyboard%2522%252C%2522author%2522%253A%2522tehisaru-git%2522%252C%2522name%2522%253A%2522Viipio%2520keyboard%2522%257D"><img src="docs/assets/viipio-obtainium-qr.png" alt="Scan to add Viipio in Obtainium" width="260"></a>

1. Confirm the Viipio entry and its GitHub source in Obtainium. If your browser blocks the shortcut, try the button on its redirect page. You can also open **Add App** in Obtainium and paste this source URL manually:
   ```text
   https://github.com/tehisaru-git/viipio-keyboard
   ```
   Add the app. Obtainium recognises GitHub repositories and follows their releases ([official Add App guide](https://wiki.obtainium.imranr.dev/ui_overview/); [deep-link guide](https://wiki.obtainium.imranr.dev/deep_links/)).
2. Open Viipio in Obtainium and install the APK from the [latest published release](https://github.com/tehisaru-git/viipio-keyboard/releases/latest). The published APK is for **arm64-v8a** Android devices; check your device's architecture if installation reports an incompatibility. Confirm the app ID is `io.github.tehisaru.viipio` and approve the Android installation prompt if shown.
3. Open **Viipio keyboard → Keyboard setup & practice**. Enable Viipio in Android's keyboard settings, select it as the active keyboard, and try typing. Keep another keyboard enabled until you have tested it in your apps.
4. For later updates, check the Viipio entry in Obtainium and install the new release it offers. If you installed Viipio from a differently signed build, Android may refuse an in-place update; use the signed GitHub release APK for updates.

## Features

- Separate **ET** and **EN** input modes, with tap typing, glide typing and word suggestions. Suggestions do not replace text automatically.
- Add a new word to the selected language while typing with **+ Add**, then **Undo** if needed; personal words stay on the device.
- Offline emoji browser, long-press character choices and a larger **Key pop-up** on character presses. Disable the pop-up under **Settings → Preferences** if you prefer.
- No Internet or microphone permission in the published APK. Diagnostic collection and swipe-correction learning require separate opt-in. Suggestions and personal-word actions are suppressed in sensitive fields. The key pop-up can still display the **pressed key** in a password field; turn it off in Preferences if you do not want that visual feedback.

## Before relying on it

Viipio is experimental. Synthetic emulator checks do **not** establish real-finger swipe accuracy, latency or compatibility with every app. Keep another keyboard available and review important text before sending it.

[Latest release and release notes](https://github.com/tehisaru-git/viipio-keyboard/releases/latest) · [Documentation](docs/TABLE_OF_CONTENTS.md) · [Build from source](docs/development/build-viipio.md)

Viipio's source code is distributed under [GPL-3.0](LICENSE). [NOTICE](NOTICE) contains upstream and third-party attribution, including bundled data.