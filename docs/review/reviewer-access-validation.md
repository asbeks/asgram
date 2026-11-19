# ASGRAM reviewer access

Google Play Developer Support case 4-4268000041744 identified device location disclosure and reusable reviewer access in its review of version 8.

## Sign-in details for Play Console

Instruction name: ASGRAM Review demo

On a fresh install, open ASGRAM and tap **Review demo** below **Start Messaging**. No phone, password or one-time code is needed for the demo.

- **Feed** opens the native story feed and reader with sample posts. Search or filter stories, open one, then save it and return to the saved list.
- **Chats** opens sample conversations. Open Amina, type and send a message. Select **Try a call** to review the sample voice and video call controls.
- **Spaces** includes sample private chats, groups, channels and a bot that replies locally. You can search, filter, or create a sample group or channel.
- **Location and privacy** displays the same disclosure text as the live app. You can decline, continue with a sample pin, or select and stop a sample live share.

This is an unofficial Telegram client. The demo is offline and uses synthetic content. It previews call and location controls, and requires no Telegram session or location, microphone or camera permission. Actual Telegram messaging, calls and map sharing use a regular Telegram sign-in from **Start Messaging**.

## Release correction

The merged release manifest removes `ACCESS_BACKGROUND_LOCATION`. Signing in does not require location. A user who chooses Live Location sees a separate disclosure and can stop the foreground service from its ongoing notification.

The earlier test-network route was replaced after two Telegram test data centers rejected the documented static codes. No Telegram authorization session is included in the Android release.

[Google Play reviewer access requirements](https://support.google.com/googleplay/android-developer/answer/15748846)

## Verified release status, November 17, 2026

The signed release APK and uploaded AAB have byte-identical DEX files. On a clean Android 16 emulator, build 12 opened Review demo without credentials. The checks covered reading and saving a story, sending a local message, receiving a sample bot reply, filtering and creating a group, opening and ending a sample call, opening both location disclosures, declining, choosing and stopping a sample live share, and exiting to ordinary sign-in. Both foreground location permissions were still ungranted after the demo. All 25 Java tests and 8 packaging checks passed.

Google Play accepted build 12 into the production draft and displayed it in the release preview. Build 9 was still in review. The new sign-in entry was prepared but Play Console required a declaration that it provided full access, including restricted functionality. The demo covers local workflows and previews. It does not establish a Telegram session or exercise live Telegram calls and sharing. The declaration was left unconfirmed and build 12 was not sent for review.

## Build 13 reviewer sign-in

Build 13 (0.3.6) adds **Reviewer sign-in** for the dedicated Telegram test account. The code from Google Play Console authorizes a fresh Telegram device through the HTTPS gateway. Reviewers enter it on the welcome screen. They do not need an SMS, Telegram login code, or access to the account owner's phone. Regular sign-in and the sample-data Review demo remain available.

The gateway was deployed separately on the existing VPS, behind a route for ASGRAM on the Fulbek HTTPS host. It rejects requests without the reviewer code and rejects invalid Telegram tokens. Local gateway tests covered invalid credentials, malformed input, replay, rate limits and Telegram failures. A clean signed release install on Android 16 authenticated the expected test account through the production gateway and opened the native Feed, Chats, Spaces, Settings and You views. A separate Telegram API test exercised fresh device authentication. An independent end-to-end voice or video call was outside this verification.

All 25 Java tests, eight packaging tests and three gateway tests passed. Bundletool and the release signature verified. The signed APK and AAB contain matching DEX files. The bundle excludes Android's all-the-time location permission, the reviewer access code and the Telegram gateway session.


## Build 14 reviewer sign-in

On a fresh install, touch and hold **ASGRAM** at the upper left of the welcome screen. Enter the existing reviewer code in **Reviewer sign-in**. The offline demo has been removed. Reviewers reach the dedicated Telegram account and can inspect the native Feed, Chats, Spaces, Settings and You screens.

ASGRAM checks the dedicated account's memberships in the four official Abdii Sabaa communities. Maintain all four memberships on the reviewer account. If the membership screen appears, it lists the destinations and **Join all and continue** joins any missing community. If Telegram requires manual approval, review access depends on approval being granted.
