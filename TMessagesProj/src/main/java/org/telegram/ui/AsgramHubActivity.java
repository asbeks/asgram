/*
 * THESIS: A media front desk puts Abdii Sabaa stories before a generic chat directory.
 * OWN-WORLD: Native Telegram theme tokens, system type, real publisher imagery.
 * STORY: Scan the network, choose a topic, read/save a real story, return to native chats.
 * FIRST VIEWPORT: ASGRAM bar; Feed title; four channel circles; topic row; lead story.
 * FORM: Grounded candidate 4, media front desk; seed ec560cb4; comp A. Five native roots
 * preserve the client's existing chat/contact/settings/profile access alongside Feed.
 * FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
 */
package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
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
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.asgram.AsgramNativeFeed;
import org.telegram.messenger.asgram.AsgramDirectory;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.asgram.AsgramFeed;
import org.telegram.messenger.asgram.AsgramFeedStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.asgram.AsgramUi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AsgramHubActivity extends BaseFragment implements MainTabsActivity.TabFragmentDelegate {
    // The debug-only screenshot activity supplies synthetic content before any capture.
    private boolean storePreview;
    public AsgramHubActivity() {}
    public AsgramHubActivity(boolean preview) { storePreview=org.telegram.messenger.BuildConfig.DEBUG_VERSION && preview; }
    private List<AsgramFeed.Post> posts=new ArrayList<>(),visible=new ArrayList<>();
    private String category="All",query="";
    private boolean latest=true;
    private boolean savedOnly,loading,destroyed,complete=true,joining;
    private AsgramUi.Palette palette;
    private RecyclerView recycler;
    private LinearLayoutManager layoutManager;
    private FeedAdapter adapter;
    private int request;

    @Override public View createView(Context context) {
        destroyed=false;
        palette=new AsgramUi.Palette(context);
        actionBar.setTitle("ASGRAM");
        actionBar.setSubtitle(storePreview?"Unofficial Telegram app · Demo content":"Unofficial Telegram app");
        actionBar.setBackgroundColor(palette.background);
        actionBar.setTitleColor(palette.text);
        actionBar.setSubtitleColor(palette.secondary);
        actionBar.setItemsColor(palette.text,false);
        actionBar.setCastShadows(false);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if(storePreview) return;
                if(id==2) {savedOnly=!savedOnly;applyFilter(true);}
                else if(id==3)refresh();
                else if(id==4)presentFragment(new AsgramSettingsActivity());
            }
        });
        actionBar.createMenu().addItem(1,R.drawable.outline_header_search).setIsSearchField(true)
            .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                @Override public void onTextChanged(EditText field) {query=field.getText().toString();applyFilter(false);}
            }).setSearchFieldHint("Search media updates");
        actionBar.createMenu().addItem(2,R.drawable.msg_saved).setContentDescription("Show saved stories");
        actionBar.createMenu().addItem(3,R.drawable.msg_retry).setContentDescription("Refresh feed");
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
        if(storePreview) {
            posts.add(new AsgramFeed.Post("asgram_demo/1","News","asgram_demo","Daily Discoveries","A little curiosity. A whole new perspective.\nExplore fresh ideas, places and stories from the channels you love. Read the full story, then save it for later.","sample://landscape",java.time.OffsetDateTime.now().toString()));
            posts.add(new AsgramFeed.Post("asgram_demo/2","Education","asgram_demo","Learning Circle","Make room for one new idea today\nA short reading list for a more thoughtful week.","",java.time.OffsetDateTime.now().minusMinutes(12).toString()));
            posts.add(new AsgramFeed.Post("asgram_demo/3","Sports","asgram_demo","The Sports Desk","Your weekend match guide\nFixtures, highlights and conversations in one place.","",java.time.OffsetDateTime.now().minusMinutes(24).toString()));
            latest=true;applyFilter(false);
        } else { posts=AsgramFeedStore.cached(context);latest=AsgramUi.prefs(context).getBoolean("latest_first",true);applyFilter(false);refresh(); }
        return root;
    }
    private void applyFilter(boolean toTop) {
        if(recycler==null||destroyed)return;
        List<AsgramFeed.Post> source=savedOnly?AsgramFeedStore.saved(recycler.getContext()):posts;
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
        adapter.notifyDataSetChanged();
        if(toTop)layoutManager.scrollToPositionWithOffset(0,0);
    }
    private LinearLayout padded(Context c,int top,int bottom) {
        LinearLayout column=AsgramUi.column(c);
        column.setPadding(AsgramUi.dp(20),AsgramUi.dp(top),AsgramUi.dp(20),AsgramUi.dp(bottom));
        return column;
    }
    private View header(Context c) {
        LinearLayout box=padded(c,18,8);
        LinearLayout heading=AsgramUi.row(c);
        heading.addView(AsgramUi.text(c,savedOnly?"Saved stories":"Feed",32,palette.text,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=AsgramUi.text(c,"Media settings",12,palette.primary,true);
        settings.setGravity(Gravity.CENTER);settings.setMinHeight(AsgramUi.dp(48));
        settings.setOnClickListener(v->{if(!storePreview)presentFragment(new AsgramSettingsActivity());});
        heading.addView(settings,AsgramUi.lp(-2,48));box.addView(heading);
        if(!savedOnly) {
            TextView description=AsgramUi.text(c,"Your people. Your stories.",14,palette.secondary,false);
            box.addView(description);AsgramUi.gap(box,18);
            LinearLayout rail=AsgramUi.row(c);rail.setGravity(Gravity.TOP);
            if(storePreview) {
                for(String[] entry:new String[][]{{"Discover","News"},{"Learning","Education"},{"Sports","Sports"},{"Community","Chats"}}) {
                    LinearLayout item=AsgramUi.column(c);item.setGravity(Gravity.CENTER);
                    item.addView(new AsgramUi.Glyph(c,entry[1],palette.primary),AsgramUi.lp(52,52));
                    item.addView(AsgramUi.text(c,entry[0],12,palette.text,true));rail.addView(item,new LinearLayout.LayoutParams(0,-2,1));
                }
                box.addView(rail,AsgramUi.lp(-1,-2));AsgramUi.gap(box,16);
            } else {
            channel(c,rail,"Abdii Sabaa","News","https://t.me/ABDII_SABAA",true);
            channel(c,rail,"University","Education","https://t.me/abdii_sabaa_university",false);
            channel(c,rail,"Sports","Sports","https://t.me/+7P24H-U3sWdlNzJk",false);
            channel(c,rail,"Community","Chats","https://t.me/Garee_Abdii_Sabaa",false);
            box.addView(rail,AsgramUi.lp(-1,-2));
            List<AsgramDirectory.Entry> network=AsgramNativeFeed.entries(c);
            if(network.size()>2){AsgramUi.gap(box,12);HorizontalScrollView extraScroll=new HorizontalScrollView(c);extraScroll.setHorizontalScrollBarEnabled(false);LinearLayout extra=AsgramUi.row(c);
                for(int n=2;n<network.size();n++){final AsgramDirectory.Entry entry=network.get(n);TextView link=AsgramUi.button(c,entry.title,palette,false,()->open("https://t.me/"+entry.username));LinearLayout.LayoutParams lp=AsgramUi.lp(-2,48);lp.rightMargin=AsgramUi.dp(8);extra.addView(link,lp);}extraScroll.addView(extra);box.addView(extraScroll);}
            AsgramUi.gap(box,16);
            org.telegram.tgnet.TLObject publisher=getMessagesController().getUserOrChat("ABDII_SABAA");
            if(publisher instanceof org.telegram.tgnet.TLRPC.Chat && org.telegram.messenger.ChatObject.isNotInChat((org.telegram.tgnet.TLRPC.Chat)publisher) && !AsgramUi.prefs(c).getBoolean("follow_dismissed_"+currentAccount,false)) {
                final org.telegram.tgnet.TLRPC.Chat channel=(org.telegram.tgnet.TLRPC.Chat)publisher;
                box.addView(AsgramUi.text(c,"Keep Abdii Sabaa in your Telegram.",16,palette.text,true));AsgramUi.gap(box,8);
                LinearLayout follow=AsgramUi.row(c);
                TextView join=AsgramUi.button(c,joining?"Joining…":"Join Abdii Sabaa",palette,true,()->{
                    if(joining)return;joining=true;applyFilter(false);
                    getMessagesController().addUserToChat(channel.id,getUserConfig().getCurrentUser(),0,null,AsgramHubActivity.this,false,()->{joining=false;applyFilter(false);},error->{joining=false;applyFilter(false);return true;});
                });join.setEnabled(!joining);follow.addView(join,new LinearLayout.LayoutParams(0,-2,1));
                TextView later=AsgramUi.button(c,"Not now",palette,false,()->{AsgramUi.prefs(c).edit().putBoolean("follow_dismissed_"+currentAccount,true).apply();applyFilter(false);});LinearLayout.LayoutParams lp=AsgramUi.lp(-2,-2);lp.leftMargin=AsgramUi.dp(8);follow.addView(later,lp);box.addView(follow);AsgramUi.gap(box,14);
            }
            }
        } else {
            box.addView(AsgramUi.text(c,"Good stories, kept on this device.",14,palette.secondary,false));AsgramUi.gap(box,12);
        }
        HorizontalScrollView horizontal=new HorizontalScrollView(c);horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout filters=AsgramUi.row(c);
        String[] values={"Latest","For you","News","Education","Sports"};String[] labels={"Latest","For you","News","Learning","Sports"};
        for(int i=0;i<values.length;i++) {
            final String value=values[i];boolean selected=!savedOnly&&(value.equals("Latest")?latest&&category.equals("All"):value.equals("For you")?!latest&&category.equals("All"):category.equals(value));
            TextView chip=AsgramUi.text(c,labels[i],14,selected?palette.inverse:palette.secondary,selected);
            chip.setGravity(Gravity.CENTER);chip.setPadding(AsgramUi.dp(18),0,AsgramUi.dp(18),0);chip.setMinHeight(AsgramUi.dp(48));
            chip.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AsgramUi.dp(24),selected?palette.primary:Color.TRANSPARENT,palette.tint));
            chip.setSelected(selected);chip.setContentDescription("Show "+labels[i]+" stories");
            chip.setOnClickListener(v->{if(value.equals("Latest")||value.equals("For you")){category="All";latest=value.equals("Latest");AsgramUi.prefs(c).edit().putBoolean("latest_first",latest).apply();}else category=value;savedOnly=false;applyFilter(true);});
            filters.addView(chip,AsgramUi.lp(-2,48));
        }
        horizontal.addView(filters);box.addView(horizontal,AsgramUi.lp(-1,48));
        return box;
    }
    private void channel(Context c,LinearLayout rail,String label,String glyph,String url,boolean logo) {
        LinearLayout item=AsgramUi.column(c);item.setGravity(Gravity.CENTER);
        FrameLayout circle=new FrameLayout(c);circle.setBackground(AsgramUi.shape(palette.tint,28));circle.setClipToOutline(true);
        if(logo) {
            ImageView brand=new ImageView(c);brand.setImageResource(R.drawable.asgram_media_logo);
            brand.setScaleType(ImageView.ScaleType.FIT_CENTER);brand.setBackground(AsgramUi.shape(Color.WHITE,28));
            circle.addView(brand,LayoutHelper.createFrame(-1,-1));
        } else circle.addView(new AsgramUi.Glyph(c,glyph,palette.primary),LayoutHelper.createFrame(-1,-1));
        item.addView(circle,AsgramUi.lp(56,56));AsgramUi.gap(item,8);
        TextView text=AsgramUi.text(c,label,12,palette.text,true);text.setGravity(Gravity.CENTER);text.setMaxLines(2);
        item.addView(text,AsgramUi.lp(-1,-2));item.setContentDescription("Open "+label+" channel");
        item.setOnClickListener(v->open(url));item.setBackground(Theme.createSelectorDrawable(palette.tint));
        rail.addView(item,new LinearLayout.LayoutParams(0,-2,1));
    }
    private View syncStatus(Context c) {
        LinearLayout box=padded(c,2,12);
        String text=storePreview?"English sample posts · No personal account data":loading?"Refreshing…":!complete?"Some updates could not refresh. Showing saved previews. Tap to retry.":lastUpdated(c);
        TextView status=AsgramUi.text(c,text,12,palette.secondary,false);status.setMinHeight(AsgramUi.dp(32));
        status.setGravity(Gravity.CENTER_VERTICAL);status.setOnClickListener(v->refresh());box.addView(status);
        return box;
    }
    private View empty(Context c) {
        LinearLayout box=padded(c,24,24);
        String title=category.equals("Sports")&&!savedOnly?"Abdii Sabaa Ispoortii":savedOnly?"Keep a good read close.":"No stories here yet.";
        box.addView(AsgramUi.text(c,title,25,palette.text,true));AsgramUi.gap(box,14);
        String copy=category.equals("Sports")&&!savedOnly?"Open our sports channel for football and the latest from the team.":savedOnly?"Save a story from your feed and it will be here when you return.":"Try another topic or search. You can also refresh when you’re connected.";
        box.addView(AsgramUi.text(c,copy,16,palette.secondary,false));AsgramUi.gap(box,22);
        box.addView(AsgramUi.button(c,category.equals("Sports")?"Open sports channel":savedOnly?"Back to Feed":"Refresh updates",palette,true,()->{
            if(category.equals("Sports")&&!savedOnly)open("https://t.me/+7P24H-U3sWdlNzJk");
            else if(savedOnly){savedOnly=false;category="All";applyFilter(true);}else refresh();
        }),AsgramUi.lp(-1,-2));return box;
    }
    private View story(Context c,AsgramFeed.Post post,boolean lead) {
        LinearLayout outside=padded(c,lead?0:8,lead?18:8);
        LinearLayout card=AsgramUi.column(c);
        if(lead) {
            card.setBackground(AsgramUi.shape(palette.surface,16));
            if(post.hasPhoto()) {
                FrameLayout image=photo(c,post,AsgramFeedStore.canLoadPhotos(c));
                image.setOnClickListener(v->expand(post));card.addView(image,AsgramUi.lp(-1,190));
            }
        }
        LinearLayout copy=AsgramUi.column(c);copy.setPadding(lead?AsgramUi.dp(16):0,AsgramUi.dp(lead?16:4),lead?AsgramUi.dp(16):0,0);
        TextView meta=AsgramUi.text(c,post.author+" · "+relative(post),12,palette.primary,true);copy.addView(meta);AsgramUi.gap(copy,9);
        LinearLayout row=AsgramUi.row(c);row.setGravity(Gravity.TOP);
        TextView title=AsgramUi.text(c,post.title(),lead?22:18,palette.text,true);
        title.setMaxLines(lead?3:3);title.setEllipsize(TextUtils.TruncateAt.END);title.setOnClickListener(v->read(post));
        row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        if(!lead&&post.hasPhoto()) {
            FrameLayout image=photo(c,post,AsgramFeedStore.canLoadPhotos(c));image.setOnClickListener(v->expand(post));
            LinearLayout.LayoutParams lp=AsgramUi.lp(92,92);lp.leftMargin=AsgramUi.dp(14);row.addView(image,lp);
        }
        copy.addView(row);
        if(lead&&post.title().length()<130&&!post.body().isEmpty()) {
            AsgramUi.gap(copy,10);TextView excerpt=AsgramUi.text(c,post.body(),14,palette.secondary,false);
            excerpt.setMaxLines(2);excerpt.setEllipsize(TextUtils.TruncateAt.END);excerpt.setOnClickListener(v->read(post));copy.addView(excerpt);
        }
        LinearLayout actions=AsgramUi.row(c);
        TextView read=AsgramUi.text(c,post.hasVideo?"Open video  →":post.mediaCount()>1?"Open post · "+post.mediaCount()+" items  →":"Read story  →",14,palette.primary,true);read.setGravity(Gravity.CENTER_VERTICAL);read.setMinHeight(AsgramUi.dp(48));
        read.setOnClickListener(v->read(post));actions.addView(read,new LinearLayout.LayoutParams(0,AsgramUi.dp(48),1));
        boolean saved=AsgramFeedStore.isSaved(c,post.id);
        TextView save=AsgramUi.text(c,saved?"Saved":"Save",13,palette.primary,true);save.setGravity(Gravity.CENTER);save.setPadding(AsgramUi.dp(12),0,AsgramUi.dp(12),0);
        save.setContentDescription((saved?"Unsave ":"Save ")+post.title());save.setOnClickListener(v->{AsgramFeedStore.toggleSave(c,post);applyFilter(false);});
        actions.addView(save,AsgramUi.lp(-2,48));
        AsgramUi.Glyph share=new AsgramUi.Glyph(c,"Share",palette.secondary);share.setContentDescription("Share "+post.title());share.setOnClickListener(v->share(post));actions.addView(share,AsgramUi.lp(48,48));
        copy.addView(actions,AsgramUi.lp(-1,48));card.addView(copy);outside.addView(card);
        if(!lead){AsgramUi.gap(outside,12);AsgramUi.divider(outside,palette);}return outside;
    }
    private void expand(AsgramFeed.Post post) {if(storePreview)read(post);else openMedia(this,post);}
    private static void openMedia(BaseFragment fragment,AsgramFeed.Post post) {
        if(fragment.getParentActivity()==null)return;
        ArrayList<org.telegram.messenger.MessageObject> items=new ArrayList<>();
        for(org.telegram.tgnet.TLRPC.Message m:post.nativeAlbum) {
            if(m.media!=null&&(m.media.photo!=null||org.telegram.messenger.MessageObject.isVideoDocument(m.media.document)))
                items.add(new org.telegram.messenger.MessageObject(post.nativeAccount,m,false,true));
        }
        if(items.isEmpty()&&post.nativeMessage!=null&&post.nativePhoto!=null)items.add(new org.telegram.messenger.MessageObject(post.nativeAccount,post.nativeMessage,false,true));
        if(items.isEmpty()){Browser.openUrl(fragment.getParentActivity(),post.url);return;}
        PhotoViewer.getInstance().setParentActivity(fragment);
        PhotoViewer.getInstance().openPhoto(items,0,0,0,0,new PhotoViewer.EmptyPhotoViewerProvider());
    }
    private void read(AsgramFeed.Post post) {AsgramArticleActivity reader=new AsgramArticleActivity(post);reader.setCurrentAccount(currentAccount);presentFragment(reader);}
    private String lastUpdated(Context c) {
        long at=AsgramUi.prefs(c).getLong("last_sync",0);
        return at==0?"Refresh for the latest posts":System.currentTimeMillis()-at<60_000?"Just updated":"Updated "+DateUtils.getRelativeTimeSpanString(at,System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS).toString().toLowerCase(Locale.ROOT);
    }
    static FrameLayout photo(Context c,AsgramFeed.Post post,boolean load) {
        AsgramUi.Palette p=new AsgramUi.Palette(c);FrameLayout frame=new FrameLayout(c);frame.setBackground(AsgramUi.shape(p.tint,12));
        if(post.imageUrl.equals("sample://landscape")) {
            ImageView sample=new ImageView(c);sample.setImageResource(R.drawable.asgram_store_landscape);sample.setScaleType(ImageView.ScaleType.CENTER_CROP);sample.setContentDescription("Illustrated mountain landscape — demo content");frame.addView(sample,LayoutHelper.createFrame(-1,-1));
        } else if(load) {
            BackupImageView image=new BackupImageView(c);image.setRoundRadius(AsgramUi.dp(12));image.setAspectFit(false);
            if(post.nativePhoto!=null) {
                image.getImageReceiver().setCurrentAccount(post.nativeAccount);
                org.telegram.tgnet.TLRPC.PhotoSize size=FileLoader.getClosestPhotoSizeWithSize(post.nativePhoto.sizes,800);
                image.setImage(ImageLocation.getForPhoto(size,post.nativePhoto),"800_500",(android.graphics.drawable.Drawable)null,post.nativeMessage);
            } else image.setImage(ImageLocation.getForPath(post.imageUrl),"800_500",(android.graphics.drawable.Drawable)null,post);
            image.setContentDescription("Photo from "+post.author);frame.addView(image,LayoutHelper.createFrame(-1,-1));
        } else {
            TextView button=AsgramUi.text(c,"Load photo",14,p.primary,true);button.setGravity(Gravity.CENTER);frame.addView(button,LayoutHelper.createFrame(-1,-1));
            button.setOnClickListener(v->{frame.removeAllViews();frame.addView(photo(c,post,true),LayoutHelper.createFrame(-1,-1));});
        }
        if(post.mediaCount()>1){TextView count=AsgramUi.text(c,"▣ "+post.mediaCount()+" · Tap to expand",12,Color.WHITE,true);count.setPadding(AsgramUi.dp(10),AsgramUi.dp(6),AsgramUi.dp(10),AsgramUi.dp(6));count.setBackground(AsgramUi.shape(0xbb111111,12));frame.addView(count,LayoutHelper.createFrame(-2,-2,Gravity.RIGHT|Gravity.BOTTOM,8,8,8,8));}
        return frame;
    }
    private static String relative(AsgramFeed.Post post) {
        return post.timestamp()<=0?"Telegram":System.currentTimeMillis()-post.timestamp()<60_000?"Just now":DateUtils.getRelativeTimeSpanString(post.timestamp(),System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS,DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    }
    private void open(String url) {if(getParentActivity()!=null)Browser.openUrl(getParentActivity(),url);}
    private void share(AsgramFeed.Post post) {
        if(getParentActivity()==null)return;
        Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TEXT,post.title()+"\n"+post.url);
        getParentActivity().startActivity(Intent.createChooser(intent,"Share story"));
    }
    private void refresh() {
        if(storePreview)return;
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
    @Override public void onFragmentDestroy(){destroyed=true;request++;ConnectionsManager.getInstance(currentAccount).cancelRequestsForGuid(classGuid);super.onFragmentDestroy();}
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
            View child=position==0?header(c):position==1?syncStatus(c):visible.isEmpty()?empty(c):story(c,visible.get(position-2),position==2);
            frame.addView(child,LayoutHelper.createFrame(-1,-2));
        }
    }

    public static final class AsgramArticleActivity extends BaseFragment {
        private final AsgramFeed.Post post;
        AsgramArticleActivity(AsgramFeed.Post post) {this.post=post;}
        @Override public View createView(Context c) {
            AsgramUi.Palette p=new AsgramUi.Palette(c);actionBar.setTitle(post.category);actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setBackgroundColor(p.background);actionBar.setTitleColor(p.text);actionBar.setItemsColor(p.text,false);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
            ScrollView scroll=new ScrollView(c);scroll.setBackgroundColor(p.background);scroll.setFillViewport(true);
            LinearLayout copy=AsgramUi.column(c);copy.setPadding(AsgramUi.dp(22),AsgramUi.dp(22),AsgramUi.dp(22),AndroidUtilities.navigationBarHeight+AsgramUi.dp(24));
            scroll.addView(copy,AsgramUi.lp(-1,-2));fragmentView=scroll;
            copy.addView(AsgramUi.text(c,post.author+" · "+relative(post),13,p.primary,true));AsgramUi.gap(copy,16);
            copy.addView(AsgramUi.text(c,post.title(),23,p.text,true));AsgramUi.gap(copy,20);
            if(post.hasPhoto()){FrameLayout media=photo(c,post,AsgramFeedStore.canLoadPhotos(c));media.setOnClickListener(v->openMedia(this,post));copy.addView(media,AsgramUi.lp(-1,230));AsgramUi.gap(copy,12);}
            if(post.mediaCount()>1||post.hasVideo){copy.addView(AsgramUi.button(c,"Expand "+(post.mediaCount()>1?post.mediaCount()+" photos and videos":"video"),p,false,()->openMedia(this,post)),AsgramUi.lp(-1,-2));AsgramUi.gap(copy,16);}
            TextView body=AsgramUi.text(c,post.body(),17,p.text,false);body.setTextIsSelectable(true);body.setLineSpacing(AsgramUi.dp(7),1f);copy.addView(body,AsgramUi.lp(-1,-2));AsgramUi.gap(copy,24);
            copy.addView(AsgramUi.button(c,"Open original in Telegram",p,true,()->{if(!post.channel.equals("asgram_demo"))Browser.openUrl(getParentActivity(),post.url);}),AsgramUi.lp(-1,-2));
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
            actionBar.setTitle("ASGRAM settings");actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
            ScrollView scroll=new ScrollView(c);content=AsgramUi.column(c);
            content.setPadding(AsgramUi.dp(22),AsgramUi.dp(20),AsgramUi.dp(22),AndroidUtilities.navigationBarHeight+AsgramUi.dp(24));
            scroll.addView(content,AsgramUi.lp(-1,-2));fragmentView=scroll;renderSettings(c);return scroll;
        }
        private void renderSettings(Context c) {
            AsgramUi.Palette p=new AsgramUi.Palette(c);content.removeAllViews();fragmentView.setBackgroundColor(p.background);
            actionBar.setBackgroundColor(p.background);actionBar.setTitleColor(p.text);actionBar.setItemsColor(p.text,false);
            checkSystemBarColors();
            content.addView(AsgramUi.text(c,"Make it yours.",27,p.text,true));AsgramUi.gap(content,24);
            content.addView(AsgramUi.button(c,"Chat themes & appearance",p,false,()->presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,22);
            LinearLayout saver=AsgramUi.row(c);LinearLayout info=AsgramUi.column(c);
            info.addView(AsgramUi.text(c,"Save mobile data",16,p.text,true));AsgramUi.gap(info,6);
            info.addView(AsgramUi.text(c,"Load feed photos automatically on Wi-Fi. You can always tap to load a photo.",13,p.secondary,false));
            saver.addView(info,new LinearLayout.LayoutParams(0,-2,1));Switch control=new Switch(c);
            control.setChecked(AsgramUi.prefs(c).getBoolean("data_saver",true));control.setContentDescription("Save mobile data for feed photos");
            control.setOnCheckedChangeListener((button,checked)->AsgramUi.prefs(c).edit().putBoolean("data_saver",checked).apply());
            LinearLayout.LayoutParams switchLp=AsgramUi.lp(58,52);switchLp.leftMargin=AsgramUi.dp(14);saver.addView(control,switchLp);
            content.addView(saver,AsgramUi.lp(-1,-2));AsgramUi.gap(content,24);AsgramUi.divider(content,p);AsgramUi.gap(content,24);
            content.addView(AsgramUi.text(c,"The media network",21,p.text,true));AsgramUi.gap(content,12);
            content.addView(AsgramUi.text(c,"Official channels update from @ABDII_SABAA. The channel team can add an ASGRAM: @channel_one @channel_two line to its Telegram description. ASGRAM verifies each public channel and shows it after Feed refresh. Personal recommendations and advert links are not added automatically.",14,p.secondary,false));AsgramUi.gap(content,24);
            content.addView(AsgramUi.text(c,"Your privacy",21,p.text,true));AsgramUi.gap(content,12);
            content.addView(AsgramUi.text(c,"ASGRAM is an independent Telegram client by Abdii Sabaa Media. Your Telegram account and chats use Telegram’s services. We do not copy your private chats or contacts to an Abdii Sabaa server.\n\nThe media feed loads public channel previews from Telegram. Saved articles and reading preferences are stored on this device.",14,p.secondary,false));
            AsgramUi.gap(content,20);
            content.addView(AsgramUi.button(c,"Notification settings",p,false,()->{
                Intent intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.getPackageName());
                if(android.os.Build.VERSION.SDK_INT<26)intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+c.getPackageName()));
                getParentActivity().startActivity(intent);
            }),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,10);
            content.addView(AsgramUi.button(c,"Contact Abdii Sabaa",p,false,()->Browser.openUrl(getParentActivity(),"https://t.me/AbdiiSabaaDm")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,10);
            content.addView(AsgramUi.button(c,"Telegram API terms",p,false,()->Browser.openUrl(getParentActivity(),"https://core.telegram.org/api/terms")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,10);
            content.addView(AsgramUi.button(c,"Telegram open-source credits",p,false,()->Browser.openUrl(getParentActivity(),"https://github.com/DrKLO/Telegram")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,24);content.addView(AsgramUi.text(c,"ASGRAM 0.3.0 · Development build\nBuilt on Telegram’s open-source Android client.\nNot affiliated with Telegram.",12,p.secondary,false));
        }
        @Override public boolean isLightStatusBar() {return fragmentView==null||!new AsgramUi.Palette(fragmentView.getContext()).dark;}
    }
}
