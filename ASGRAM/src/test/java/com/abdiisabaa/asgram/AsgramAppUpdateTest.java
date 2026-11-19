package com.abdiisabaa.asgram;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AsgramAppUpdateTest {
    @Test
    public void requiresEveryNewerBuildThatPlayOffers() {
        assertTrue(AsgramAppUpdate.requiresUpdate(14, 15, true));
        assertTrue(AsgramAppUpdate.requiresUpdate(15, 16, true));
        assertTrue(AsgramAppUpdate.requiresUpdate(15, 19, true));
        assertFalse(AsgramAppUpdate.requiresUpdate(15, 16, false));
    }

    @Test
    public void allowsTheCurrentBuildAndIgnoresVersionsThatAreNotNewer() {
        assertFalse(AsgramAppUpdate.requiresUpdate(15, 15, true));
        assertFalse(AsgramAppUpdate.requiresUpdate(15, 14, true));
        assertFalse(AsgramAppUpdate.requiresUpdate(15, 0, false));
    }
}
