package org.telegram.ui.asgram;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.asgram.AsgramLocationPolicy;
import org.telegram.ui.ActionBar.AlertDialog;

/** No timeout, implicit acceptance, location request at sign-in, or always-on permission. */
public final class AsgramLocationDisclosure {
    private static volatile boolean liveServiceRunning;
    private AsgramLocationDisclosure() { }
    public static boolean isAsgram() {
        return ApplicationLoader.applicationContext != null
                && "com.abdiisabaa.asgram".equals(ApplicationLoader.applicationContext.getPackageName());
    }
    private static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("asgram_location_privacy", Context.MODE_PRIVATE);
    }
    public static boolean hasForegroundConsent() {
        return !isAsgram() || preferences().getInt("foreground_consent", 0) >= AsgramLocationPolicy.CONSENT_VERSION;
    }
    private static boolean hasLiveConsent() {
        return !isAsgram() || preferences().getInt("live_consent", 0) >= AsgramLocationPolicy.CONSENT_VERSION;
    }
    public static boolean canReadLocation() {
        return !isAsgram() || AsgramLocationPolicy.canRead(hasForegroundConsent(), !ApplicationLoader.mainInterfacePaused, hasLiveConsent(), liveServiceRunning);
    }
    public static boolean canStartLiveService() {
        return !isAsgram() || AsgramLocationPolicy.canStartService(hasForegroundConsent(), hasLiveConsent(), !ApplicationLoader.mainInterfacePaused, liveServiceRunning);
    }
    public static void setLiveServiceRunning(boolean running) { liveServiceRunning = running; }
    public static void showIntroduction(Activity activity, Runnable signIn) {
        if (!isAsgram()) { signIn.run(); return; }
        new AlertDialog.Builder(activity)
                .setTitle("Optional location sharing")
                .setMessage(AsgramLocationPolicy.LIVE_DISCLOSURE + "\n\nSigning in does not collect your location. You decide when you use a location feature.")
                // Navigation is never recorded as consent and requests no Android permission.
                .setPositiveButton("Continue without location", (dialog, which) -> signIn.run())
                .setNegativeButton("Back", null).show();
    }
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        boolean location = false;
        for (String permission : permissions) location |= AsgramLocationPolicy.isLocationPermission(permission);
        if (!isAsgram() || !location) { activity.requestPermissions(permissions, requestCode); return; }
        org.telegram.messenger.asgram.AsgramConsentDecision decision = new org.telegram.messenger.asgram.AsgramConsentDecision(() -> {
            preferences().edit().putInt("foreground_consent", AsgramLocationPolicy.CONSENT_VERSION).commit();
            activity.requestPermissions(permissions, requestCode);
        }, () -> {
            int[] denied = new int[permissions.length];
            java.util.Arrays.fill(denied, PackageManager.PERMISSION_DENIED);
            activity.onRequestPermissionsResult(requestCode, permissions, denied);
        });
        for (String permission : permissions) {
            if ("android.permission.ACCESS_BACKGROUND_LOCATION".equals(permission)) { decision.decline(); return; }
        }
        AlertDialog prompt = new AlertDialog.Builder(activity)
                .setTitle("Location in ASGRAM")
                .setMessage(AsgramLocationPolicy.FOREGROUND_DISCLOSURE)
                .setPositiveButton("Allow location", (dialog, which) -> decision.allow())
                .setNegativeButton("Not now", (dialog, which) -> decision.decline()).create();
        prompt.setOnCancelListener(dialog -> decision.decline());
        prompt.show();
    }
    public static void showLiveConsent(Activity activity, Runnable onAllowed) {
        if (!isAsgram()) { onAllowed.run(); return; }
        new AlertDialog.Builder(activity)
                .setTitle("Share live location?")
                .setMessage(AsgramLocationPolicy.LIVE_DISCLOSURE)
                .setPositiveButton("Continue to sharing time", (dialog, which) -> {
                    preferences().edit().putInt("live_consent", AsgramLocationPolicy.CONSENT_VERSION).commit();
                    onAllowed.run();
                })
                .setNegativeButton("Not now", null).show();
    }
}
