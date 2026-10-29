# ASGRAM native design verification

Checked October 29, 2026. Internal debug build 0.2.0 (3), com.abdiisabaa.asgram, arm64, Android 6+.

## Implemented

- Official Telegram Android 13.1.1 source, upstream commit 3de37c8; the official upstream HEAD was checked on October 29 and remained at that commit.
- Matching official native library from Telegram 13.1.1 (7237). Vendor signer and APK/native hashes are recorded in Tools/asgram/native-provenance.json. This internal build reuses that native binary; it is not a complete native C++ rebuild.
- ASGRAM native welcome with its own A/conversation illustration, a single Start Messaging action, language-only selection and paired light/dark appearance.
- ASGRAM and ASGRAM Night native themes, including readable input hints, selection borders and cursor colors. Custom/imported themes are preserved; all original themes remain available.
- ASGRAM wordmark, launcher and notification glyph. The former media hub remains in source with no active entry.
- Standard native production LoginActivity. The developer backend checkbox is omitted unless an existing session already uses the test network.
- Signed-in non-members may receive one optional Abdii Sabaa invitation. View channel opens the native channel; Not now dismisses it. There is no automatic subscription and no membership gate. A separate channel shortcut remains available.
- Verbose client logging is off by default; private API credentials remain ignored and restricted locally.

## Verification

- Final assembleDebug passed in 16 seconds; log: .asgram/build-native-brand-final.log. The preceding full Java build and six parser tests also passed.
- Six retained feed-parser JUnit tests passed, zero failures/errors/skips. They test the deferred feed parser, not authenticated messaging.
- Four Python brand/boundary checks passed: paired theme contrast, own identity/native login, no automatic channel subscription, and production sign-in. Contrast checks include focused-field labels, hints and borders.
- APK signature verification passed for v1 and v2. This is a debug-signed internal APK, not a release-signed distribution artifact.
- Installed version 0.2.0 (3) on Medium Phone API 36.1, emulator-5554. App data was retained during replacement, and the temporary emulator storage reserve was restored to its original null value.
- Visually checked final native welcome in light and dark mode, readable language list without translation/premium controls, and standard phone sign-in without the Test Backend option.
- No new crash-buffer entries appeared during the final pre-login checks. The app process remained alive. This is limited runtime evidence, not proof of every Telegram feature.
- Actual phone captures: .asgram/screenshots/asgram-0.2.0-welcome-light.png and asgram-0.2.0-welcome-dark.png. Additional language/sign-in evidence is saved alongside them.

## Remaining before public release

- Account-holder completion of real Telegram sign-in, including their own login code and two-step password. No code or password should be sent in chat.
- Authenticated conversations, groups, channels, downloads, calls, session recovery and device permissions.
- Authenticated invitation/shortcut behavior and the themed messaging screens.
- ASGRAM-owned Firebase and Telegram push configuration, plus delivery while the app is closed. These are not proven by the inherited source or pre-login tests.
- Release signing, required ABIs, corresponding modified-source publication, Play Console setup and review.

No ASGRAM Play Store release has been submitted. No account subscription, real message or phone-login request was sent during these UI checks.

Final APK: ASGRAM/build/outputs/apk/debug/ASGRAM-debug.apk

Final APK SHA-256: c2c13d9bc2b659fb5859f0f402d2428fc19eb8238700945cdea423c3135189fa
