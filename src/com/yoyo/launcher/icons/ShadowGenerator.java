package com.yoyo.launcher.icons;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

/**
 * Utility class to add shadows to bitmaps.
 */
public class ShadowGenerator {
    public static final float ICON_SCALE_FOR_SHADOWS = 0.98f;
    
    private static final float BLUR_FACTOR = 1.68f / 48;
    private static final float KEY_SHADOW_DISTANCE = 1f / 48;
    private static final int AMBIENT_SHADOW_ALPHA = 25;
    private static final int KEY_SHADOW_ALPHA = 7;

    private final int mIconSize;
    private final Paint mBlurPaint;
    private final Paint mDrawPaint;

    public ShadowGenerator(int iconSize) {
        mIconSize = iconSize;
        mBlurPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mBlurPaint.setMaskFilter(new BlurMaskFilter(mIconSize * BLUR_FACTOR, BlurMaskFilter.Blur.NORMAL));
        mDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    }

    public synchronized void drawShadow(Bitmap icon, Canvas out) {
        if (icon == null) return;
        int[] offset = new int[2];
        Bitmap shadow = icon.extractAlpha(mBlurPaint, offset);
        if (shadow == null) return;

        mDrawPaint.setAlpha(AMBIENT_SHADOW_ALPHA);
        out.drawBitmap(shadow, offset[0], offset[1], mDrawPaint);

        mDrawPaint.setAlpha(KEY_SHADOW_ALPHA);
        out.drawBitmap(shadow, offset[0], offset[1] + mIconSize * KEY_SHADOW_DISTANCE, mDrawPaint);
    }
    
    public static class Builder {
        public final RectF bounds = new RectF();
        public int color;
        public float shadowBlur;
        public float keyShadowDistance;
        public int keyShadowAlpha;
        public int ambientShadowAlpha;

        public Builder(int color) {
            this.color = color;
        }

        public void drawShadow(Canvas canvas) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            
            p.setShadowLayer(shadowBlur, 0, keyShadowDistance, 
                GraphicsUtils.setColorAlphaBound(Color.BLACK, keyShadowAlpha));
            canvas.drawRoundRect(bounds, bounds.height() / 2, bounds.height() / 2, p);
            
            p.setShadowLayer(shadowBlur, 0, 0, 
                GraphicsUtils.setColorAlphaBound(Color.BLACK, ambientShadowAlpha));
            canvas.drawRoundRect(bounds, bounds.height() / 2, bounds.height() / 2, p);
            
            if (Color.alpha(color) < 255) {
                p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                p.setShadowLayer(0, 0, 0, 0);
                canvas.drawRoundRect(bounds, bounds.height() / 2, bounds.height() / 2, p);
            }
        }
    }
}
