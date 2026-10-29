package com.abdiisabaa.asgram;
import android.os.Bundle;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.AsgramHubActivity;
import org.telegram.ui.LaunchActivity;
/** Debug-only entry. The feed uses English fixtures and never loads account content. */
public final class StorePreviewActivity extends LaunchActivity {
 @Override protected void onCreate(Bundle state) {
  super.onCreate(null);
  AndroidUtilities.runOnUIThread(()->presentFragment(new AsgramHubActivity(true)),350);
 }
}
