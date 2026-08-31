package com.yoyo.launcher.icons;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * Ported/Stubbed MonoChromeIconDrawable for standalone build.
 */
public class MonoChromeIconDrawable extends Drawable {

    private final Drawable mBackingDrawable;

    public MonoChromeIconDrawable(Drawable backingDrawable) {
        mBackingDrawable = backingDrawable;
    }

    @Override
    public void draw(Canvas canvas) {
        mBackingDrawable.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        mBackingDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mBackingDrawable.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
