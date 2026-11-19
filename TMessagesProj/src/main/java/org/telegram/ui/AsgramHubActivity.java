/*
 * THESIS: A native social timeline makes public channel posts the first visible content.
 * OWN-WORLD: Native Telegram theme tokens, system type, real publisher imagery.
 * STORY: Choose Latest or For you, read a post, expand its media, save or share.
 * FIRST VIEWPORT: Compact ASGRAM bar, sort tabs, topic chips, publisher and a full-width post.
 * FORM: Social post timeline, comp A, user delegated selection on November 18. Five native roots
 * preserve the client's existing chat/contact/settings/profile access alongside Feed.
 * FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
 */
package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.asgram.AsgramNativeFeed;
import org.telegram.messenger.asgram.AsgramDirectory;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.asgram.AsgramFeed;
import org.telegram.messenger.asgram.AsgramFeedStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.asgram.AsgramUi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AsgramHubActivity extends BaseFragment implements MainTabsActivity.TabFragmentDelegate {
    // The debug-only screenshot activity supplies synthetic content before any capture.
    private boolean storePreview;
    private boolean sampleContent() { return storePreview; }
    public AsgramHubActivity() {}
    public AsgramHubActivity(boolean preview) { storePreview=org.telegram.messenger.BuildConfig.DEBUG_VERSION && preview; }
    private List<AsgramFeed.Post> posts=new ArrayList<>(),visible=new ArrayList<>();
    private String category="All",query="";
    private boolean latest=true;
    private boolean savedOnly,loading,destroyed,complete=true;
    private AsgramUi.Palette palette;
    private RecyclerView recycler;
    private LinearLayoutManager layoutManager;
    private FeedAdapter adapter;
    private int request;

    @Override public View createView(Context context) {
        destroyed=false;
        palette=new AsgramUi.Palette(context);
        actionBar.setTitle("ASGRAM");
        actionBar.setSubtitle(null);
        actionBar.setBackgroundColor(palette.background);
        actionBar.setTitleColor(palette.text);
        actionBar.setSubtitleColor(palette.secondary);
        actionBar.setItemsColor(palette.text,false);
        actionBar.setCastShadows(false);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (storePreview && id >= 4) return;
                if(id==2) {savedOnly=!savedOnly;applyFilter(true);}
                else if(id==4)showChannels();
                else if(id==5)refresh();
                else if(id==6)presentFragment(new AsgramSettingsActivity());
            }
        });
        actionBar.createMenu().addItem(1,R.drawable.outline_header_search).setIsSearchField(true)
            .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                @Override public void onTextChanged(EditText field) {query=field.getText().toString();applyFilter(false);}
            }).setSearchFieldHint("Search posts");
        actionBar.createMenu().addItem(2,R.drawable.msg_saved).setContentDescription("Show saved stories");
        ActionBarMenuItem more = actionBar.createMenu().addItem(3, R.drawable.ic_ab_other);
        more.setContentDescription("Feed options");
        more.addSubItem(4, R.drawable.msg_channel, "Channels");
        more.addSubItem(5, R.drawable.msg_retry, "Refresh");
        more.addSubItem(6, R.drawable.msg_settings, "Feed settings");
        FrameLayout root=new FrameLayout(context);
        root.setBackgroundColor(palette.background);
        root.setPadding(0,AndroidUtilities.statusBarHeight+ActionBar.getCurrentActionBarHeight(),0,0);
        fragmentView=root;
        recycler=new RecyclerView(context);
        layoutManager=new LinearLayoutManager(context);
        recycler.setLayoutManager(layoutManager);
        recycler.setClipToPadding(false);
        recycler.setPadding(0,0,0,AndroidUtilities.navigationBarHeight+AsgramUi.dp(104));
        recycler.setItemAnimator(null);
        adapter=new FeedAdapter();recycler.setAdapter(adapter);
        root.addView(recycler,LayoutHelper.createFrame(-1,-1));
        if(sampleContent()) {
            posts.clear();
            posts.add(new AsgramFeed.Post("asgram_demo/1","News","asgram_demo","Daily Discoveries","Walking the mountain trail\nPhotos from the morning hike.","sample://landscape",java.time.OffsetDateTime.now().toString()));
            posts.add(new AsgramFeed.Post("asgram_demo/2","Education","asgram_demo","Learning Circle","New study group\nThe group meets on Friday at the library.","",java.time.OffsetDateTime.now().minusMinutes(12).toString()));
            posts.add(new AsgramFeed.Post("asgram_demo/3","Sports","asgram_demo","The Sports Desk","Weekend fixtures\nThe match schedule is out.","",java.time.OffsetDateTime.now().minusMinutes(24).toString()));
            latest=true;applyFilter(false);
        } else { posts=AsgramFeedStore.cached(context);latest=AsgramUi.prefs(context).getBoolean("latest_first",true);applyFilter(false);refresh(); }
        return root;
    }
    private void applyFilter(boolean toTop) {
        if(recycler==null||destroyed)return;
        List<AsgramFeed.Post> source = savedOnly ? savedWithLiveMedia(recycler.getContext()) : posts;
        visible=new ArrayList<>();String term=query.trim().toLowerCase(Locale.ROOT);
        for(AsgramFeed.Post post:source) {
            if(!savedOnly&&!category.equals("All")&&!category.equals(post.category))continue;
            if(!term.isEmpty()&&!(post.text+" "+post.author).toLowerCase(Locale.ROOT).contains(term))continue;
            visible.add(post);
        }
        if(!savedOnly) {
            visible.sort((a,b)->Long.compare(b.timestamp(),a.timestamp()));
            if(!latest&&term.isEmpty())visible=AsgramFeed.featuredFirst(visible);
        }
        actionBar.setTitle(savedOnly ? "Saved posts" : "ASGRAM");
        actionBar.createMenu().getItem(2).setContentDescription(savedOnly ? "Back to feed" : "Saved posts");
        adapter.notifyDataSetChanged();
        if(toTop)layoutManager.scrollToPositionWithOffset(0,0);
    }
    private List<AsgramFeed.Post> savedWithLiveMedia(Context context) {
        List<AsgramFeed.Post> saved = AsgramFeedStore.saved(context);
        for (int i = 0; i < saved.size(); i++) {
            AsgramFeed.Post item = saved.get(i);
            for (AsgramFeed.Post live : posts) {
                if (item.id.equals(live.id)) {
                    saved.set(i, live);
                    break;
                }
            }
        }
        return saved;
    }
    private LinearLayout padded(Context c,int top,int bottom) {
        LinearLayout column=AsgramUi.column(c);
        column.setPadding(AsgramUi.dp(20),AsgramUi.dp(top),AsgramUi.dp(20),AsgramUi.dp(bottom));
        return column;
    }
    private View header(Context c) {
        LinearLayout box = padded(c, 0, 8);
        if (savedOnly) {
            TextView back = AsgramUi.text(c, "All posts", 14, palette.primary, true);
            back.setMinHeight(AsgramUi.dp(48));
            back.setGravity(Gravity.CENTER_VERTICAL);
            back.setOnClickListener(v -> { savedOnly = false; applyFilter(true); });
            box.addView(back, AsgramUi.lp(-1, 48));
            return box;
        }
        LinearLayout sort = AsgramUi.row(c);
        for (String label : new String[]{"Latest", "For you"}) {
            final boolean chronological = label.equals("Latest");
            final boolean selected = latest == chronological;
            LinearLayout tab = AsgramUi.column(c);
            tab.setBackground(Theme.createSelectorDrawable(palette.tint));
            TextView text = AsgramUi.text(c, label, 15, selected ? palette.primary : palette.secondary, selected);
            text.setGravity(Gravity.CENTER);
            tab.addView(text, AsgramUi.lp(-1, 45));
            View underline = new View(c);
            underline.setBackgroundColor(selected ? palette.primary : Color.TRANSPARENT);
            tab.addView(underline, AsgramUi.lp(-1, 3));
            tab.setSelected(selected);
            tab.setContentDescription(label + " posts");
            tab.setOnClickListener(v -> {
                latest = chronological;
                AsgramUi.prefs(c).edit().putBoolean("latest_first", latest).apply();
                applyFilter(true);
            });
            sort.addView(tab, new LinearLayout.LayoutParams(0, AsgramUi.dp(48), 1));
        }
        box.addView(sort, AsgramUi.lp(-1, 48));
        AsgramUi.gap(box, 8);
        HorizontalScrollView horizontal = new HorizontalScrollView(c);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = AsgramUi.row(c);
        for (String value : new String[]{"All", "News", "Education", "Sports"}) {
            boolean selected = category.equals(value);
            TextView chip = AsgramUi.text(c, value, 13, selected ? palette.inverse : palette.text, selected);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(AsgramUi.dp(16), 0, AsgramUi.dp(16), 0);
            chip.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AsgramUi.dp(24), selected ? palette.primary : palette.tint, palette.outline));
            chip.setDuplicateParentStateEnabled(true);
            chip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout target = new FrameLayout(c);
            target.setSelected(selected);
            target.setContentDescription(value + " posts");
            target.setOnClickListener(v -> { category = value; applyFilter(true); });
            target.addView(chip, LayoutHelper.createFrame(-1, 36, Gravity.CENTER));
            LinearLayout.LayoutParams lp = AsgramUi.lp(-2, 48);
            lp.rightMargin = AsgramUi.dp(8);
            filters.addView(target, lp);
        }
        horizontal.addView(filters);
        box.addView(horizontal, AsgramUi.lp(-1, 48));
        return box;
    }

    private void showChannels() {
        if (getParentActivity() == null) return;
        List<String> labels = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        labels.add("Abdii Sabaa"); urls.add("https://t.me/ABDII_SABAA");
        labels.add("University"); urls.add("https://t.me/abdii_sabaa_university");
        labels.add("Sports"); urls.add("https://t.me/+7P24H-U3sWdlNzJk");
        labels.add("Community"); urls.add("https://t.me/Garee_Abdii_Sabaa");
        for (AsgramDirectory.Entry entry : AsgramNativeFeed.entries(getParentActivity())) {
            String url = "https://t.me/" + entry.username;
            if (!urls.contains(url)) { labels.add(entry.title); urls.add(url); }
        }
        new AlertDialog.Builder(getParentActivity()).setTitle("Channels")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> open(urls.get(which))).show();
    }

    private View syncStatus(Context c) {
        if (sampleContent() || savedOnly || (loading && visible.isEmpty()) || (!loading && complete)) return new View(c);
        LinearLayout box = padded(c, 0, 8);
        TextView status = AsgramUi.text(c, loading ? "Updating…" : "Some channels couldn't refresh. Tap to retry.", 12, palette.secondary, false);
        status.setMinHeight(AsgramUi.dp(48));
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setOnClickListener(v -> refresh());
        box.addView(status);
        return box;
    }

    private View empty(Context c) {
        LinearLayout box = padded(c, 28, 28);
        if (loading && !savedOnly) {
            box.addView(AsgramUi.text(c, "Loading posts…", 15, palette.secondary, false));
            return box;
        }
        String title = savedOnly ? "No saved posts" : query.isEmpty() ? "No posts yet" : "Nothing found";
        box.addView(AsgramUi.text(c, title, 22, palette.text, true));
        AsgramUi.gap(box, 10);
        String copy = savedOnly ? "Tap Save on a post to keep it here." : "Try another topic or refresh your feed.";
        box.addView(AsgramUi.text(c, copy, 15, palette.secondary, false));
        AsgramUi.gap(box, 20);
        box.addView(AsgramUi.button(c, savedOnly ? "Back to feed" : "Refresh", palette, true, () -> {
            if (savedOnly) { savedOnly = false; applyFilter(true); } else refresh();
        }), AsgramUi.lp(-1, -2));
        if (category.equals("Sports") && !savedOnly) {
            AsgramUi.gap(box, 8);
            box.addView(AsgramUi.button(c, "Open sports channel", palette, false,
                    () -> open("https://t.me/+7P24H-U3sWdlNzJk")), AsgramUi.lp(-1, -2));
        }
        return box;
    }

    private View story(Context c, AsgramFeed.Post post) {
        LinearLayout card = padded(c, 12, 0);
        LinearLayout identity = AsgramUi.row(c);
        BackupImageView avatar = new BackupImageView(c);
        avatar.setRoundRadius(AsgramUi.dp(21));
        avatar.getImageReceiver().setCurrentAccount(currentAccount);
        TLObject peer = storePreview ? null : getMessagesController().getUserOrChat(post.channel);
        if (peer instanceof TLRPC.Chat) {
            TLRPC.Chat chat = (TLRPC.Chat) peer;
            avatar.setForUserOrChat(chat, new AvatarDrawable(chat));
        } else {
            AvatarDrawable placeholder = new AvatarDrawable();
            placeholder.setInfo(0, post.author, null);
            avatar.setImageDrawable(placeholder);
        }
        identity.addView(avatar, AsgramUi.lp(42, 42));
        LinearLayout byline = AsgramUi.column(c);
        TextView author = AsgramUi.text(c, post.author, 15, palette.text, true);
        byline.addView(author);
        AsgramUi.gap(byline, 4);
        byline.addView(AsgramUi.text(c, relative(post), 12, palette.secondary, false));
        LinearLayout.LayoutParams authorLp = new LinearLayout.LayoutParams(0, -2, 1);
        authorLp.leftMargin = AsgramUi.dp(10);
        identity.addView(byline, authorLp);
        identity.setMinimumHeight(AsgramUi.dp(48));
        identity.setContentDescription("Open " + post.author + " channel");
        identity.setOnClickListener(v -> { if (!storePreview) open("https://t.me/" + post.channel); });
        card.addView(identity, AsgramUi.lp(-1, -2));
        AsgramUi.gap(card, 12);
        TextView text = AsgramUi.text(c, post.text, 16, palette.text, false);
        int firstLine = post.text.indexOf('\n');
        if (firstLine > 0 && firstLine <= 90) {
            SpannableString preview = new SpannableString(post.text);
            preview.setSpan(new StyleSpan(Typeface.BOLD), 0, firstLine, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setText(preview);
        }
        text.setMaxLines(5);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setOnClickListener(v -> read(post));
        card.addView(text, AsgramUi.lp(-1, -2));
        View media = postMedia(c, this, post, 200, sampleContent() ? () -> read(post) : null);
        if (media != null) {
            AsgramUi.gap(card, 12);
            card.addView(media, AsgramUi.lp(-1, -2));
        }
        AsgramUi.gap(card, 4);
        LinearLayout actions = AsgramUi.row(c);
        TextView read = postAction(c, post.hasVideo ? "Watch" : "Open",
                post.hasVideo ? R.drawable.msg_round_play_m : R.drawable.msg_openin,
                () -> { if (post.hasVideo && !sampleContent()) watch(this, post); else read(post); });
        actions.addView(read, new LinearLayout.LayoutParams(0, AsgramUi.dp(48), 1));
        boolean saved = AsgramFeedStore.isSaved(c, post.id);
        TextView save = postAction(c, saved ? "Saved" : "Save", R.drawable.msg_saved, () -> {
            AsgramFeedStore.toggleSave(c, post);
            applyFilter(false);
        });
        save.setContentDescription((saved ? "Unsave " : "Save ") + post.title());
        actions.addView(save, new LinearLayout.LayoutParams(0, AsgramUi.dp(48), 1));
        TextView share = postAction(c, "Share", R.drawable.msg_share, () -> share(post));
        share.setContentDescription("Share " + post.title());
        actions.addView(share, new LinearLayout.LayoutParams(0, AsgramUi.dp(48), 1));
        card.addView(actions, AsgramUi.lp(-1, 48));
        AsgramUi.gap(card, 8);
        AsgramUi.divider(card, palette);
        return card;
    }

    private TextView postAction(Context c, String label, int iconResource, Runnable action) {
        TextView button = AsgramUi.text(c, label, 13, palette.secondary, true);
        button.setGravity(Gravity.CENTER);
        // The icon and label form a single centered button.
        Drawable icon = c.getDrawable(iconResource).mutate();
        icon.setTint(palette.secondary);
        icon.setBounds(0, 0, AsgramUi.dp(17), AsgramUi.dp(17));
        SpannableString content = new SpannableString("  " + label);
        content.setSpan(new android.text.style.ImageSpan(icon, android.text.style.DynamicDrawableSpan.ALIGN_BASELINE),
                0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        button.setText(content);
        button.setContentDescription(label);
        button.setBackground(Theme.createSelectorDrawable(palette.tint));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private static void watch(BaseFragment fragment, AsgramFeed.Post post) {
        List<TLRPC.Message> nativeMedia = post.nativeMedia();
        for (int i = 0; i < nativeMedia.size(); i++) {
            if (MessageObject.isVideoDocument(nativeMedia.get(i).media.document)) {
                openMedia(fragment, post, nativeMedia, i);
                return;
            }
        }
        if (fragment.getParentActivity() != null) Browser.openUrl(fragment.getParentActivity(), post.url);
    }

    private static void openMedia(BaseFragment fragment, AsgramFeed.Post post, List<TLRPC.Message> nativeMedia, int index) {
        if (fragment.getParentActivity() == null) return;
        if (nativeMedia.isEmpty()) {
            Browser.openUrl(fragment.getParentActivity(), post.url);
            return;
        }
        ArrayList<MessageObject> items = new ArrayList<>();
        for (TLRPC.Message message : nativeMedia) {
            items.add(new MessageObject(post.nativeAccount, message, false, true));
        }
        PhotoViewer.getInstance().setParentActivity(fragment);
        PhotoViewer.getInstance().openPhoto(items, index, 0, 0, 0, new PhotoViewer.EmptyPhotoViewerProvider());
    }
    private void read(AsgramFeed.Post post) {AsgramArticleActivity reader=new AsgramArticleActivity(post);reader.setCurrentAccount(currentAccount);presentFragment(reader);}
    private static View postMedia(Context context, BaseFragment fragment, AsgramFeed.Post post, int fullHeight, Runnable previewClick) {
        List<TLRPC.Message> nativeMedia = post.nativeMedia();
        int count = nativeMedia.isEmpty() ? post.imageUrls.size() : nativeMedia.size();
        if (count == 0) return null;

        AsgramUi.Palette palette = new AsgramUi.Palette(context);
        LinearLayout gallery = AsgramUi.column(context);
        Runnable showGallery = () -> {
            gallery.removeAllViews();
            for (int start = 0; start < count;) {
                int columns = start == 0 && count % 2 != 0 ? 1 : 2;
                if (start > 0) AsgramUi.gap(gallery, 8);
                LinearLayout row = AsgramUi.row(context);
                for (int column = 0; column < columns; column++) {
                    int index = start + column;
                    TLRPC.Message message = nativeMedia.isEmpty() ? null : nativeMedia.get(index);
                    String url = message == null ? post.imageUrls.get(index) : "";
                    FrameLayout image = mediaTile(context, palette, post, message, url, columns == 1);
                    String kind = message != null && message.media.photo == null ? "Video" : "Photo";
                    image.setContentDescription(count == 1 ? kind + " from " + post.author : kind + " " + (index + 1) + " of " + count + " from " + post.author);
                    image.setOnClickListener(v -> {
                        if (previewClick != null) previewClick.run();
                        else if (!post.channel.equals("asgram_demo")) openMedia(fragment, post, nativeMedia, index);
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
                    if (column > 0) params.setMarginStart(AsgramUi.dp(8));
                    row.addView(image, params);
                }
                gallery.addView(row, AsgramUi.lp(-1, columns == 1 ? fullHeight : 116));
                start += columns;
            }
        };
        if (previewClick != null || AsgramFeedStore.canLoadPhotos(context)) {
            showGallery.run();
        } else {
            String label = count > 1 ? "Load " + count + " previews" : post.hasVideo ? "Load video preview" : "Load photo";
            gallery.addView(AsgramUi.button(context, label, palette, false, showGallery), AsgramUi.lp(-1, 48));
        }
        return gallery;
    }

    private static FrameLayout mediaTile(Context context, AsgramUi.Palette palette, AsgramFeed.Post post,
                                         TLRPC.Message message, String url, boolean fullWidth) {
        FrameLayout frame = new FrameLayout(context);
        frame.setBackground(AsgramUi.shape(palette.tint, 12));
        frame.setClipToOutline(true);
        if (org.telegram.messenger.BuildConfig.DEBUG_VERSION && url.equals("sample://landscape")) {
            ImageView sample = new ImageView(context);
            sample.setImageResource(R.drawable.asgram_store_landscape);
            sample.setScaleType(ImageView.ScaleType.CENTER_CROP);
            frame.addView(sample, LayoutHelper.createFrame(-1, -1));
            return frame;
        }

        BackupImageView image = new BackupImageView(context);
        image.setAspectFit(false);
        boolean video = message != null && message.media.photo == null;
        int previewSize = fullWidth ? 800 : 480;
        ImageLocation location;
        if (message == null) {
            location = ImageLocation.getForPath(url);
        } else {
            image.getImageReceiver().setCurrentAccount(post.nativeAccount);
            if (video) {
                TLRPC.Document document = message.media.document;
                location = ImageLocation.getForDocument(FileLoader.getClosestPhotoSizeWithSize(document.thumbs, previewSize), document);
            } else {
                TLRPC.Photo photo = message.media.photo;
                location = ImageLocation.getForPhoto(FileLoader.getClosestPhotoSizeWithSize(photo.sizes, previewSize), photo);
            }
        }
        image.setImage(location, fullWidth ? "800_500" : "480_300", (Drawable) null, message == null ? post : message);
        frame.addView(image, LayoutHelper.createFrame(-1, -1));
        if (video) {
            ImageView play = new ImageView(context);
            play.setImageResource(R.drawable.msg_round_play_m);
            play.setColorFilter(Color.WHITE);
            play.setPadding(AsgramUi.dp(8), AsgramUi.dp(8), AsgramUi.dp(8), AsgramUi.dp(8));
            play.setBackground(AsgramUi.shape(0xb3000000, 20));
            frame.addView(play, LayoutHelper.createFrame(40, 40, Gravity.CENTER));
        }
        return frame;
    }
    private static String relative(AsgramFeed.Post post) {
        return post.timestamp()<=0?"Telegram":System.currentTimeMillis()-post.timestamp()<60_000?"Just now":DateUtils.getRelativeTimeSpanString(post.timestamp(),System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS,DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    }
    private void open(String url) {if(getParentActivity()!=null)Browser.openUrl(getParentActivity(),url);}
    private void share(AsgramFeed.Post post) {
        if (storePreview || getParentActivity() == null) return;
        Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TEXT,post.title()+"\n"+post.url);
        getParentActivity().startActivity(Intent.createChooser(intent,"Share story"));
    }
    private void refresh() {
        if(sampleContent())return;
        if(loading||fragmentView==null||destroyed)return;loading=true;applyFilter(false);final int generation=++request;
        final Context c=fragmentView.getContext().getApplicationContext();
        AsgramNativeFeed.refresh(c,currentAccount,classGuid,(result,all)->{
            if(destroyed||generation!=request)return;posts=result;complete=all;loading=false;applyFilter(false);
        });
    }
    @Override public void onResume() {
        super.onResume();
        if(fragmentView!=null) {
            AsgramUi.Palette next=new AsgramUi.Palette(fragmentView.getContext());
            if(palette.dark!=next.dark||palette.background!=next.background||palette.primary!=next.primary||palette.text!=next.text){palette=next;fragmentView.setBackgroundColor(palette.background);actionBar.setBackgroundColor(palette.background);actionBar.setTitleColor(palette.text);actionBar.setSubtitleColor(palette.secondary);actionBar.setItemsColor(palette.text,false);}
            applyFilter(false);
        }
    }
    @Override public void onFragmentDestroy(){destroyed=true;request++;if(!sampleContent())ConnectionsManager.getInstance(currentAccount).cancelRequestsForGuid(classGuid);super.onFragmentDestroy();}
    @Override public boolean isLightStatusBar(){return palette==null||!palette.dark;}
    @Override public boolean canParentTabsSlide(MotionEvent e,boolean forward){return false;}
    @Override public void onParentScrollToTop(){if(recycler!=null)recycler.smoothScrollToPosition(0);}
    private final class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemCount(){return 2+Math.max(1,visible.size());}
        @Override public int getItemViewType(int position){return position<2?position:2;}
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){
            FrameLayout frame=new FrameLayout(parent.getContext());frame.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return new RecyclerView.ViewHolder(frame){};
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,int position){
            FrameLayout frame=(FrameLayout)holder.itemView;frame.removeAllViews();Context c=frame.getContext();
            View child=position==0?header(c):position==1?syncStatus(c):visible.isEmpty()?empty(c):story(c,visible.get(position-2));
            frame.addView(child,LayoutHelper.createFrame(-1,-2));
        }
    }

    public static final class AsgramArticleActivity extends BaseFragment {
        private final AsgramFeed.Post post;
        AsgramArticleActivity(AsgramFeed.Post post) {
            this.post=post;
        }
        @Override public View createView(Context c) {
            AsgramUi.Palette p=new AsgramUi.Palette(c);actionBar.setTitle(post.category);actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setBackgroundColor(p.background);actionBar.setTitleColor(p.text);actionBar.setItemsColor(p.text,false);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
            ScrollView scroll=new ScrollView(c);scroll.setBackgroundColor(p.background);scroll.setFillViewport(true);
            LinearLayout copy=AsgramUi.column(c);copy.setPadding(AsgramUi.dp(22),AsgramUi.dp(22),AsgramUi.dp(22),AndroidUtilities.navigationBarHeight+AsgramUi.dp(24));
            scroll.addView(copy,AsgramUi.lp(-1,-2));fragmentView=scroll;
            copy.addView(AsgramUi.text(c,post.author+" · "+relative(post),13,p.primary,true));AsgramUi.gap(copy,16);
            copy.addView(AsgramUi.text(c,post.title(),23,p.text,true));AsgramUi.gap(copy,20);
            View media = postMedia(c, this, post, 230, null);
            if (media != null) {
                copy.addView(media, AsgramUi.lp(-1, -2));
                AsgramUi.gap(copy, 12);
            }
            TextView body=AsgramUi.text(c,post.body(),17,p.text,false);body.setTextIsSelectable(true);body.setLineSpacing(AsgramUi.dp(7),1f);copy.addView(body,AsgramUi.lp(-1,-2));AsgramUi.gap(copy,24);
            if (!post.channel.equals("asgram_demo")) {
                copy.addView(AsgramUi.button(c, "Open in Telegram", p, true,
                        () -> Browser.openUrl(getParentActivity(), post.url)), AsgramUi.lp(-1, -2));
            }
            AsgramUi.gap(copy,10);
            TextView save=AsgramUi.button(c,AsgramFeedStore.isSaved(c,post.id)?"Remove from saved":"Save for later",p,false,()->{});
            save.setOnClickListener(v->{boolean now=AsgramFeedStore.toggleSave(c,post);save.setText(now?"Remove from saved":"Save for later");});
            copy.addView(save,AsgramUi.lp(-1,-2));return scroll;
        }
        @Override public boolean isLightStatusBar() {return fragmentView==null||!new AsgramUi.Palette(fragmentView.getContext()).dark;}
    }

    public static final class AsgramSettingsActivity extends BaseFragment {
        private LinearLayout content;
        @Override public View createView(Context c) {
            actionBar.setTitle("Feed settings");actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
            ScrollView scroll=new ScrollView(c);content=AsgramUi.column(c);
            content.setPadding(AsgramUi.dp(22),AsgramUi.dp(20),AsgramUi.dp(22),AndroidUtilities.navigationBarHeight+AsgramUi.dp(24));
            scroll.addView(content,AsgramUi.lp(-1,-2));fragmentView=scroll;renderSettings(c);return scroll;
        }
        private void renderSettings(Context c) {
            AsgramUi.Palette p=new AsgramUi.Palette(c);content.removeAllViews();fragmentView.setBackgroundColor(p.background);
            actionBar.setBackgroundColor(p.background);actionBar.setTitleColor(p.text);actionBar.setItemsColor(p.text,false);
            checkSystemBarColors();
            content.addView(AsgramUi.button(c,"Appearance",p,false,()->presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,22);
            LinearLayout saver=AsgramUi.row(c);LinearLayout info=AsgramUi.column(c);
            info.addView(AsgramUi.text(c,"Save mobile data",16,p.text,true));AsgramUi.gap(info,6);
            info.addView(AsgramUi.text(c,"Load photos on Wi-Fi. Tap a photo to load it on mobile data.",13,p.secondary,false));
            saver.addView(info,new LinearLayout.LayoutParams(0,-2,1));Switch control=new Switch(c);
            control.setChecked(AsgramUi.prefs(c).getBoolean("data_saver",true));control.setContentDescription("Save mobile data for feed photos");
            control.setOnCheckedChangeListener((button,checked)->AsgramUi.prefs(c).edit().putBoolean("data_saver",checked).apply());
            LinearLayout.LayoutParams switchLp=AsgramUi.lp(58,52);switchLp.leftMargin=AsgramUi.dp(14);saver.addView(control,switchLp);
            content.addView(saver,AsgramUi.lp(-1,-2));AsgramUi.gap(content,24);AsgramUi.divider(content,p);AsgramUi.gap(content,24);
            content.addView(AsgramUi.text(c,"Channels",21,p.text,true));AsgramUi.gap(content,12);
            content.addView(AsgramUi.text(c,"Posts come from the public Abdii Sabaa network on Telegram.",14,p.secondary,false));AsgramUi.gap(content,24);
            content.addView(AsgramUi.text(c,"Your privacy",21,p.text,true));AsgramUi.gap(content,12);
            content.addView(AsgramUi.text(c,"ASGRAM is an unofficial Telegram app. Your Telegram account and chats use Telegram’s services. We do not copy your private chats or contacts to an ASGRAM server.\n\nThe feed loads public channel previews from Telegram. Saved articles and reading preferences are stored on this device.",14,p.secondary,false));
            AsgramUi.gap(content,10);
            content.addView(AsgramUi.button(c,"ASGRAM privacy policy",p,false,()->Browser.openUrl(getParentActivity(),"https://asbeks.github.io/asgram/privacy.html")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,20);
            content.addView(AsgramUi.button(c,"Notification settings",p,false,()->{
                Intent intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.getPackageName());
                if(android.os.Build.VERSION.SDK_INT<26)intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+c.getPackageName()));
                getParentActivity().startActivity(intent);
            }),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,10);
            content.addView(AsgramUi.button(c,"Contact support",p,false,()->Browser.openUrl(getParentActivity(),"https://t.me/fulbeksupport")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,10);
            content.addView(AsgramUi.button(c,"Telegram API terms",p,false,()->Browser.openUrl(getParentActivity(),"https://core.telegram.org/api/terms")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,10);
            content.addView(AsgramUi.button(c,"Telegram open-source credits",p,false,()->Browser.openUrl(getParentActivity(),"https://github.com/DrKLO/Telegram")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,24);content.addView(AsgramUi.text(c,"ASGRAM 0.4.0 · Unofficial Telegram client",12,p.secondary,false));
        }
        @Override public boolean isLightStatusBar() {return fragmentView==null||!new AsgramUi.Palette(fragmentView.getContext()).dark;}
    }
}
