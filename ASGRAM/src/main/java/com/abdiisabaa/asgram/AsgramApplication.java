package com.abdiisabaa.asgram;
import org.telegram.messenger.ApplicationLoader;
public final class AsgramApplication extends ApplicationLoader {
 @Override protected String onGetApplicationId() { return BuildConfig.APPLICATION_ID; }
}
