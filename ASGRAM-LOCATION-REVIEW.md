# ASGRAM 0.3.2 (9): location policy correction

The Google Play email dated 3 November 2026 rejected ASGRAM for Missing Prominent Disclosure, specifically access to BACKGROUND_LOCATION. Its evidence shows the welcome and Telegram sign-in screens. The original APK inherited ACCESS_BACKGROUND_LOCATION from the Telegram library. Its background explanation named Telegram and was reached only inside Live Location.

## Shipped behavior

- The application overlay removes ACCESS_BACKGROUND_LOCATION. Only coarse/fine while-in-use access and the existing location foreground-service type remain.
- Normal onboarding shows ASGRAM's live-location information before sign-in. Continue without location grants no consent and requests no Android location permission.
- A current-location feature presents the ASGRAM disclosure before its Android runtime request. An existing Android permission does not bypass the app-level consent after upgrade.
- Live Location presents a separate disclosure naming ASGRAM, location data, Telegram/chat recipients, use when the app is closed or not in use, chosen duration, ongoing notification and how to stop. Not now does not start sharing.
- GPS and cached-location providers are gated by consent and UI visibility, or by consented live sharing with the foreground service running. Cold background service starts are refused. Maps disable their location layer when paused.
- Removing fused updates uses the same registered callback instance. Stopping a live share does not leave that listener registered because of a callback identity mismatch.
- Android can stop a foreground service. This release does not claim always-on tracking, restart-after-force-stop or silent recovery.

The foreground-service approach is the actual implementation, not a change to the form alone. Google permits foreground location service usage as continuation of a user-initiated in-app action, with prompt termination when its intended action ends. See https://support.google.com/googleplay/android-developer/answer/9799150 and https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start.

## Play Console update

1. Create the replacement release with version code 9 only. Remove the rejected/retained code 8 (and code 7 if retained) from that draft; do not mix the old background-permission artifact with the correction.
2. Refresh the location declaration from the new artifact. Declare the location foreground-service purpose as user-started live location sharing; the new artifact does not request ACCESS_BACKGROUND_LOCATION. Data safety must still describe optional precise/approximate location use and sharing through Telegram and map/place providers. Do not say the app has no location use.
3. Use the current privacy URL https://asbeks.github.io/asgram/privacy.html, with the updated Location and Live Location section. Keep the store listing description accurate about optional location sharing and its notification/stop behavior.
4. Send changes for review from Publishing overview. This is a correction, not an assertion that Google's rejection was mistaken. Uploading a bundle is distinct from submitting it, approval and public availability.

## Suggested reviewer note

ASGRAM 0.3.2 (9) removes ACCESS_BACKGROUND_LOCATION from the merged application manifest. Location is optional for sign-in and messaging. Foreground location requests are preceded by ASGRAM's explanation and affirmative consent. User-started Live Location has a separate disclosure explaining location updates through Telegram to the selected chat even when ASGRAM is closed or not in use, its chosen duration, ongoing notification and stop control. New location services do not start silently from the background. The privacy policy has been updated. Please review version code 9; the rejected artifacts must not be retained in this replacement release.

## Verification boundaries

Twenty-two native JUnit tests (13 feed, 6 location policy, 3 consent decisions) and eight Python brand/privacy/packaging checks pass. The packaging regression check rejects background permission in either uses-permission or uses-permission-sdk-23. Use the actual merged manifest and AAB dump for permission/version checks and release signature checks as well. The user's signed-in emulator is not cleared, overwritten or captured. No new Telegram message, live location or real coordinates are sent by the unit and packaging checks. These are unit and package checks, not an end-to-end device test of the new consent flow. A signed build and a policy correction do not establish Google Play approval. Record actual submission separately; do not claim a submission based on a local artifact.
