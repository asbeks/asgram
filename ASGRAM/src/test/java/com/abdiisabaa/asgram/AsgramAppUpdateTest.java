package com.abdiisabaa.asgram;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AsgramAppUpdateTest {
    @Test
    public void requiresUpdateOnlyWhenPlayOffersTheMinimum() {
        assertTrue(AsgramAppUpdate.requiresUpdate(14, 15, 15, true));
        assertTrue(AsgramAppUpdate.requiresUpdate(14, 15, 16, true));
        assertFalse(AsgramAppUpdate.requiresUpdate(14, 15, 14, true));
        assertFalse(AsgramAppUpdate.requiresUpdate(14, 15, 15, false));
    }

    @Test
    public void allowsSupportedBuildsAndDisabledPolicy() {
        assertFalse(AsgramAppUpdate.requiresUpdate(15, 15, 15, true));
        assertFalse(AsgramAppUpdate.requiresUpdate(14, 0, 15, true));
    }
}
