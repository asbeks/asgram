package org.telegram.ui.asgram;

import android.content.Context;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.asgram.AsgramCommunityMembership;
import org.telegram.messenger.asgram.AsgramCommunityMembership.Community;
import org.telegram.messenger.asgram.AsgramCommunityMembership.Status;

import java.util.EnumMap;

public final class AsgramCommunityGate extends FrameLayout {
    private static final long RECHECK_AFTER_MS = 5 * 60_000;

    private final EnumMap<Community, TextView> memberStatus = new EnumMap<>(Community.class);
    private final TextView message;
    private final TextView action;
    private final Runnable onAccessChanged;
    private AsgramCommunityMembership membership;
    private int checkedAccount = -1;
    private long checkedUserId;
    private long lastConfirmed;

    public AsgramCommunityGate(Context context, Runnable onAccessChanged) {
        super(context);
        this.onAccessChanged = onAccessChanged;
        AsgramUi.Palette palette = new AsgramUi.Palette(context);
        setBackgroundColor(palette.background);
        setClickable(true);
        setVisibility(GONE);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setPadding(AsgramUi.dp(24), AndroidUtilities.statusBarHeight + AsgramUi.dp(24),
                AsgramUi.dp(24), AndroidUtilities.navigationBarHeight + AsgramUi.dp(24));
        addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout center = new FrameLayout(context);
        scroll.addView(center, new ScrollView.LayoutParams(-1, -2));
        LinearLayout content = AsgramUi.column(context);
        center.addView(content, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));

        AsgramUi.Glyph brand = new AsgramUi.Glyph(context, "ASGRAM", palette.primary);
        content.addView(brand, AsgramUi.lp(56, 56));
        AsgramUi.gap(content, 20);
        content.addView(AsgramUi.text(context, "Join the ASGRAM community", 27, palette.text, true));
        AsgramUi.gap(content, 10);
        content.addView(AsgramUi.text(context,
                "Membership in these Telegram communities is required to use ASGRAM.", 16, palette.secondary, false));
        AsgramUi.gap(content, 20);

        for (Community community : Community.values()) {
            AsgramUi.divider(content, palette);
            LinearLayout row = AsgramUi.column(context);
            row.setPadding(0, AsgramUi.dp(8), 0, AsgramUi.dp(8));
            row.addView(AsgramUi.text(context, community.title, 16, palette.text, true));
            TextView status = AsgramUi.text(context, "Checking…", 13, palette.secondary, false);
            memberStatus.put(community, status);
            row.addView(status);
            content.addView(row, AsgramUi.lp(-1, -2));
        }
        AsgramUi.divider(content, palette);
        AsgramUi.gap(content, 16);

        message = AsgramUi.text(context, "", 14, palette.secondary, false);
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(message, AsgramUi.lp(-1, -2));
        AsgramUi.gap(content, 12);
        action = AsgramUi.button(context, "Checking membership…", palette, true,
                () -> membership.check(membership.canJoin()));
        content.addView(action, AsgramUi.lp(-1, -2));
    }

    public void showForAccount(int account, boolean foreground) {
        UserConfig user = UserConfig.getInstance(account);
        long userId = user.getClientUserId();
        if (!user.isClientActivated() || userId == 0 || user.unacceptedTermsOfService != null) {
            clear();
            return;
        }
        if (account != checkedAccount || userId != checkedUserId) {
            clear();
            checkedAccount = account;
            checkedUserId = userId;
            membership = new AsgramCommunityMembership(account, userId, AsgramUi.prefs(getContext()), this::render);
            membership.check(false);
        } else if (foreground && SystemClock.elapsedRealtime() - lastConfirmed >= RECHECK_AFTER_MS) {
            membership.check(false);
        }
    }

    public boolean blocksAccess() { return getVisibility() == VISIBLE; }

    public void clear() {
        if (membership != null) membership.close();
        membership = null;
        checkedAccount = -1;
        checkedUserId = 0;
        lastConfirmed = 0;
        setVisibility(GONE);
    }

    private void render() {
        if (!membership.isBusy() && membership.hasAccess()) {
            lastConfirmed = SystemClock.elapsedRealtime();
            setVisibility(GONE);
            onAccessChanged.run();
            return;
        }

        boolean pending = false;
        boolean unavailable = false;
        for (Community community : Community.values()) {
            Status state = membership.status(community);
            pending |= state == Status.APPROVAL_PENDING;
            unavailable |= state == Status.UNAVAILABLE;
            memberStatus.get(community).setText(state.label);
        }

        boolean busy = membership.isBusy();
        boolean canJoin = membership.canJoin();
        String label = busy ? "Checking membership…" : canJoin ? "Join all and continue" : "Check again";
        action.setText(label);
        action.setContentDescription(label);
        action.setEnabled(!busy);
        action.setAlpha(busy ? 0.6f : 1f);
        if (busy) message.setText("Checking with Telegram…");
        else if (canJoin) message.setText("One tap joins the communities you’re missing with this Telegram account.");
        else if (pending) message.setText("Check again after your join request is approved.");
        else if (unavailable) message.setText("Check your connection and try again.");
        else message.setText("");
        setVisibility(VISIBLE);
        onAccessChanged.run();
    }
}
