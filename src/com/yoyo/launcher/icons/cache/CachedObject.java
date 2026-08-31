package com.yoyo.launcher.icons.cache;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.yoyo.launcher.icons.IconProvider;

public interface CachedObject {
    @NonNull ComponentName getComponent();
    @NonNull UserHandle getUser();
    @Nullable CharSequence getLabel();
    @Nullable ApplicationInfo getApplicationInfo();
    
    default @Nullable Drawable getFullResIcon(BaseIconCache cache) {
        return null;
    }

    default @Nullable String getFreshnessIdentifier(IconProvider iconProvider) {
        return null;
    }
}
