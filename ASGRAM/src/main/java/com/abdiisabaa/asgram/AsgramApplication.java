package com.abdiisabaa.asgram;

import org.telegram.messenger.ApplicationLoader;

public final class AsgramApplication extends ApplicationLoader {
    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) registerActivityLifecycleCallbacks(new AsgramAppUpdate(this));
    }

    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
