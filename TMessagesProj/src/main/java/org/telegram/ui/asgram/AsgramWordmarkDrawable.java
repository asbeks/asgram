package org.telegram.ui.asgram;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
/** ASGRAM wordmark; no official Telegram app branding. */
public final class AsgramWordmarkDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public AsgramWordmarkDrawable(){paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setColor(0xffffffff);}
    @Override public void draw(Canvas canvas){float height=getBounds().height();paint.setTextSize(height*.88f);paint.setTextScaleX(1f);float width=paint.measureText("ASGRAM");if(width>getBounds().width())paint.setTextScaleX(getBounds().width()/width);canvas.drawText("ASGRAM",getBounds().left,getBounds().top+(height-paint.ascent()-paint.descent())/2f,paint);}
    @Override public int getIntrinsicWidth(){return AsgramUi.dp(90);}
    @Override public int getIntrinsicHeight(){return AsgramUi.dp(22);}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
