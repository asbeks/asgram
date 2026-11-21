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

import org.telegram.messenger.asgram.AsgramChannelConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.LaunchActivity;

/** Enforces the newer version that Google Play offers to this device. */
final class AsgramAppUpdate implements Application.ActivityLifecycleCallbacks {
    private static final String PLAY_LISTING = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
    private static final int UPDATE_REQUEST = 8024;
    private static final long CHECK_INTERVAL_MS = 5 * 60_000L;

    private final AppUpdateManager manager;
    private Activity foreground;
    private AlertDialog updateDialog;
    private Activity dialogOwner;
    private boolean checking;
    private int request;
    private int promptedBuild;
    private long lastCheck = -CHECK_INTERVAL_MS;

    AsgramAppUpdate(Application app) {
        manager = AppUpdateManagerFactory.create(app);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity.getClass() != LaunchActivity.class) return;
        foreground = activity;
        if (updateDialog != null || SystemClock.elapsedRealtime() - lastCheck >= CHECK_INTERVAL_MS) {
            AsgramChannelConfig.refresh(activity, settings -> {
                if (foreground != activity || activity.isFinishing()) return;
                if (settings.requireLatestBuild) check(activity, false);
                else dismiss();
            });
        }
    }

    private void check(Activity activity, boolean userRequestedUpdate) {
        if (checking || foreground != activity || activity.isFinishing()) return;
        checking = true;
        int currentRequest = ++request;
        lastCheck = SystemClock.elapsedRealtime();
        manager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (!isCurrent(activity, currentRequest)) return;
            checking = false;
            int availability = info.updateAvailability();
            boolean available = availability == UpdateAvailability.UPDATE_AVAILABLE;
            boolean inProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS;
            if (!requiresUpdate(BuildConfig.VERSION_CODE, info.availableVersionCode(), available || inProgress)) {
                dismiss();
                return;
            }
            showUpdateDialog(activity);
            if (userRequestedUpdate || inProgress || (promptedBuild != info.availableVersionCode()
                    && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))) {
                promptedBuild = info.availableVersionCode();
                startUpdate(activity, info);
            }
        }).addOnFailureListener(error -> {
            if (!isCurrent(activity, currentRequest)) return;
            checking = false;
            if (updateDialog != null) updateDialog.setMessage("Google Play is temporarily unavailable. Connect and check again.");
        });
    }

    static boolean requiresUpdate(int installed, int playBuild, boolean available) {
        return installed > 0 && available && playBuild > installed;
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
