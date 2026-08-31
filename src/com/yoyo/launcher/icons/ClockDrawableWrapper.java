package com.yoyo.launcher.icons;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

public class ClockDrawableWrapper extends DrawableWrapper {
    public static boolean sRunningInTest = false;
    public ClockDrawableWrapper(Drawable dr) {
        super(dr);
    }
}
