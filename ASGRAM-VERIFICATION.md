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

## 29 October 2026 — production draft preparation

- Final intended upload: ASGRAM 0.3.0, build 6, API 24 minimum / API 36 target, four ABIs.
- Play rejected build 5 because automatic protection requires API 24. Build 6 raises the app minimum to Android 7 and retains automatic protection.
- Release APK signature and 16 KB alignment checks passed. Final AAB/APK hashes are in outputs/play/release-receipt.json.
- English listing, original icon, feature graphic and two native demo screenshots are saved. No personal chats, phone numbers or real media logos were used in listing screenshots.
- Privacy, ads, IARC content rating, government, financial and health declarations are saved. Data safety answers are saved as a draft. Target audience and final Data safety are blocked until reviewer access is supplied.
- A dedicated non-personal Telegram reviewer account is still required. Test DC calls confirmed test_mode=true but documented public test numbers/codes returned PHONE_CODE_INVALID. Telegram maintainer explains the restriction at https://github.com/tdlib/td/issues/3370 . The nonworking test-server option was removed from the release.
- No public rollout or review submission has occurred. The owner chose preparation only until the reviewer account is available.
- The existing authenticated debug app continued opening successfully after build 5; account data was preserved. The secondary emulator failed with SIGSEGV during boot. Build 6 debug installation ran out of emulator storage, so build 6 release runtime has not been proven on an emulator. Temporary storage-reserve settings were restored.
- Background push remains unverified. The ETB Premium shop is disabled.
