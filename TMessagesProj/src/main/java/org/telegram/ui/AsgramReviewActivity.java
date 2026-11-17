package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.asgram.AsgramLocationPolicy;
import org.telegram.messenger.asgram.AsgramReviewSession;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.asgram.AsgramUi;

/** Offline entry for reviewing ASGRAM with synthetic content on a clean install. */
public final class AsgramReviewActivity extends BaseFragment {
    private final AsgramReviewSession session = new AsgramReviewSession();
    private AsgramUi.Palette palette;
    private LinearLayout content;

    @Override public View createView(Context context) {
        palette = new AsgramUi.Palette(context);
        setHeader(this, "Review demo", palette);
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(palette.background);
        content = paddedColumn(context);
        scroll.addView(content, AsgramUi.lp(-1, -2));
        fragmentView = scroll;

        content.addView(AsgramUi.text(context, "Explore ASGRAM", 30, palette.text, true));
        AsgramUi.gap(content, 10);
        content.addView(AsgramUi.text(context, "Try the app with sample content. Messages, calls and location sharing in this demo stay on this device.", 15, palette.secondary, false));
        AsgramUi.gap(content, 22);
        link("Feed", "Read, filter and save sample stories", "News",
                () -> presentFragment(AsgramHubActivity.forReview(session)));
        link("Chats", "Open a conversation and send a sample message", "Chats",
                () -> presentFragment(new ReviewSpaces(session, false)));
        link("Spaces", "Explore sample groups, channels and bots", "Education",
                () -> presentFragment(new ReviewSpaces(session, true)));
        link("Location and privacy", "Review both location disclosures and sharing controls", "Settings",
                () -> presentFragment(new ReviewPrivacy()));
        AsgramUi.gap(content, 16);
        content.addView(AsgramUi.button(context, "Exit demo", palette, false, this::finishFragment), AsgramUi.lp(-1, -2));
        return scroll;
    }

