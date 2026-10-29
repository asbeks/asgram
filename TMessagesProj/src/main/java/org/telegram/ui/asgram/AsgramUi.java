package org.telegram.ui.asgram;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;

public final class AsgramUi {
    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("asgram_preferences",Context.MODE_PRIVATE);
    }
    public static final class Palette {
        public final boolean dark;
        public final int background, surface, text, secondary, primary, tint, outline, inverse;
        public Palette(Context c) {
            String mode=prefs(c).getString("appearance","system");
            dark=mode.equals("dark") || (mode.equals("system") && (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
            background=dark?0xff111814:0xfff5f8f5;
            surface=dark?0xff19231d:0xffffffff;
            text=dark?0xffecf3ed:0xff13281c;
            secondary=dark?0xffb4c5b9:0xff52665a;
            primary=dark?0xff9bd7b6:0xff176b49;
            tint=dark?0xff253f30:0xffe4f1e8;
            outline=dark?0xff34483b:0xffdce6dd;
            inverse=dark?0xff143422:0xffffffff;
        }
    }
    public static int dp(float n) { return AndroidUtilities.dp(n); }
    public static GradientDrawable shape(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    public static TextView text(Context c,String value,int size,int color,boolean bold) {
        TextView t=new TextView(c);t.setText(value);t.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
        t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1f);
        return t;
    }
    public static TextView button(Context c,String value,Palette p,boolean filled,Runnable action) {
        TextView v=text(c,value,16,filled?p.inverse:p.primary,true);
        v.setGravity(Gravity.CENTER);v.setPadding(dp(20),dp(14),dp(20),dp(14));v.setMinHeight(dp(52));
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(p.dark?0x33ffffff:0x20000000),shape(filled?p.primary:p.tint,16),null));
        v.setOnClickListener(x->action.run());v.setContentDescription(value);return v;
    }
    public static LinearLayout column(Context c) {
        LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;
    }
    public static LinearLayout row(Context c) {
        LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    public static LinearLayout.LayoutParams lp(int width,int height) { return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height)); }
    public static void gap(LinearLayout v,int height) { View gap=new View(v.getContext());v.addView(gap,lp(1,height)); }
    public static void divider(LinearLayout v,Palette p) { View line=new View(v.getContext());line.setBackgroundColor(p.outline);v.addView(line,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1))); }
    public static final class Glyph extends View {
        private final String kind; private final int color; private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        public Glyph(Context c,String kind,int color) { super(c);this.kind=kind;this.color=color;setContentDescription(kind); }
        @Override protected void onDraw(Canvas raw) {
            super.onDraw(raw);Canvas c=raw;int save=c.save();
            float size=Math.min(getWidth(),getHeight()),s=size/48f;
            c.translate((getWidth()-size)/2f,(getHeight()-size)/2f);c.scale(s,s);
            paint.setColor(color);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);
            Path path=new Path();
            switch(kind) {
                case "ASGRAM":
                    paint.setStyle(Paint.Style.FILL);c.drawRoundRect(0,0,48,48,14,14,paint);
                    paint.setColor(0xffffffff);path.moveTo(11,35);path.lineTo(20,12);path.lineTo(27,12);path.lineTo(36,35);path.lineTo(29,35);path.lineTo(27,29);path.lineTo(19,29);path.lineTo(17,35);path.close();c.drawPath(path,paint);
                    paint.setColor(0xff176b49);path.reset();path.moveTo(21,24);path.lineTo(25,24);path.lineTo(23,18);path.close();c.drawPath(path,paint);break;
                case "Search": c.drawCircle(22,22,7,paint);c.drawLine(27,27,33,33,paint);break;
                case "Saved": path.moveTo(17,14);path.lineTo(31,14);path.lineTo(31,34);path.lineTo(24,29);path.lineTo(17,34);path.close();c.drawPath(path,paint);break;
                case "News": c.drawRoundRect(14,13,34,35,3,3,paint);c.drawLine(19,19,29,19,paint);c.drawLine(19,24,29,24,paint);c.drawLine(19,29,26,29,paint);break;
                case "Education": path.moveTo(11,20);path.lineTo(24,14);path.lineTo(37,20);path.lineTo(24,26);path.close();c.drawPath(path,paint);c.drawLine(16,23,16,30,paint);c.drawLine(16,30,24,34,paint);c.drawLine(24,34,32,30,paint);c.drawLine(32,30,32,23,paint);break;
                case "Sports": c.drawRoundRect(17,13,31,27,3,3,paint);c.drawLine(24,27,24,34,paint);c.drawLine(18,35,30,35,paint);c.drawArc(11,14,22,26,90,180,false,paint);c.drawArc(26,14,37,26,270,180,false,paint);break;
                case "Share":c.drawCircle(16,24,3,paint);c.drawCircle(31,15,3,paint);c.drawCircle(31,33,3,paint);c.drawLine(19,22,28,16,paint);c.drawLine(19,26,28,32,paint);break;
                case "Settings": c.drawCircle(24,24,7,paint);for(int a=0;a<8;a++){double r=a*Math.PI/4;c.drawLine(24+(float)Math.cos(r)*10,24+(float)Math.sin(r)*10,24+(float)Math.cos(r)*13,24+(float)Math.sin(r)*13,paint);}break;
                case "Home":path.moveTo(12,23);path.lineTo(24,13);path.lineTo(36,23);c.drawPath(path,paint);c.drawRoundRect(16,22,32,35,2,2,paint);break;
                case "Chats":c.drawRoundRect(12,14,36,31,6,6,paint);c.drawLine(17,31,17,36,paint);c.drawLine(17,36,23,31,paint);break;
                case "Refresh":c.drawArc(15,15,33,33,40,290,false,paint);path.moveTo(33,15);path.lineTo(33,22);path.lineTo(27,20);c.drawPath(path,paint);break;
                default:c.drawLine(17,24,31,24,paint);c.drawLine(26,19,31,24,paint);c.drawLine(26,29,31,24,paint);
            }
            c.restoreToCount(save);
        }
    }
    private AsgramUi() {}
}
