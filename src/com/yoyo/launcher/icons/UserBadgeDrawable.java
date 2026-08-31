package com.yoyo.launcher.icons;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

/**
 * Ported/Stubbed UserBadgeDrawable for standalone build.
 */
public class UserBadgeDrawable extends DrawableWrapper {

    public UserBadgeDrawable(Context context, int iconRes, int colorRes, boolean isThemed) {
        super(context.getDrawable(iconRes));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
}
