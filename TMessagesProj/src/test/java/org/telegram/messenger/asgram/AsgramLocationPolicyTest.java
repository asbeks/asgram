package org.telegram.messenger.asgram;
import org.junit.Test;
import static org.junit.Assert.*;

public class AsgramLocationPolicyTest {
    @Test public void aPriorAndroidPermissionIsNotAppConsent() {
        assertFalse(AsgramLocationPolicy.canRead(false, true, false, false));
        assertFalse(AsgramLocationPolicy.canStartService(false, true, true, false));
    }
    @Test public void foregroundLocationRequiresConsentAndVisibility() {
        assertTrue(AsgramLocationPolicy.canRead(true, true, false, false));
        assertFalse(AsgramLocationPolicy.canRead(true, false, false, false));
    }
    @Test public void backgroundReadNeedsConsentedLiveSharingAndItsRunningNotificationService() {
        assertTrue(AsgramLocationPolicy.canRead(true, false, true, true));
        assertFalse(AsgramLocationPolicy.canRead(true, false, false, true));
        assertFalse(AsgramLocationPolicy.canRead(true, false, true, false));
        assertFalse(AsgramLocationPolicy.canRead(false, false, true, true));
    }
    @Test public void aColdBackgroundLaunchCannotRestartTracking() {
        assertFalse(AsgramLocationPolicy.canStartService(true, true, false, false));
        assertTrue(AsgramLocationPolicy.canStartService(true, true, true, false));
        assertTrue(AsgramLocationPolicy.canStartService(true, true, false, true));
    }
    @Test public void stoppingServiceRemovesBackgroundEligibility() {
        boolean allowed = AsgramLocationPolicy.canRead(true, false, true, true);
        assertTrue(allowed);
        allowed = AsgramLocationPolicy.canRead(true, false, true, false);
        assertFalse(allowed);
    }
    @Test public void disclosuresDescribeTheAppDataRecipientsBackgroundScopeAndStopControl() {
        String live = AsgramLocationPolicy.LIVE_DISCLOSURE;
        assertTrue(live.contains("ASGRAM collects location data"));
        assertTrue(live.contains("Telegram"));
        assertTrue(live.contains("chat you choose"));
        assertTrue(live.contains("closed or not in use"));
        assertTrue(live.contains("ongoing location notification"));
        assertTrue(live.contains("Stop it"));
        assertTrue(AsgramLocationPolicy.FOREGROUND_DISCLOSURE.contains("optional"));
    }
}
