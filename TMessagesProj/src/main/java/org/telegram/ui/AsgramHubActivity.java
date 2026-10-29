package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.asgram.AsgramFeed;
import org.telegram.messenger.asgram.AsgramFeedStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.asgram.AsgramUi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AsgramHubActivity extends BaseFragment {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private List<AsgramFeed.Post> posts=new ArrayList<>();
    private String category="All",query="";
    private boolean savedOnly,loading,destroyed,complete=true;
    private AsgramUi.Palette palette;
    private LinearLayout body,list,filters,bottom;
    private TextView status;
    private int request;
    @Override public View createView(Context context) {
        palette=new AsgramUi.Palette(context);
        actionBar.setTitle("ASGRAM");
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if(id==-1)finishFragment();
                else if(id==1)refresh();
            }
        });
        actionBar.createMenu().addItem(1,R.drawable.msg_retry).setContentDescription("Refresh updates");
        FrameLayout root=new FrameLayout(context) {
            @Override protected void onMeasure(int widthSpec,int heightSpec) {
                if(body!=null) {
                    ViewGroup.LayoutParams lp=body.getLayoutParams();
                    lp.width=Math.min(View.MeasureSpec.getSize(widthSpec),AsgramUi.dp(780));
                    body.setLayoutParams(lp);
                }
                super.onMeasure(widthSpec,heightSpec);
            }
        };
        fragmentView=root;
        body=AsgramUi.column(context);
        root.addView(body,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT,Gravity.CENTER_HORIZONTAL));
        posts=AsgramFeedStore.cached(context);
        build(context);
        refresh();
        return root;
    }
    private void build(Context context) {
        palette=new AsgramUi.Palette(context);AsgramUi.Palette p=palette;
        fragmentView.setBackgroundColor(p.background);body.removeAllViews();body.setBackgroundColor(p.background);
        actionBar.setBackgroundColor(p.background);actionBar.setTitleColor(p.text);actionBar.setItemsColor(p.text,false);
        actionBar.setItemsBackgroundColor(p.tint,false);
        LinearLayout header=AsgramUi.column(context);header.setPadding(AsgramUi.dp(20),AsgramUi.dp(16),AsgramUi.dp(20),0);
        LinearLayout masthead=AsgramUi.row(context);
        LinearLayout titles=AsgramUi.column(context);
        titles.addView(AsgramUi.text(context,"Abdii Sabaa",29,p.text,true));
        AsgramUi.gap(titles,6);
        titles.addView(AsgramUi.text(context,"Your news. Your people. Your place.",14,p.secondary,false));
        masthead.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        ImageView logo=new ImageView(context);logo.setImageResource(R.drawable.asgram_media_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Abdii Sabaa Media");
        logo.setBackground(AsgramUi.shape(Color.WHITE,16));masthead.addView(logo,AsgramUi.lp(54,54));
        header.addView(masthead,AsgramUi.lp(-1,-2));
        AsgramUi.gap(header,20);
        LinearLayout search=AsgramUi.row(context);search.setBackground(AsgramUi.shape(p.surface,16));
        search.addView(new AsgramUi.Glyph(context,"Search",p.secondary),AsgramUi.lp(48,48));
        EditText field=new EditText(context);field.setSingleLine(true);field.setTextSize(15);field.setTextColor(p.text);field.setHintTextColor(p.secondary);
        field.setBackgroundColor(Color.TRANSPARENT);field.setPadding(0,0,AsgramUi.dp(12),0);field.setHint("Search news and education");
        field.setContentDescription("Search public Abdii Sabaa updates");field.setText(query);
        search.addView(field,new LinearLayout.LayoutParams(0,AsgramUi.dp(52),1));
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) {query=s.toString();render();}
            public void afterTextChanged(Editable e) {}
        });
        header.addView(search,AsgramUi.lp(-1,52));
        AsgramUi.gap(header,12);
        HorizontalScrollView horizontal=new HorizontalScrollView(context);horizontal.setHorizontalScrollBarEnabled(false);
        filters=AsgramUi.row(context);horizontal.addView(filters);
        header.addView(horizontal,AsgramUi.lp(-1,52));
        status=AsgramUi.text(context,"",12,p.secondary,false);status.setPadding(AsgramUi.dp(2),AsgramUi.dp(6),0,AsgramUi.dp(10));
        header.addView(status,AsgramUi.lp(-1,-2));body.addView(header,AsgramUi.lp(-1,-2));
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.setClipToPadding(false);
        scroll.setPadding(AsgramUi.dp(20),AsgramUi.dp(6),AsgramUi.dp(20),AsgramUi.dp(20));
        list=AsgramUi.column(context);scroll.addView(list,AsgramUi.lp(-1,-2));
        body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        bottom=AsgramUi.row(context);bottom.setBackgroundColor(p.surface);
        bottom.setPadding(AsgramUi.dp(8),AsgramUi.dp(7),AsgramUi.dp(8),Math.max(AsgramUi.dp(8),AndroidUtilities.navigationBarHeight));
        body.addView(bottom,AsgramUi.lp(-1,-2));
        render();
    }
    private void nav(Context c,LinearLayout parent,String icon,String label,boolean selected,Runnable action) {
        LinearLayout item=AsgramUi.column(c);item.setGravity(Gravity.CENTER);item.setPadding(0,AsgramUi.dp(2),0,AsgramUi.dp(4));
        item.setContentDescription(label);item.setMinimumHeight(AsgramUi.dp(56));item.setClickable(true);
        AsgramUi.Glyph glyph=new AsgramUi.Glyph(c,icon,selected?palette.primary:palette.secondary);
        if(selected)glyph.setBackground(AsgramUi.shape(palette.tint,16));
        item.addView(glyph,AsgramUi.lp(56,32));AsgramUi.gap(item,3);
        item.addView(AsgramUi.text(c,label,11,selected?palette.primary:palette.secondary,selected));
        item.setOnClickListener(v->action.run());parent.addView(item,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void updateNav(Context context) {
        if(bottom==null)return;
        bottom.removeAllViews();
        nav(context,bottom,"Chats","Chats",false,()->{
            if(UserConfig.getInstance(currentAccount).isClientActivated())finishFragment();
            else AsgramWelcomeActivity.openLogin(this);
        });
        nav(context,bottom,"Home","Home",!savedOnly,()->{savedOnly=false;category="All";render();});
        nav(context,bottom,"Saved","Saved",savedOnly,()->{savedOnly=true;render();});
        nav(context,bottom,"Settings","Settings",false,()->presentFragment(new AsgramSettingsActivity()));
    }
    private void render() {
        if(list==null||destroyed)return;Context c=list.getContext();AsgramUi.Palette p=palette;
        updateNav(c);
        filters.removeAllViews();
        for(String value:new String[]{"All","News","Education","Sports"}) {
            boolean active=category.equals(value)&&!savedOnly;
            TextView chip=AsgramUi.text(c,value,14,active?p.inverse:p.secondary,active);
            chip.setGravity(Gravity.CENTER);chip.setMinHeight(AsgramUi.dp(48));chip.setPadding(AsgramUi.dp(17),0,AsgramUi.dp(17),0);
            if(active)chip.setBackground(AsgramUi.shape(p.primary,24));
            chip.setContentDescription("Show "+value+" updates");chip.setSelected(active);
            chip.setOnClickListener(v->{category=value;savedOnly=false;render();});
            LinearLayout.LayoutParams cp=AsgramUi.lp(-2,48);cp.rightMargin=AsgramUi.dp(5);filters.addView(chip,cp);
        }
        list.removeAllViews();
        status.setText(loading?"Refreshing public updates…":!complete?"Couldn’t refresh everything. Saved updates are available.":savedOnly?"Saved on this device":lastUpdated(c));
        if(!savedOnly && category.equals("Sports")) {
            list.addView(AsgramUi.text(c,"Ispoortii, closer to you.",24,p.text,true));
            AsgramUi.gap(list,12);list.addView(AsgramUi.text(c,"Football news and updates from the Abdii Sabaa Ispoortii team.",16,p.secondary,false));
            AsgramUi.gap(list,20);
            list.addView(AsgramUi.button(c,"Open sports channel",p,true,()->open("https://t.me/+7P24H-U3sWdlNzJk")),AsgramUi.lp(-1,-2));
            AsgramUi.gap(list,14);list.addView(AsgramUi.text(c,"You decide whether to join when the channel opens.",13,p.secondary,false));
            community(c);return;
        }
        List<AsgramFeed.Post> source=savedOnly?AsgramFeedStore.saved(c):posts;
        List<AsgramFeed.Post> selected=new ArrayList<>();String term=query.toLowerCase(Locale.ROOT).trim();
        for(AsgramFeed.Post post:source) {
            if(!savedOnly&&!category.equals("All")&&!category.equals(post.category))continue;
            if(!term.isEmpty()&&!(post.text+" "+post.author).toLowerCase(Locale.ROOT).contains(term))continue;
            selected.add(post);
        }
        list.addView(AsgramUi.text(c,savedOnly?"Saved for later":term.isEmpty()?"Latest updates":"Search results",21,p.text,true));
        AsgramUi.gap(list,14);
        if(selected.isEmpty()) {
            list.addView(AsgramUi.text(c,savedOnly?"Keep a good read close.":"No updates to show yet.",20,p.text,true));
            AsgramUi.gap(list,10);
            list.addView(AsgramUi.text(c,savedOnly?"Tap Save on an article to read it again here.":"Try another search or refresh when you’re connected.",15,p.secondary,false));
            AsgramUi.gap(list,16);
            if(!savedOnly)list.addView(AsgramUi.button(c,"Refresh updates",p,false,this::refresh),AsgramUi.lp(-1,-2));
        }
        for(AsgramFeed.Post post:selected)articleCard(c,post);
        if(!savedOnly && term.isEmpty()) {
            String link=category.equals("Education")?"https://t.me/abdii_sabaa_university":"https://t.me/ABDII_SABAA";
            AsgramUi.gap(list,8);list.addView(AsgramUi.button(c,"Open channel in Telegram",p,false,()->open(link)),AsgramUi.lp(-1,-2));
            community(c);
        }
    }
    private String lastUpdated(Context c) {
        long at=AsgramUi.prefs(c).getLong("last_sync",0);
        return at==0?"Public previews · refresh for the latest":"Updated "+DateUtils.getRelativeTimeSpanString(at,System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS).toString().toLowerCase(Locale.ROOT);
    }
    private void articleCard(Context c,AsgramFeed.Post post) {
        AsgramUi.Palette p=palette;
        LinearLayout card=AsgramUi.column(c);card.setBackground(AsgramUi.shape(p.surface,16));card.setPadding(AsgramUi.dp(16),AsgramUi.dp(16),AsgramUi.dp(16),AsgramUi.dp(6));
        LinearLayout author=AsgramUi.row(c);
        TextView source=AsgramUi.text(c,post.author,13,p.primary,true);author.addView(source,new LinearLayout.LayoutParams(0,-2,1));
        TextView time=AsgramUi.text(c,relative(post),11,p.secondary,false);author.addView(time,AsgramUi.lp(-2,-2));
        card.addView(author,AsgramUi.lp(-1,-2));AsgramUi.gap(card,12);
        if(!post.imageUrl.isEmpty()) {
            FrameLayout photo=photo(c,post,AsgramFeedStore.canLoadPhotos(c));card.addView(photo,AsgramUi.lp(-1,180));AsgramUi.gap(card,14);
        }
        TextView title=AsgramUi.text(c,post.title(),19,p.text,true);title.setMaxLines(4);title.setEllipsize(TextUtils.TruncateAt.END);
        title.setOnClickListener(v->presentFragment(new AsgramArticleActivity(post)));title.setContentDescription("Read "+post.title());
        card.addView(title,AsgramUi.lp(-1,-2));
        String excerpt=post.body();
        if(excerpt.length()>30) {
            AsgramUi.gap(card,8);TextView text=AsgramUi.text(c,excerpt,14,p.secondary,false);text.setMaxLines(2);text.setEllipsize(TextUtils.TruncateAt.END);
            text.setOnClickListener(v->presentFragment(new AsgramArticleActivity(post)));card.addView(text,AsgramUi.lp(-1,-2));
        }
        AsgramUi.gap(card,8);
        LinearLayout actions=AsgramUi.row(c);
        TextView read=AsgramUi.text(c,"Read story  →",14,p.primary,true);read.setMinHeight(AsgramUi.dp(48));read.setGravity(Gravity.CENTER_VERTICAL);
        read.setOnClickListener(v->presentFragment(new AsgramArticleActivity(post)));actions.addView(read,new LinearLayout.LayoutParams(0,AsgramUi.dp(48),1));
        boolean isSaved=AsgramFeedStore.isSaved(c,post.id);
        TextView save=AsgramUi.text(c,isSaved?"Saved":"Save",13,p.primary,true);save.setGravity(Gravity.CENTER);save.setPadding(AsgramUi.dp(14),0,AsgramUi.dp(14),0);save.setMinHeight(AsgramUi.dp(48));
        save.setContentDescription((isSaved?"Remove saved article: ":"Save article: ")+post.title());
        save.setOnClickListener(v->{AsgramFeedStore.toggleSave(c,post);render();});actions.addView(save,AsgramUi.lp(-2,48));
        AsgramUi.Glyph share=new AsgramUi.Glyph(c,"Share",p.secondary);share.setContentDescription("Share "+post.title());
        share.setOnClickListener(v->share(post));actions.addView(share,AsgramUi.lp(48,48));
        card.addView(actions,AsgramUi.lp(-1,48));
        list.addView(card,AsgramUi.lp(-1,-2));AsgramUi.gap(list,14);
    }
    static FrameLayout photo(Context c,AsgramFeed.Post post,boolean load) {
        AsgramUi.Palette p=new AsgramUi.Palette(c);
        FrameLayout frame=new FrameLayout(c);frame.setBackground(AsgramUi.shape(p.tint,12));
        if(load) {
            BackupImageView image=new BackupImageView(c);image.setRoundRadius(AsgramUi.dp(12));image.setAspectFit(false);
            image.setImage(ImageLocation.getForPath(post.imageUrl),"800_500",(android.graphics.drawable.Drawable)null,post);
            image.setContentDescription("Photo from "+post.author);frame.addView(image,LayoutHelper.createFrame(-1,-1));
        } else {
            TextView button=AsgramUi.text(c,"Load photo",14,p.primary,true);button.setGravity(Gravity.CENTER);
            frame.addView(button,LayoutHelper.createFrame(-1,-1));
            button.setOnClickListener(v->{frame.removeAllViews();FrameLayout loaded=photo(c,post,true);frame.addView(loaded,LayoutHelper.createFrame(-1,-1));});
        }
        return frame;
    }
    private static String relative(AsgramFeed.Post post) {
        return post.timestamp()>0?DateUtils.getRelativeTimeSpanString(post.timestamp(),System.currentTimeMillis(),DateUtils.HOUR_IN_MILLIS,DateUtils.FORMAT_ABBREV_RELATIVE).toString():"Telegram";
    }
    private void community(Context c) {
        AsgramUi.gap(list,28);
        list.addView(AsgramUi.text(c,"Be part of the conversation.",20,palette.text,true));AsgramUi.gap(list,10);
        list.addView(AsgramUi.text(c,"Meet the community or send a story to our team.",14,palette.secondary,false));
        AsgramUi.gap(list,14);
        list.addView(AsgramUi.button(c,"Open community group",palette,false,()->open("https://t.me/Garee_Abdii_Sabaa")),AsgramUi.lp(-1,-2));
        AsgramUi.gap(list,8);
        list.addView(AsgramUi.button(c,"Send a story or contact us",palette,false,()->open("https://t.me/AbdiiSabaaDm")),AsgramUi.lp(-1,-2));
    }
    private void open(String url) {if(getParentActivity()!=null)Browser.openUrl(getParentActivity(),url);}
    private void share(AsgramFeed.Post post) {
        if(getParentActivity()==null)return;
        Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TEXT,post.title()+"\n"+post.url);
        getParentActivity().startActivity(Intent.createChooser(intent,"Share story"));
    }
    private void refresh() {
        if(loading||fragmentView==null)return;loading=true;render();final int generation=++request;
        final Context c=fragmentView.getContext().getApplicationContext();
        executor.execute(()->{
            AsgramFeedStore.Result result=AsgramFeedStore.refresh(c);
            AndroidUtilities.runOnUIThread(()->{if(destroyed||generation!=request)return;posts=result.posts;complete=result.complete;loading=false;render();});
        });
    }
    @Override public void onResume() {
        super.onResume();
        if(fragmentView!=null) {
            AsgramUi.Palette next=new AsgramUi.Palette(fragmentView.getContext());
            if(palette.dark!=next.dark)build(fragmentView.getContext());else render();
        }
    }
    @Override public void onFragmentDestroy() {destroyed=true;request++;executor.shutdownNow();super.onFragmentDestroy();}
    @Override public boolean isLightStatusBar() {return palette==null||!palette.dark;}

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
            if(!post.imageUrl.isEmpty()){copy.addView(photo(c,post,AsgramFeedStore.canLoadPhotos(c)),AsgramUi.lp(-1,230));AsgramUi.gap(copy,20);}
            TextView body=AsgramUi.text(c,post.body(),17,p.text,false);body.setTextIsSelectable(true);body.setLineSpacing(AsgramUi.dp(7),1f);copy.addView(body,AsgramUi.lp(-1,-2));AsgramUi.gap(copy,24);
            copy.addView(AsgramUi.button(c,"Open original in Telegram",p,true,()->Browser.openUrl(getParentActivity(),post.url)),AsgramUi.lp(-1,-2));
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
            String mode=AsgramUi.prefs(c).getString("appearance","system");
            content.addView(AsgramUi.button(c,"Appearance · "+mode,p,false,()->new AlertDialog.Builder(getParentActivity()).setTitle("Appearance")
                .setItems(new CharSequence[]{"Use device setting","Light","Dark"},(dialog,which)->{AsgramUi.prefs(c).edit().putString("appearance",new String[]{"system","light","dark"}[which]).apply();renderSettings(c);}).show()),AsgramUi.lp(-1,-2));
            AsgramUi.gap(content,22);
            LinearLayout saver=AsgramUi.row(c);LinearLayout info=AsgramUi.column(c);
            info.addView(AsgramUi.text(c,"Save mobile data",16,p.text,true));AsgramUi.gap(info,6);
            info.addView(AsgramUi.text(c,"Load hub photos automatically on Wi-Fi. You can always tap to load a photo.",13,p.secondary,false));
            saver.addView(info,new LinearLayout.LayoutParams(0,-2,1));Switch control=new Switch(c);
            control.setChecked(AsgramUi.prefs(c).getBoolean("data_saver",true));control.setContentDescription("Save mobile data for hub photos");
            control.setOnCheckedChangeListener((button,checked)->AsgramUi.prefs(c).edit().putBoolean("data_saver",checked).apply());
            LinearLayout.LayoutParams switchLp=AsgramUi.lp(58,52);switchLp.leftMargin=AsgramUi.dp(14);saver.addView(control,switchLp);
            content.addView(saver,AsgramUi.lp(-1,-2));AsgramUi.gap(content,24);AsgramUi.divider(content,p);AsgramUi.gap(content,24);
            content.addView(AsgramUi.text(c,"Your privacy",21,p.text,true));AsgramUi.gap(content,12);
            content.addView(AsgramUi.text(c,"ASGRAM is an independent Telegram client by Abdii Sabaa Media. Your Telegram account and chats use Telegram’s services. We do not copy your private chats or contacts to an Abdii Sabaa server.\n\nThe media hub loads public channel previews from Telegram. Saved articles and reading preferences are stored on this device.",14,p.secondary,false));
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
            AsgramUi.gap(content,24);content.addView(AsgramUi.text(c,"ASGRAM 0.1.0 · Development build\nBuilt on Telegram’s open-source Android client.\nNot affiliated with Telegram.",12,p.secondary,false));
        }
        @Override public boolean isLightStatusBar() {return fragmentView==null||!new AsgramUi.Palette(fragmentView.getContext()).dark;}
    }
}
