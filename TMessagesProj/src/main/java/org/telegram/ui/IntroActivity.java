/*
 * ASGRAM native welcome, extending the official Telegram Android client.
 * GPL v2 or later. Original client copyright Nikolai Kudashov, 2013-2026.
 * Brand contract: native Telegram theme roles, the ASGRAM A, one clear messaging action.
 * Preserve native account sign-in, language selection and user-controlled appearance.
 */
package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.SimpleThemeDescription;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;

public class IntroActivity extends BaseFragment {
    private final ArrayList<TextView> textViews = new ArrayList<>();
    private TextView appearance, start;
    private View art;
    private LinearLayout content;
    private boolean starting;

    @Override public boolean onFragmentCreate() {
        MessagesController.getGlobalMainSettings().edit().putLong("intro_crashed_time", 0).apply();
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();
        LocaleController.getInstance().loadRemoteLanguages(currentAccount);
        return super.onFragmentCreate();
    }
    public IntroActivity setOnLogout() { return this; }

    @Override public View createView(Context context) {
        textViews.clear(); actionBar.setAddToContainer(false); actionBar.setVisibility(View.GONE);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        content = new LinearLayout(context); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(28), AndroidUtilities.statusBarHeight + dp(14), dp(28), AndroidUtilities.navigationBarHeight + dp(20));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fragmentView = scroll;
        LinearLayout top = new LinearLayout(context); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView wordmark = text(context, "ASGRAM", 20, true); wordmark.setLetterSpacing(.025f);
        top.addView(wordmark, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView language = text(context, "Language", 13, true); language.setGravity(Gravity.CENTER); language.setPadding(dp(10),0,dp(10),0);
        language.setContentDescription("Choose language"); language.setOnClickListener(v -> presentFragment(new LanguageSelectActivity().setLanguageOnly(true)));
        top.addView(language, new LinearLayout.LayoutParams(-2, dp(48)));
        appearance = text(context, "", 13, true); appearance.setGravity(Gravity.CENTER); appearance.setPadding(dp(10),0,dp(10),0);
        appearance.setOnClickListener(v -> {
            Theme.ThemeInfo next = Theme.getTheme(Theme.isCurrentThemeDark() ? "Day" : "Dark Blue");
            if (next != null) { Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE; Theme.saveAutoNightThemeConfig(); Theme.applyTheme(next); updateColors(); }
        }); top.addView(appearance,new LinearLayout.LayoutParams(-2,dp(48))); content.addView(top);
        space(context, 0, 1);
        art = new ConversationMark(context); art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        content.addView(art,new LinearLayout.LayoutParams(-1,dp(210)));
        space(context,24,0);
        TextView headline = text(context,"Closer to\nyour people.",36,true); headline.setLineSpacing(dp(1),1f); content.addView(headline,new LinearLayout.LayoutParams(-1,-2));
        space(context,16,0);
        TextView description = text(context,"Your Telegram chats, groups and channels.\nA fresh ASGRAM experience.",16,false); description.setTag("secondary"); description.setLineSpacing(dp(5),1f); content.addView(description,new LinearLayout.LayoutParams(-1,-2));
        space(context,26,0);
        TextView features = text(context,"Chats  ·  Groups  ·  Channels  ·  Calls",13,true); features.setTag("accent"); content.addView(features,new LinearLayout.LayoutParams(-1,-2));
        space(context,0,1);
        space(context,32,0);
        start = text(context,LocaleController.getString(R.string.StartMessaging),16,true); start.setGravity(Gravity.CENTER); start.setMinimumHeight(dp(56));
        start.setOnClickListener(v -> { if (!starting) { starting=true; presentFragment(new LoginActivity()); } });
        content.addView(start,new LinearLayout.LayoutParams(-1,dp(56)));
        space(context,16,0);
        TextView disclosure = text(context,"An unofficial Telegram client\nby Abdii Sabaa Media.",12,false); disclosure.setTag("secondary"); disclosure.setGravity(Gravity.CENTER); disclosure.setLineSpacing(dp(3),1f);
        content.addView(disclosure,new LinearLayout.LayoutParams(-1,-2));
        updateColors(); return fragmentView;
    }
    private TextView text(Context c,String value,int size,boolean bold) {
        TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTypeface(bold?AndroidUtilities.bold():Typeface.DEFAULT);v.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);v.setIncludeFontPadding(false);textViews.add(v);return v;
    }
    private void space(Context context,int height,float weight) { content.addView(new View(context),new LinearLayout.LayoutParams(1,dp(height),weight)); }
    private static int dp(float value) { return AndroidUtilities.dp(value); }
    private void updateColors() {
        if (fragmentView==null) return;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        for(TextView v:textViews) v.setTextColor(Theme.getColor("secondary".equals(v.getTag())?Theme.key_windowBackgroundWhiteGrayText2:"accent".equals(v.getTag())?Theme.key_windowBackgroundWhiteBlueText:Theme.key_windowBackgroundWhiteBlackText));
        int accent=Theme.getColor(Theme.key_featuredStickers_addButton);
        appearance.setText(Theme.isCurrentThemeDark()?"Light":"Dark"); appearance.setContentDescription(Theme.isCurrentThemeDark()?"Switch to light appearance":"Switch to dark appearance");
        appearance.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(24),Theme.getColor(Theme.key_windowBackgroundGray),Theme.getColor(Theme.key_listSelector)));
        start.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(28),accent,Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        start.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText)); art.invalidate();
    }
    @Override public void onResume() { super.onResume();starting=false;updateColors(); }
    @Override public boolean isLightStatusBar() { return !Theme.isCurrentThemeDark(); }
    @Override public ArrayList<ThemeDescription> getThemeDescriptions() {
        return SimpleThemeDescription.createThemeDescriptions(()->updateColors(),Theme.key_windowBackgroundWhite,Theme.key_windowBackgroundWhiteBlackText,Theme.key_featuredStickers_addButton);
    }
    private static final class ConversationMark extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); private final RectF box=new RectF(); private final Path path=new Path();
        ConversationMark(Context context) {super(context);}
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);float scale=Math.min(getWidth()/300f,getHeight()/210f);float left=(getWidth()-300*scale)/2,top=(getHeight()-210*scale)/2;
            canvas.save();canvas.translate(left,top);canvas.scale(scale,scale);
            boolean dark=Theme.isCurrentThemeDark();
            paint.setColor(dark?0xff253f30:0xffe4f1e8);box.set(10,28,240,114);canvas.drawRoundRect(box,28,28,paint);
            paint.setColor(dark?0xff34483b:0xffc7dfce);box.set(115,117,288,178);canvas.drawRoundRect(box,24,24,paint);
            paint.setColor(dark?0xff9bd7b6:0xff176b49);box.set(84,30,204,164);canvas.drawRoundRect(box,32,32,paint);
            path.reset();path.moveTo(111,135);path.lineTo(139,61);path.lineTo(151,61);path.lineTo(179,135);path.lineTo(161,135);path.lineTo(156,119);path.lineTo(133,119);path.lineTo(128,135);path.close();
            paint.setColor(dark?0xff143422:0xffffffff);canvas.drawPath(path,paint);path.reset();path.moveTo(138,104);path.lineTo(151,104);path.lineTo(144.5f,84);path.close();paint.setColor(dark?0xff9bd7b6:0xff176b49);canvas.drawPath(path,paint);
            paint.setColor(dark?0xff9bd7b6:0xff176b49);for(int i=0;i<3;i++)canvas.drawCircle(44+i*18,73,4,paint);
            paint.setColor(dark?0xffb4c5b9:0xff52665a);box.set(220,142,259,147);canvas.drawRoundRect(box,3,3,paint);box.set(220,153,245,158);canvas.drawRoundRect(box,3,3,paint);
            canvas.restore();
        }
    }
}
