package com.abdiisabaa.asgram;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.json.JSONObject;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.LaunchActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/** Checks ASGRAM's minimum build against the update available for this Play account. */
final class AsgramAppUpdate implements Application.ActivityLifecycleCallbacks {
    private static final String RELEASE_POLICY = "https://asbeks.github.io/asgram/android-release.json";
    private static final String PLAY_LISTING = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
    private static final int UPDATE_REQUEST = 8024;
    private static final long CHECK_INTERVAL_MS = 5 * 60_000L;

    private final AppUpdateManager manager;
    private Activity foreground;
    private AlertDialog updateDialog;
    private Activity dialogOwner;
    private boolean checking;
    private int request;
    private long lastCheck = -CHECK_INTERVAL_MS;

    AsgramAppUpdate(Application app) {
        manager = AppUpdateManagerFactory.create(app);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity.getClass() != LaunchActivity.class) return;
        foreground = activity;
        if (updateDialog != null || SystemClock.elapsedRealtime() - lastCheck >= CHECK_INTERVAL_MS) {
            check(activity, false);
        }
    }

    private void check(Activity activity, boolean userRequestedUpdate) {
        if (checking || foreground != activity || activity.isFinishing()) return;
        checking = true;
        int currentRequest = ++request;
        lastCheck = SystemClock.elapsedRealtime();
        new Thread(() -> {
            int minimumBuild = fetchMinimumBuild();
            activity.runOnUiThread(() -> {
                if (!isCurrent(activity, currentRequest)) return;
                if (minimumBuild <= BuildConfig.VERSION_CODE) {
                    checking = false;
                    dismiss();
                    return;
                }
                manager.getAppUpdateInfo().addOnSuccessListener(info -> {
                    if (!isCurrent(activity, currentRequest)) return;
                    checking = false;
                    int availability = info.updateAvailability();
                    boolean available = availability == UpdateAvailability.UPDATE_AVAILABLE;
                    boolean inProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS;
                    if (!requiresUpdate(BuildConfig.VERSION_CODE, minimumBuild, info.availableVersionCode(),
                            available || inProgress)) {
                        dismiss();
                        return;
                    }
                    showUpdateDialog(activity);
                    if (userRequestedUpdate || inProgress) startUpdate(activity, info);
                }).addOnFailureListener(error -> {
                    if (!isCurrent(activity, currentRequest)) return;
                    checking = false;
                    dismiss();
                });
            });
        }, "asgram-release-policy").start();
    }

    static boolean requiresUpdate(int installed, int minimum, int playBuild, boolean available) {
        return installed > 0 && installed < minimum && available && playBuild >= minimum;
    }

    private static int fetchMinimumBuild() {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(RELEASE_POLICY).openConnection();
            connection.setConnectTimeout(4_000);
            connection.setReadTimeout(4_000);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) return 0;
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] buffer = new byte[512];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (body.size() + count > 4096) return 0;
                    body.write(buffer, 0, count);
                }
                JSONObject policy = new JSONObject(new String(body.toByteArray(), StandardCharsets.UTF_8));
                Object minimum = policy.opt("minimumBuild");
                Object latest = policy.opt("latestBuild");
                if (!BuildConfig.APPLICATION_ID.equals(policy.optString("packageName"))
                        || !(minimum instanceof Integer) || !(latest instanceof Integer)) return 0;
                int floor = (Integer) minimum;
                return floor > 0 && floor <= (Integer) latest ? floor : 0;
            }
        } catch (IOException | org.json.JSONException ignored) {
            return 0;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private boolean isCurrent(Activity activity, int expectedRequest) {
        return foreground == activity && request == expectedRequest && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void showUpdateDialog(Activity activity) {
        if (dialogOwner == activity) return;
        dismiss();
        dialogOwner = activity;
        updateDialog = new AlertDialog.Builder(activity)
                .setTitle("Update ASGRAM")
                .setMessage("Install the latest version from Google Play to continue.")
                .setPositiveButton("Update", (dialog, button) -> check(activity, true))
                .setNegativeButton("Check again", (dialog, button) -> check(activity, false))
                .create();
        updateDialog.setCancelable(false);
        updateDialog.setCanceledOnTouchOutside(false);
        updateDialog.setDismissDialogByButtons(false);
        updateDialog.show();
    }

    private void startUpdate(Activity activity, AppUpdateInfo info) {
        try {
            if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    && manager.startUpdateFlowForResult(info, activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(), UPDATE_REQUEST)) {
                return;
            }
        } catch (SendIntentException ignored) {
            // The same app listing is available even if Play cannot start its in-app flow.
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_LISTING))
                    .setPackage("com.android.vending"));
        } catch (ActivityNotFoundException ignored) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_LISTING)));
            } catch (ActivityNotFoundException unavailable) {
                updateDialog.setMessage("Open Google Play and update ASGRAM, then check again.");
            }
        }
    }

    private void dismiss() {
        if (updateDialog == null) return;
        updateDialog.dismiss();
        updateDialog = null;
        dialogOwner = null;
    }

    @Override public void onActivityPaused(Activity activity) {
        if (foreground == activity) {
            foreground = null;
            if (checking) lastCheck = -CHECK_INTERVAL_MS;
            checking = false;
            ++request;
        }
    }

    @Override public void onActivityDestroyed(Activity activity) {
        if (dialogOwner == activity) dismiss();
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
}
