package org.telegram.ui;

/*
 ASGRAM direction contract — Operate, native Android.
 Material: white/forest-green surfaces from the Abdii Sabaa identity; readable system type.
 Structure: seven grounded options were a branch directory, chronological newsroom, subject index,
 compact destination list (chosen, seed f9c15e39 index 4), channel magazine, visual media stream,
 and a split reader. The compact directory strengthens destination clarity on small phones.
 The incumbent Telegram client owns messaging conventions; the media hub inherits native controls.
 FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
*/

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.asgram.AsgramUi;

public final class AsgramWelcomeActivity extends BaseFragment {
    @Override public View createView(Context context) {
        AsgramUi.Palette p=new AsgramUi.Palette(context);
        actionBar.setVisibility(View.GONE);
        actionBar.setAddToContainer(false);
        setHasOwnBackground(true);
        FrameLayout root=new FrameLayout(context);root.setBackgroundColor(p.background);
        fragmentView=root;
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        root.addView(scroll,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        LinearLayout content=AsgramUi.column(context);
        content.setPadding(AsgramUi.dp(28),AndroidUtilities.statusBarHeight+AsgramUi.dp(22),AsgramUi.dp(28),AndroidUtilities.navigationBarHeight+AsgramUi.dp(22));
        scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout brand=AsgramUi.row(context);
        brand.addView(new AsgramUi.Glyph(context,"ASGRAM",p.primary),AsgramUi.lp(38,38));
        TextView wordmark=AsgramUi.text(context,"ASGRAM",22,p.text,true);LinearLayout.LayoutParams wordLp=AsgramUi.lp(-2,-2);wordLp.leftMargin=AsgramUi.dp(12);brand.addView(wordmark,wordLp);
        content.addView(brand);
        AsgramUi.gap(content,45);
        TextView title=AsgramUi.text(context,"Your conversations.\nYour community.",35,p.text,true);
        title.setLineSpacing(AsgramUi.dp(3),1f);content.addView(title,AsgramUi.lp(-1,-2));
        AsgramUi.gap(content,15);
        content.addView(AsgramUi.text(context,"Telegram messaging, with Abdii Sabaa closer to you.",17,p.secondary,false),AsgramUi.lp(-1,-2));
        AsgramUi.gap(content,30);
        branch(context,content,p,"News","Abdii Sabaa","News and stories that matter.");
        branch(context,content,p,"Education","Learn & grow","Education updates and student information.");
        branch(context,content,p,"Sports","Stay in the game","The latest from Abdii Sabaa Ispoortii.");
        AsgramUi.gap(content,32);
        content.addView(AsgramUi.button(context,"Continue with Telegram",p,true,()->openLogin(this)),AsgramUi.lp(-1,-2));
        AsgramUi.gap(content,10);
        content.addView(AsgramUi.button(context,"Explore Abdii Sabaa",p,false,()->presentFragment(new AsgramHubActivity())),AsgramUi.lp(-1,-2));
        AsgramUi.gap(content,22);
        TextView disclosure=AsgramUi.text(context,"An independent, unofficial Telegram client\nby Abdii Sabaa Media.",12,p.secondary,false);
        disclosure.setGravity(Gravity.CENTER);content.addView(disclosure,AsgramUi.lp(-1,-2));
        return root;
    }
    private void branch(Context c,LinearLayout parent,AsgramUi.Palette p,String icon,String title,String subtitle) {
        LinearLayout row=AsgramUi.row(c);row.setPadding(0,AsgramUi.dp(10),0,AsgramUi.dp(10));
        AsgramUi.Glyph glyph=new AsgramUi.Glyph(c,icon,p.primary);glyph.setBackground(AsgramUi.shape(p.tint,14));row.addView(glyph,AsgramUi.lp(48,48));
        LinearLayout copy=AsgramUi.column(c);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.leftMargin=AsgramUi.dp(16);
        copy.addView(AsgramUi.text(c,title,16,p.text,true));AsgramUi.gap(copy,5);
        copy.addView(AsgramUi.text(c,subtitle,13,p.secondary,false));row.addView(copy,cp);parent.addView(row,AsgramUi.lp(-1,-2));
    }
    public static void openLogin(BaseFragment owner) {
        if(BuildVars.APP_ID<=0||BuildVars.APP_HASH.length()!=32) {
            new AlertDialog.Builder(owner.getParentActivity()).setTitle("Telegram sign-in is being prepared")
                .setMessage("This test build needs ASGRAM’s registered Telegram app credentials. You can explore the public Abdii Sabaa hub meanwhile.")
                .setPositiveButton("OK",null).show();return;
        }
        owner.presentFragment(new LoginActivity());
    }
    @Override public boolean isLightStatusBar() {return fragmentView!=null&&!new AsgramUi.Palette(fragmentView.getContext()).dark;}
}
