package org.telegram.messenger.asgram;

/** Android-independent decisions shared by the actual permission and service gates. */
public final class AsgramLocationPolicy {
    private AsgramLocationPolicy() { }
    public static final int CONSENT_VERSION = 1;
    public static final String FOREGROUND_DISCLOSURE =
            "ASGRAM uses your location for the location feature you open, such as sharing a pin, nearby places, a bot you approve, or sunset times. " +
            "For sharing and bot/place requests, location is sent to Telegram, the chat or bot you choose, or the relevant map/place provider.\n\n" +
            "Location is optional for signing in and messaging. ASGRAM does not use your location for advertising.";
    public static final String LIVE_DISCLOSURE =
            "ASGRAM collects location data for Live Location and sends updates through Telegram to the participants in the chat you choose, even when the app is closed or not in use.\n\n" +
            "Sharing runs only for the period you choose, with an ongoing location notification. Stop it in the chat or from the notification. " +
            "ASGRAM does not use your location for advertising.";
    public static boolean canRead(boolean foregroundConsent, boolean visible, boolean liveConsent, boolean serviceRunning) {
        return foregroundConsent && (visible || liveConsent && serviceRunning);
    }
    public static boolean canStartService(boolean foregroundConsent, boolean liveConsent, boolean visible, boolean serviceRunning) {
        return foregroundConsent && liveConsent && (visible || serviceRunning);
    }
    public static boolean isLocationPermission(String permission) {
        return "android.permission.ACCESS_COARSE_LOCATION".equals(permission)
                || "android.permission.ACCESS_FINE_LOCATION".equals(permission)
                || "android.permission.ACCESS_BACKGROUND_LOCATION".equals(permission);
    }
}
