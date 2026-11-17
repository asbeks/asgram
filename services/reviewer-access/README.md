# ASGRAM reviewer sign-in

The production reviewer signs into the dedicated Telegram test account with a reusable ASGRAM access code. A reviewer installation starts a new Telegram QR token and Telegram returns a distinct device session after approval. The account session used to approve tokens stays on the gateway. Normal Telegram sign-in and offline Review demo remain available.

## Prepare the dedicated account

1. Use an existing Telegram account dedicated to review. Add sample contacts, a group, a channel and a chat partner for live messaging and calls. Keep personal data, admin privileges and paid accounts out of it.
2. From the project root run `npm --prefix services/reviewer-access ci --ignore-scripts`, then `npm --prefix services/reviewer-access run provision` in Terminal. From the dedicated account use Telegram **Settings → Devices → Link Desktop Device** to scan the QR.
3. Provisioning stores the gateway environment and the separate reusable reviewer code in `.asgram/reviewer-access/`. The files are ignored by Git and readable only by the local owner. Keep the runtime environment in the host's secret store and put the reusable code only in Google Play's reviewer sign-in instructions.

## Deploy and verify

1. Run one container behind an HTTPS reverse proxy with `/v1/asgram/reviewer/authorize` forwarded to container port 8098. Deploy with the provisioned runtime environment as server-side variables. Redact the authorization header and request bodies from proxy and service logs. Keep the origin port private. A second replica would require shared rate-limit and replay state.
2. Build ASGRAM with `ASGRAM_REVIEW_ACCESS_URL=https://<review-host>/v1/asgram/reviewer/authorize` and `ASGRAM_REVIEW_TELEGRAM_ID` set to the dedicated account ID. These two settings are public metadata included in the app. Keep the private Telegram session and reviewer code out of build inputs.
3. On a clean release install, tap **Reviewer sign-in**, enter the reusable ASGRAM code from Play Console, and verify that the correct account opens. Test a message, a group/channel, a real call, and the location disclosure. Repeat on another clean install or after revoking the first device session. Telegram can still impose anti-abuse limits and account-specific checks, including a password challenge. Verify the test account signs in through this route before declaring full access.
4. In Play Console go to **Policy and programs → App content → App access**. Enter clear English instructions, the reusable code and any sample destinations needed to review the app. Confirm the declaration only after the release build works from a fresh installation. Leave the account and gateway working for review and subsequent rechecks.

If access must be revoked, replace the code digest on the gateway and revoke reviewer device sessions from the dedicated account's Telegram **Devices** settings. Rotating the code stops new logins. Active Telegram device sessions remain active until separately revoked. Keep the Play Console instructions current when rotating.

See [Telegram's QR login flow](https://core.telegram.org/api/qr-login) and [Google Play reviewer access requirements](https://support.google.com/googleplay/android-developer/answer/15748846).
