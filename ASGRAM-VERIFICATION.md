# ASGRAM 0.3.0 (4) release verification

Checked October 29, 2026. Native Telegram Android 13.1.1 fork, com.abdiisabaa.asgram, Android 6+.

## Confirmed

- Signed APK and Android App Bundle built successfully. Private upload key is restricted and ignored.
- All four Android ABIs included. Both arm64 and x86_64 native libraries have 16 KB ELF load alignment.
- Thirteen JUnit feed/directory/album tests passed, no failures, errors or skips. Six brand/privacy boundary checks and 17 separate premium-shop server tests passed.
- Real sign-in completed by the account holder earlier. Native chats, real five-item photo albums (including page 2), Feed, Spaces, filters and light/dark theme continuity were inspected in the emulator.
- A separate debug-only StorePreviewActivity is used for current Play screenshots. English synthetic posts, generic channel icons and an illustrated landscape replace all account/publisher content. This activity is absent from the release manifest.
- No current store capture contains the signed-in private chat list, profile, phone number, real publisher logo or media name.
- ASGRAM was created in Play Console, app ID 4974411356411501708, following specific user approval of legal declarations.
- Own Firebase Android app registered and module configuration included. Analytics and advertising-ID collection disabled.
- Corresponding modified source published at https://github.com/asbeks/asgram/tree/codex/asgram-13.1 . Privacy page is live at https://asbeks.github.io/asgram/privacy.html (HTTP 200).

## Remaining before claiming public availability

- Complete Play listing, app-content forms, release upload and review submission; verify the actual resulting status.
- Verify Firebase/Telegram background push delivery end to end. Registration and build success do not prove notification delivery.
- Final signed-artifact runtime checks; call/media-permission flows and every inherited Telegram setting are not exhaustively tested.
- Premium purchases with birr are not available: the separate server service remains disabled pending a dedicated funded gift bot, real pricing and live fulfillment verification.

Artifact checksums and current submission status are recorded in outputs/play/release-receipt.json. That directory is ignored; credentials are not published.