    private void link(String title, String subtitle, String icon, Runnable action) {
        Context context = content.getContext();
        LinearLayout row = AsgramUi.row(context);
        row.setMinimumHeight(AsgramUi.dp(70));
        row.setBackground(Theme.createSelectorDrawable(palette.tint));
        row.addView(new AsgramUi.Glyph(context, icon, palette.primary), AsgramUi.lp(46, 46));
        LinearLayout labels = AsgramUi.column(context);
        labels.addView(AsgramUi.text(context, title, 17, palette.text, true));
        AsgramUi.gap(labels, 4);
        labels.addView(AsgramUi.text(context, subtitle, 13, palette.secondary, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        row.setOnClickListener(view -> action.run());
        content.addView(row);
        AsgramUi.divider(content, palette);
    }

    private static LinearLayout paddedColumn(Context context) {
        LinearLayout view = AsgramUi.column(context);
        view.setPadding(AsgramUi.dp(20), AsgramUi.dp(20), AsgramUi.dp(20),
                AndroidUtilities.navigationBarHeight + AsgramUi.dp(20));
        return view;
    }

    private static void setHeader(BaseFragment fragment, String title, AsgramUi.Palette palette) {
        ActionBar bar = fragment.getActionBar();
        bar.setTitle(title);
        bar.setSubtitle("Sample content");
        bar.setBackButtonImage(R.drawable.ic_ab_back);
        bar.setTitleColor(palette.text);
        bar.setSubtitleColor(palette.secondary);
        bar.setItemsColor(palette.text, false);
        bar.setBackgroundColor(palette.background);
        bar.setCastShadows(false);
        bar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) fragment.finishFragment();
            }
        });
    }

    private static EditText field(Context context, AsgramUi.Palette palette, String hint, int limit) {
        EditText edit = new EditText(context);
        edit.setHint(hint);
        edit.setContentDescription(hint);
        edit.setTextColor(palette.text);
        edit.setHintTextColor(palette.secondary);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setSingleLine(true);
        edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        edit.setPadding(AsgramUi.dp(12), AsgramUi.dp(10), AsgramUi.dp(12), AsgramUi.dp(10));
        edit.setBackground(AsgramUi.shape(palette.surface, 12));
        return edit;
    }

    private static final class ReviewSpaces extends BaseFragment {
        private final AsgramReviewSession session;
        private final boolean spaces;
        private AsgramUi.Palette palette;
        private LinearLayout results;
        private AsgramReviewSession.Kind filter;

        ReviewSpaces(AsgramReviewSession session, boolean spaces) {
            this.session = session;
            this.spaces = spaces;
        }

        @Override public View createView(Context context) {
            palette = new AsgramUi.Palette(context);
            setHeader(this, spaces ? "Spaces" : "Chats", palette);
            ScrollView scroll = new ScrollView(context);
            scroll.setBackgroundColor(palette.background);
            scroll.setFillViewport(true);
            LinearLayout root = paddedColumn(context);
            root.setFocusableInTouchMode(true);
            scroll.addView(root, AsgramUi.lp(-1, -2));
            fragmentView = scroll;

            EditText search = field(context, palette, "Search sample conversations", 64);
            root.addView(search, AsgramUi.lp(-1, 52));
            AsgramUi.gap(root, 10);
            if (spaces) {
                root.addView(AsgramUi.button(context, "Filter by type", palette, false,
                        () -> filterSpaces(search)), AsgramUi.lp(-1, -2));
                AsgramUi.gap(root, 8);
                root.addView(AsgramUi.button(context, "Create sample space", palette, true,
                        () -> createSpace(context, search)), AsgramUi.lp(-1, -2));
                AsgramUi.gap(root, 10);
            }
            results = AsgramUi.column(context);
            root.addView(results);
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable text) {}
                @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                    updateResults(text.toString());
                }
            });
            updateResults("");
            return scroll;
        }

        private void updateResults(String query) {
            results.removeAllViews();
            Context context = results.getContext();
            for (AsgramReviewSession.Chat chat : session.find(query, filter)) {
                LinearLayout row = AsgramUi.column(context);
                row.setPadding(AsgramUi.dp(12), AsgramUi.dp(13), AsgramUi.dp(12), AsgramUi.dp(13));
                row.setBackground(Theme.createSelectorDrawable(palette.tint));
                row.addView(AsgramUi.text(context, chat.title, 17, palette.text, true));
                AsgramUi.gap(row, 4);
                row.addView(AsgramUi.text(context, chat.kind.label, 13, palette.secondary, false));
                row.setOnClickListener(view -> presentFragment(new ReviewChat(session, chat)));
                results.addView(row);
                AsgramUi.divider(results, palette);
            }
            if (results.getChildCount() == 0) {
                AsgramUi.gap(results, 12);
                results.addView(AsgramUi.text(context, "No matching conversations.", 15, palette.secondary, false));
            }
        }

        private void filterSpaces(EditText search) {
            new AlertDialog.Builder(getParentActivity()).setTitle("Filter by type")
                    .setItems(new CharSequence[]{"All", "Private", "Groups", "Channels", "Bots"}, (dialog, which) -> {
                        filter = which == 0 ? null : AsgramReviewSession.Kind.values()[which - 1];
                        updateResults(search.getText().toString());
                    }).show();
        }

        private void createSpace(Context context, EditText search) {
            new AlertDialog.Builder(getParentActivity()).setTitle("Create sample space")
                    .setItems(new CharSequence[]{"Group", "Channel"}, (dialog, which) -> {
                        EditText name = field(context, palette, "Space name", 64);
                        AlertDialog create = new AlertDialog.Builder(getParentActivity())
                                .setTitle(which == 0 ? "New group" : "New channel")
                                .setView(name).setNegativeButton("Cancel", null)
                                .setPositiveButton("Create", null).create();
                        create.show();
                        create.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
                            AsgramReviewSession.Chat chat = session.create(name.getText().toString(),
                                    which == 0 ? AsgramReviewSession.Kind.GROUP : AsgramReviewSession.Kind.CHANNEL);
                            if (chat == null) {
                                name.setError("Enter a name, or exit and restart after reaching the demo limit.");
                                return;
                            }
                            create.dismiss();
                            filter = null;
                            search.setText("");
                            updateResults("");
                            presentFragment(new ReviewChat(session, chat));
                        });
                    }).show();
        }
    }

    private static final class ReviewChat extends BaseFragment {
        private final AsgramReviewSession session;
        private final AsgramReviewSession.Chat chat;
        private AsgramUi.Palette palette;
        private LinearLayout messages;
        private ScrollView scroll;

        ReviewChat(AsgramReviewSession session, AsgramReviewSession.Chat chat) {
            this.session = session;
            this.chat = chat;
        }

        @Override public View createView(Context context) {
            palette = new AsgramUi.Palette(context);
            setHeader(this, chat.title, palette);
            LinearLayout root = AsgramUi.column(context);
            root.setBackgroundColor(palette.background);
            root.setFocusableInTouchMode(true);
            scroll = new ScrollView(context);
            messages = paddedColumn(context);
            scroll.addView(messages);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            LinearLayout actions = AsgramUi.row(context);
            actions.setPadding(AsgramUi.dp(12), AsgramUi.dp(4), AsgramUi.dp(12), AsgramUi.dp(4));
            actions.addView(AsgramUi.button(context, "Location", palette, false,
                    () -> presentFragment(new ReviewPrivacy())), new LinearLayout.LayoutParams(0, -2, 1));
            actions.addView(AsgramUi.button(context, "Try a call", palette, false, this::openCall),
                    new LinearLayout.LayoutParams(0, -2, 1));
            root.addView(actions);
            LinearLayout composer = AsgramUi.row(context);
            composer.setPadding(AsgramUi.dp(12), AsgramUi.dp(6), AsgramUi.dp(12), AndroidUtilities.navigationBarHeight + AsgramUi.dp(8));
            EditText input = field(context, palette, "Sample message", 1024);
            composer.addView(input, new LinearLayout.LayoutParams(0, AsgramUi.dp(50), 1));
            composer.addView(AsgramUi.button(context, "Send", palette, true, () -> {
                if (!session.send(chat, input.getText().toString())) {
                    input.setError("Enter a message or start a new sample conversation.");
                    return;
                }
                input.setText("");
                renderMessages();
            }));
            root.addView(composer);
            fragmentView = root;
            renderMessages();
            return root;
        }

        private void renderMessages() {
            messages.removeAllViews();
            for (AsgramReviewSession.Message message : chat.messages()) {
                TextView bubble = AsgramUi.text(messages.getContext(), message.body, 16, palette.text, false);
                bubble.setPadding(AsgramUi.dp(13), AsgramUi.dp(10), AsgramUi.dp(13), AsgramUi.dp(10));
                bubble.setBackground(AsgramUi.shape(message.outgoing ? palette.tint : palette.surface, 14));
                bubble.setMaxWidth(AsgramUi.dp(300));
                LinearLayout.LayoutParams params = AsgramUi.lp(-2, -2);
                params.gravity = message.outgoing ? Gravity.END : Gravity.START;
                params.bottomMargin = AsgramUi.dp(8);
                messages.addView(bubble, params);
            }
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        }

        private void openCall() {
            new AlertDialog.Builder(getParentActivity()).setTitle("Try a sample call")
                    .setItems(new CharSequence[]{"Voice call", "Video call"}, (dialog, which) ->
                            new AlertDialog.Builder(getParentActivity())
                                    .setTitle(which == 0 ? "Sample voice call" : "Sample video call")
                                    .setMessage("Call screen preview for " + chat.title + ". No microphone, camera or network connection is used.")
                                    .setPositiveButton("End call", null).show()).show();
        }
    }

    private static final class ReviewPrivacy extends BaseFragment {
        @Override public View createView(Context context) {
            AsgramUi.Palette colors = new AsgramUi.Palette(context);
            setHeader(this, "Location and privacy", colors);
            ScrollView scroll = new ScrollView(context);
            scroll.setBackgroundColor(colors.background);
            LinearLayout root = paddedColumn(context);
            scroll.addView(root);
            fragmentView = scroll;
            root.addView(AsgramUi.text(context, "Location is optional", 24, colors.text, true));
            AsgramUi.gap(root, 10);
            root.addView(AsgramUi.text(context, "Review the disclosures used in the app. This demo uses a sample location and leaves device permissions alone.", 15, colors.secondary, false));
            AsgramUi.gap(root, 18);
            root.addView(AsgramUi.button(context, "Share a location pin", colors, false, () ->
                    new AlertDialog.Builder(getParentActivity()).setTitle("Location in ASGRAM")
                            .setMessage(AsgramLocationPolicy.FOREGROUND_DISCLOSURE)
                            .setNegativeButton("Not now", null)
                            .setPositiveButton("Continue with a sample pin", (dialog, which) -> showLocation(false)).show()), AsgramUi.lp(-1, -2));
            AsgramUi.gap(root, 10);
            root.addView(AsgramUi.button(context, "Share live location", colors, false, () ->
                    new AlertDialog.Builder(getParentActivity()).setTitle("Share live location?")
                            .setMessage(AsgramLocationPolicy.LIVE_DISCLOSURE)
                            .setNegativeButton("Not now", null)
                            .setPositiveButton("Continue to sharing time", (dialog, which) ->
                                    new AlertDialog.Builder(getParentActivity()).setTitle("Sample sharing time")
                                            .setItems(new CharSequence[]{"15 minutes", "1 hour"}, (ignored, index) -> showLocation(true))
                                            .setNegativeButton("Cancel", null).show()).show()), AsgramUi.lp(-1, -2));
            return scroll;
        }

        private void showLocation(boolean live) {
            new AlertDialog.Builder(getParentActivity())
                    .setTitle(live ? "Sample live location" : "Sample location pin")
                    .setMessage(live ? "In a live chat, sharing shows an ongoing notification with a stop control."
                            : "Sample place: City Library. A map pin uses your location only if you choose to share it.")
                    .setPositiveButton(live ? "Stop sharing" : "Done", null).show();
        }
    }
}
