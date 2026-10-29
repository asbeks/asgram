# ASGRAM Android

ASGRAM is an unofficial native Telegram client by Abdii Sabaa Media. The current build is an internal development build: version 0.2.0, package com.abdiisabaa.asgram, arm64 Android 6 or newer.

## What is implemented

- Telegram Android 13.1.1 native messaging engine and screens, with ASGRAM’s own name and launcher/notification identity.
- A native welcome screen with an A/conversation illustration, one Start Messaging action, paired light/dark appearance and a language-only picker.
- Two native ASGRAM themes: forest green on white and mint on dark green. They theme the existing login, conversation, composer and settings controls; original/custom themes remain available.
- A one-time invitation for signed-in non-members to explore @ABDII_SABAA, with Not now and a separate channel shortcut. Opening the channel does not subscribe the account.
- Standard phone sign-in on Telegram’s production network; the developer network checkbox is hidden unless the account is already on a test backend.
- Verbose client logging remains disabled by default.
- The former media hub remains deferred and is not ASGRAM’s home screen.

Core Telegram screens remain Android Java/native code. This is not a website wrapper and is not Flutter. A Flutter version is possible using a Telegram client engine such as TDLib, but would require a separate interface and integration project.

## Build

The source is pinned to Telegram Android 13.1.1, commit 3de37c8. The internal arm64 build reuses the matching official native library. The exact APK and native-library checksums are recorded in Tools/asgram/native-provenance.json. This is not a full from-source native binary build.

1. Install JDK 21, Android SDK 36, build-tools 36.1.0 and NDK 28.2.13676358.
2. Initialize the Java dependencies: git submodule update --init --depth=1 TMessagesProj/lib/jlatexmath TMessagesProj_Modules/media
3. Obtain the matching official 13.1.1 (7237) APK from Telegram's verified @TAndroidAPK release channel or the vendor download. Run python3 Tools/asgram/prepare_native.py --apk /path/to/Telegram.apk. It stops if the vendor APK or native-library checksum differs from Tools/asgram/native-provenance.json. The ordinary vendor URL may temporarily serve a previous release; do not mix versions.
4. Set sdk.dir in ignored local.properties.
5. Copy asgram.local.properties.example to ignored asgram.local.properties, enter the app's own registered API credentials, and restrict access to that file. Never commit those values.
6. Run ./gradlew :ASGRAM:assembleDebug :TMessagesProj:testDebugUnitTest --tests org.telegram.messenger.asgram.AsgramFeedTest

On this Mac, set JAVA_HOME to /Applications/Android Studio.app/Contents/jbr/Contents/Home before running Gradle.

The APK is ASGRAM/build/outputs/apk/debug/ASGRAM-debug.apk. Upstream source and its GPL licence are retained. A distributed release must also make the corresponding modified source available, excluding credentials and signing material.

## Verification and remaining release work

The debug APK builds, signature verification passes, all six retained feed-parser tests pass, and four native-brand checks pass (theme contrast, own identity/native login, no automatic subscription, production sign-in). The native welcome has been inspected in light and dark mode on Medium Phone API 36.1. These checks do not establish authenticated messaging or closed-app push delivery. See ASGRAM-VERIFICATION.md for the final installed artifact and runtime evidence.

The following must still be verified before public release:

- A real Telegram account sign-in, including code/2FA handling by the account holder.
- Authenticated chats, channels, calls, downloads, session recovery and device permissions.
- ASGRAM-owned Firebase configuration, Telegram application push configuration and background notification delivery while the app is closed.
- Release signing, required ABIs, Play requirements and publication of corresponding modified source.
- Authenticated messaging screens after all ASGRAM branding changes.

Do not interpret a successful build or a working onboarding screen as proof of these untested flows. No Google Play release has been submitted for ASGRAM.
