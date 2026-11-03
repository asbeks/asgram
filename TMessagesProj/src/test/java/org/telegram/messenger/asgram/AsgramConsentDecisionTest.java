package org.telegram.messenger.asgram;
import org.junit.Test;
import static org.junit.Assert.*;

public class AsgramConsentDecisionTest {
    @Test public void backOutsideAndNotNowNeverRequestPermissionOrRecordConsent() {
        int[] events = new int[2];
        AsgramConsentDecision choice = new AsgramConsentDecision(() -> events[0]++, () -> events[1]++);
        choice.decline();choice.decline();choice.allow();
        assertEquals(0, events[0]);assertEquals(1, events[1]);
    }
    @Test public void affirmativeChoiceRunsThePermissionStepOnlyOnce() {
        int[] events = new int[2];
        AsgramConsentDecision choice = new AsgramConsentDecision(() -> events[0]++, () -> events[1]++);
        choice.allow();choice.allow();choice.decline();
        assertEquals(1, events[0]);assertEquals(0, events[1]);
    }
    @Test public void simplyDisplayingTheDialogIsNotConsent() {
        int[] events = new int[2];
        new AsgramConsentDecision(() -> events[0]++, () -> events[1]++);
        assertEquals(0, events[0]);assertEquals(0, events[1]);
    }
}
