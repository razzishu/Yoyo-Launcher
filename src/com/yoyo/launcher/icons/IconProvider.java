package com.yoyo.launcher.icons;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Ported/Stubbed IconProvider for standalone build.
 */
public class IconProvider {
    private static final String TAG = "IconProvider";
    protected final Context mContext;
    protected String mSystemState = "";

    public IconProvider(Context context) {
        mContext = context;
    }

    public Drawable getIcon(ActivityInfo info) {
        return info.loadIcon(mContext.getPackageManager());
    }

    public Drawable getIcon(ApplicationInfo info) {
        return info.loadIcon(mContext.getPackageManager());
    }

    public Drawable getIcon(LauncherActivityInfo info, int iconDpi) {
        try {
            return info.getIcon(iconDpi);
        } catch (Exception e) {
            Log.e(TAG, "Error loading icon for " + info.getComponentName(), e);
            return mContext.getPackageManager().getDefaultActivityIcon();
        }
    }

    public String getSystemStateForPackage(String systemState, String packageName) {
        return systemState;
    }

    public Drawable getDefaultIcon(int iconDpi) {
        return mContext.getPackageManager().getDefaultActivityIcon();
    }

    public String getStateForApp(ApplicationInfo info) {
        return "";
    }

    protected ThemeData getThemeDataForPackage(String packageName) {
        return null;
    }

    public void updateSystemState() {}

    public static class ThemeData {
        public ThemeData(Resources res, int iconId) {}
    }
}
