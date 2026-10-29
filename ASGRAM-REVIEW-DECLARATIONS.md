# ASGRAM release declaration notes

Prepared 29 October 2026. These notes do not mean Play Console submission is complete.

- App: ASGRAM, com.abdiisabaa.asgram, native Telegram client fork, version 0.3.0 (code 6).
- Listing identifies it as an unofficial Telegram app. No private account screenshots are permitted.
- Privacy: https://asbeks.github.io/asgram/privacy.html
- Support: support@fulbek.com, as previously confirmed by the owner.
- Source: https://github.com/asbeks/asgram/tree/codex/asgram-13.1
- Signing and four-ABI release artifacts are ready under outputs/play/.
- Premium ETB purchase functionality is not enabled and must not be advertised.
- Background notifications remain unverified; do not claim they are fully tested.
- Native Telegram sponsored messages may appear; declare ads if included by the compiled client.
- Privacy/data safety declarations must include Telegram cloud data handling and included SDKs, not only ASGRAM-operated servers. Ordinary cloud chats are not end-to-end encrypted.

## Review-access requirement

Google Play requires reusable access that works regardless of reviewer location, including a method that does not depend on one-time passwords. The current release requires real Telegram sign-in. Never supply a personal Telegram session, OTP or the owner's private account. The debug-only English StorePreviewActivity is absent from release and cannot be offered as if it were available to reviewers. A reviewer-access strategy needs to be resolved honestly before claiming this requirement complete.

Official requirement: https://support.google.com/googleplay/android-developer/answer/15748846

## Local resource limit

The Mac had about 1.2 GiB free during this check. Do not wipe the signed-in emulator or user account to install the differently signed release APK. Preserve the existing authenticated test data. A final signed-build runtime check should use a separate emulator with adequate space.

## Confirmed review blocker

On 29 October, direct calls to Telegram test DCs confirmed test_mode=true but returned PHONE_CODE_INVALID for documented reserved numbers and fixed codes. Telegram maintainer confirms these public test accounts are disabled: https://github.com/tdlib/td/issues/3370 . The attempted test-server UI was removed before release. Owner chose to prepare all release materials and explain the remaining step, without a dedicated review account yet. Do not claim full reviewer access or submit the production rollout until a dedicated non-personal account and reviewer-compatible code retrieval are arranged.

## Play upload validation

Play rejected build 5 with automatic protection enabled because minSdkVersion was 23. Build 6 raises the ASGRAM app minimum to API 24 (Android 7), keeping Play automatic protection enabled. No new test-server sign-in UI is included.
