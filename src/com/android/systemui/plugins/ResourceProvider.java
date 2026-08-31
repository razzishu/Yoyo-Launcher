package com.android.systemui.plugins;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.FractionRes;
import androidx.annotation.IntegerRes;

public interface ResourceProvider extends Plugin {
    int getInt(@IntegerRes int resId);
    float getFraction(@FractionRes int resId);
    float getDimension(@DimenRes int resId);
    int getColor(@ColorRes int resId);
    float getFloat(@DimenRes int resId);
}
