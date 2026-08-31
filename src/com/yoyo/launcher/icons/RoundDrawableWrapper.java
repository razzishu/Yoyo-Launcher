package com.yoyo.launcher.icons;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

public class RoundDrawableWrapper extends DrawableWrapper {
    public RoundDrawableWrapper(Drawable dr) {
        super(dr);
    }
    public RoundDrawableWrapper(Drawable dr, float radius) {
        super(dr);
    }
    public void setCornerRadiusScale(float scale) {}
}
