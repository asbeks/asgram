# ASGRAM Android

ASGRAM is an unofficial native Telegram app. Version 0.3.2 (9), package com.abdiisabaa.asgram, Android 7+.

The native Java interface includes Feed, Chats, Spaces, Settings and You. Telegram handles sign-in, conversations, private groups, channels, bots and calls. Feed reads verified public channels, offers Latest and For you, topic filters, saved stories, full readers and complete native photo albums. Native Telegram light, dark and custom themes remain available. Channel invitations are optional and require a deliberate Join action; accounts are never silently subscribed.

## Build

The source is pinned to Telegram Android 13.1.1, commit 3de37c8. Matching official native libraries are reused for arm64-v8a, armeabi-v7a, x86 and x86_64. Exact vendor APK and native hashes are in Tools/asgram/native-provenance.json. This is not a full native C++ rebuild.

1. Install JDK 21, Android SDK 36 and build-tools 36.1.0.
2. Initialize the Java submodules: `git submodule update --init --depth=1 TMessagesProj/lib/jlatexmath TMessagesProj_Modules/media`.
3. Run `python3 Tools/asgram/prepare_native.py --apk /path/to/matching/Telegram.apk`.
4. Set sdk.dir in ignored local.properties, and your own Telegram API ID/hash in ignored asgram.local.properties (see its example). Configure ASGRAM_MAPS_API_KEY there, enable Maps SDK for Android in the key's project, and restrict it to that API and com.abdiisabaa.asgram with the debug, upload and Google Play app-signing certificate SHA-1 fingerprints. Packaging fails if the key is absent; the Maps SDK otherwise crashes asynchronously when opening location sharing. Enabling the API and any required Google account/billing verification must be completed separately from adding the key.
5. Register com.abdiisabaa.asgram in your own Firebase project and put google-services.json in ASGRAM/. Configure Telegram application push delivery as documented at https://core.telegram.org/api/push-updates. Never reuse upstream Firebase credentials.
6. For debug: `./gradlew :ASGRAM:assembleDebug :TMessagesProj:testDebugUnitTest --tests org.telegram.messenger.asgram.AsgramFeedTest`.
7. For release, create a private upload keystore and ignored ASGRAM/keystore.properties with storeFile, storePassword, keyAlias and keyPassword. Run `./gradlew :ASGRAM:bundleRelease :ASGRAM:assembleRelease`.
8. Check the actual merged release manifest: `python3 Tools/asgram/check_manifest.py ASGRAM/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml --release`. ASGRAM must explicitly declare foreground coarse/fine location permissions and remove the background permission inherited from Telegram's library.

On this Mac, JAVA_HOME is /Applications/Android Studio.app/Contents/jbr/Contents/Home. Signing keys, Telegram API credentials and Firebase configuration must not be committed. Release tasks fail closed when their configuration is missing.

## Store artwork

The debug-only StorePreviewActivity uses the production Feed and reader components with English demo content. It never reads account content into its Feed and is absent from release manifests. Store artwork must not show personal chats, profiles, contact details or the real publisher logo. Promo text describes ASGRAM as an unofficial Telegram app. See ASGRAM-PLAY-LISTING.md.

## Premium gift service

services/premium-shop contains a separate server-side payment and fulfillment implementation with 17 tests. It is not connected to a live app checkout. Sales remain disabled until a dedicated funded gift bot, actual Star costs, production payment configuration and end-to-end fulfillment are verified. A successful build does not prove live payment delivery.

## Verification

Version 0.3.2 passes twenty-two JUnit tests for feed/directory/album behavior and location consent/service policy, plus eight brand/privacy and packaging checks. Its release manifest, bundle and signing must also be verified before distribution. Earlier real sign-in was completed by the account holder; native chats, Feed, full albums, Spaces and light/dark continuity were checked in the emulator before store preparation. The 0.3.2 correction does not use the signed-in emulator. Background push delivery, calls and every native Telegram setting have not all been proven end to end. See ASGRAM-LOCATION-REVIEW.md for the correction and ASGRAM-VERIFICATION.md for earlier evidence.

The corresponding modified source is published under the retained GPL license. No private credentials or signing material belong in the public repository.

## Location privacy correction (0.3.2)

The ASGRAM manifest removes the library's ACCESS_BACKGROUND_LOCATION permission. Current-location features disclose data use before permission requests, including on an upgrade with an existing Android grant. Live sharing has its own explicit disclosure and continues only as a user-initiated foreground location service with an ongoing notification and stop control. New services cannot start while the UI is hidden, and GPS providers refuse access without the corresponding consent and visible-activity or active-sharing state. Foreground and live consents are separate from sign-in. Back, outside dismissal and Not now never count as permission approval. Existing signed-in emulator data is preserved; this correction does not use private chat screenshots. See ASGRAM-LOCATION-REVIEW.md for resubmission evidence and limits.


## Required Android updates

ASGRAM checks the public [Android release policy](https://asbeks.github.io/asgram/android-release.json) when the app opens or returns to the foreground. A build below `minimumBuild` shows a required update only when Google Play offers that device a build at or above the minimum. The Update button opens Play's immediate update flow or the ASGRAM store listing. A failed check leaves the app usable.

After a new release is available on Google Play, set `latestBuild` to its version code in `docs/android-release.json`. To require a minimum version, set `minimumBuild` to that published version code and deploy the existing ASGRAM Pages workflow. Set it to `0` to turn off the requirement. Build 14 introduces the check, so earlier installations first need a normal Play update.
