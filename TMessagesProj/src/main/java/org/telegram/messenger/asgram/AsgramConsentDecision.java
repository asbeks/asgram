package org.telegram.messenger.asgram;

/** A permission dialog has exactly one explicit outcome, including Back/outside dismissal. */
public final class AsgramConsentDecision {
    private boolean decided;
    private final Runnable onAllowed, onDeclined;
    public AsgramConsentDecision(Runnable onAllowed, Runnable onDeclined) {
        this.onAllowed = onAllowed;
        this.onDeclined = onDeclined;
    }
    public synchronized void allow() {
        if (decided) return;
        decided = true;
        onAllowed.run();
    }
    public synchronized void decline() {
        if (decided) return;
        decided = true;
        onDeclined.run();
    }
}
