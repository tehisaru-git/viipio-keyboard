# Viipio: key-press preview (v0.10.2)

**Priority:** P1 · **Status:** Shipped · **Release:** v0.10.2

## Purpose and reference

In a controlled emulator text editor, Gboard 18.6.3 with Preferences → Key tap → Key pop-up ON showed a large, light circular `s`/`a` indicator during two short taps and committed exactly `sa`. Its symbol page showed the same transient enlargement for `2` and `@` (editor `2@2@`); a synthetic password editor also showed magnified letters. These are bounded behavioral observations, not claims about other versions. Evidence: ignored local `build/tooling/gboard-keypreview-observation/` (`gboard-single-taps.mp4`, frame-004/006/009) and `build/tooling/v0102-key-preview/` (`gboard-number-symbol-taps.mp4`, `gboard-password-taps.mp4`). The official Gboard help describes access to Preferences, not the indicator's shape: https://support.google.com/gboard/answer/6102154?co=GENIE.Platform%3DAndroid&hl=en.

## Requirements

1. A physical pointer-down on an ordinary text-producing key (letter, digit, symbol or punctuation) draws a visibly enlarged version of that key's current label near/above it, while it remains eligible for a short tap. Controls, Space, Backspace and Shift have no character pop-up. The preview is only visual; it must not insert text early, change shift, predictions, accessibility actions or create persistent input records. Letters respect the displayed shift state and the ET/EN keyboard page. Key pop-up is independently switchable in the active Viipio Settings → Preferences, **ON by default**; OFF means no preview, but never disables normal tap or existing long-press alternatives.
2. A glide, a move off the originating key, a completed long press, release, cancellation, second pointer, geometry/page/language change, editor change, view replacement or detachment removes the indicator. A held-choice popup wins over the short-tap preview. Existing swipe trail, hold timing (default 300 ms, user adjustable), Backspace repeat and committed characters must remain unchanged.
3. The original Viipio indicator is legible in light/dark modes, bounded horizontally at edge keys and stays in the measured keyboard view (no extra input-window height or inaccessible overlay). The candidate toolbar and touch targets remain unchanged. Avoid conflating the visual switch with separate sound/vibration or hold-duration controls.
4. Keep existing privacy gates intact: the preview uses only the currently drawn key label and is never saved or sent anywhere. The bounded Gboard password-field observation displayed key pop-ups for short `s`/`a` taps; Viipio may show only the currently pressed label there, with no suggestions, diagnostics or editor-content access introduced. Test a synthetic sensitive editor for privacy regressions; this does not generalize to all OS/Gboard combinations.

## Implementation surface

- `estonianui/KeyboardSurfaceView.kt`: transient pointer/preview state and original rendering on Canvas; do not modify gesture classifier or decoder decisions.
- `ViipioBehaviorPreference.kt`, `ViipioInputMethodService.kt`, active `ui/settings/SettingsScreen.kt`, `SettingsPersistence.kt`, `activities/SettingsActivity.kt`, and English `res/values/strings.xml`: default/persistent toggle and live service read on view creation/start.
- `KeyboardSurfaceViewTest`, `ViipioBehaviorPreferenceTest`, route tests: RED→GREEN pressed pixels, exact gesture, preference default/OFF, hold/glide/cancel/editor/page transitions, active Settings route.

## Acceptance

Run targeted tests and the full Gradle/Python suites on the actual changed source. Install a freshly signed same-ID update on the shared emulator; verify ON/OFF in Settings, a transient preview during ET and EN taps and exact editor readback; check swipe, long-press and sensitive-editor regression. Restore the original emulator IME/foreground once, then publish a separate version/tag only after release checks and read back the public APK against the tested signed file. Human physical-device perception/latency remains unmeasured.
