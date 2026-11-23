# Viipio keyboard · identity and UI guide

**Displayed name:** `Viipio keyboard` (preserve this capitalization in the launcher, Android IME picker and setup). **Repository:** `tehisaru-git/viipio-keyboard`. **Android package ID:** `io.github.tehisaru.viipio`. Keep the package ID and release-signing certificate stable across updates so Android can install new versions in place.

## Mark

The symbol has three light rounded keycaps and one continuous pale-mint gesture with a terminal dot. Use the vector `res/drawable/ic_launcher_foreground.xml` on Android 26+ with `ic_launcher_background.xml`, its single-colour `ic_launcher_monochrome.xml` on themed launchers, and the generated PNGs for legacy densities. `scripts/generate_viipio_icon.py` is the reproducible PNG source. Review the actual 48 px and 192 px rasterizations whenever geometry changes. Avoid words inside the icon: small labels become illegible.

## Visual tokens

- Icon background: deep navy `#22364C`; keycaps: soft white `#EDF3F7`; gesture accent: mint `#A5E4D9`.
- Setup light surface `#F6F8FB`, ink `#1F2C39`; setup dark surface `#151D28`, ink `#E5EAF1`. Source of truth for app colors remains `theme/ViipioTheme.kt`.
- Active keyboard keys keep their independently drawn slate/white palette (`estonianui/KeyboardSurfaceView.kt`) and clear blue action key. The icon accent need not tint every key; input legibility takes priority over decoration.
- Avoid animation on the keyboard itself, text in the launcher mark, crowded toolbars, decorative gradients and low-contrast selection states. Follow system light/dark mode and ensure TalkBack describes the full candidate word even when visually shortened.

## Copy and attribution

Use short English labels for user-facing setup and controls; Estonian and English are *input languages*, not separate launcher locales. Keep GPL-3.0 and upstream/third-party attribution in `LICENSE` and `NOTICE`.

## Release checks

Check both Android application and IME picker labels, all five density-specific legacy PNGs, adaptive/themed icon, the launcher in system light/dark modes, GitHub source link, package ID, versionCode and release-signing certificate. Back up the private key securely off-host so future releases can update the installed app. Check the signed APK on a real device before making claims about touch feel or accessibility; a source screenshot alone does not verify installed behavior.
