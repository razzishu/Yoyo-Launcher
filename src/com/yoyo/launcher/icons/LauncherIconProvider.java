/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yoyo.launcher.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.yoyo.launcher.R;
import com.yoyo.launcher.config.FeatureFlags;
import com.yoyo.launcher.dagger.ApplicationContext;
import com.yoyo.launcher.dagger.LauncherAppSingleton;
import com.yoyo.launcher.graphics.ThemeManager;
import com.yoyo.launcher.LauncherPrefs;

import org.xmlpull.v1.XmlPullParser;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

/**
 * Extension of {@link IconProvider} with support for overriding theme icons
 */
@LauncherAppSingleton
public class LauncherIconProvider extends IconProvider {

    private static final String TAG_ICON = "icon";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_DRAWABLE = "drawable";

    private static final String TAG = "LIconProvider";
    private static final Map<String, ThemeData> DISABLED_MAP = Collections.emptyMap();

    private Map<String, ThemeData> mThemedIconMap;

    protected final ThemeManager mThemeManager;
    public final IconPackManager mIconPackManager;
    protected final LauncherPrefs mPrefs;

    @Inject
    public LauncherIconProvider(
            @ApplicationContext Context context,
            ThemeManager themeManager,
            IconPackManager iconPackManager,
            LauncherPrefs prefs) {
        super(context);
        mThemeManager = themeManager;
        mIconPackManager = iconPackManager;
        mPrefs = prefs;
        mThemedIconMap = FeatureFlags.USE_LOCAL_ICON_OVERRIDES.get() ? null : DISABLED_MAP;
    }

    @Override
    public Drawable getIcon(LauncherActivityInfo info, int iconDpi) {
        String iconPack = mPrefs.get(LauncherPrefs.ICON_PACK);
        if (iconPack != null && !iconPack.equals("default") && !iconPack.equals("themed")) {
            Drawable icon = mIconPackManager.getIcon(info.getComponentName(), iconPack);
            if (icon != null) {
                return icon;
            }
        }
        return super.getIcon(info, iconDpi);
    }

    @Override
    public Drawable getIcon(ActivityInfo info) {
        String iconPack = mPrefs.get(LauncherPrefs.ICON_PACK);
        if (iconPack != null && !iconPack.equals("default") && !iconPack.equals("themed")) {
            Drawable icon = mIconPackManager.getIcon(new ComponentName(info.packageName, info.name), iconPack);
            if (icon != null) {
                return icon;
            }
        }
        return super.getIcon(info);
    }

    @Override
    public Drawable getIcon(ApplicationInfo info) {
        String iconPack = mPrefs.get(LauncherPrefs.ICON_PACK);
        if (iconPack != null && !iconPack.equals("default") && !iconPack.equals("themed")) {
            // For application info, we don't have a specific activity, but we can try the main activity
            String pkg = info.packageName;
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null && intent.getComponent() != null) {
                Drawable icon = mIconPackManager.getIcon(intent.getComponent(), iconPack);
                if (icon != null) {
                    return icon;
                }
            }
            // Fallback: try using package name as key if component fails
            Drawable icon = mIconPackManager.getIcon(new ComponentName(pkg, ""), iconPack);
            if (icon != null) {
                return icon;
            }
        }
        return super.getIcon(info);
    }

    @Override
    protected ThemeData getThemeDataForPackage(String packageName) {
        return getThemedIconMap().get(packageName);
    }

    @Override
    public void updateSystemState() {
        super.updateSystemState();
        mSystemState += "," + mThemeManager.getIconState().toUniqueId();
        mSystemState += "," + mPrefs.get(LauncherPrefs.ICON_PACK);
    }

    private Map<String, ThemeData> getThemedIconMap() {
        if (mThemedIconMap != null) {
            return mThemedIconMap;
        }
        ArrayMap<String, ThemeData> map = new ArrayMap<>();
        Resources res = mContext.getResources();
        try (XmlResourceParser parser = res.getXml(R.xml.grayscale_icon_map)) {
            final int depth = parser.getDepth();
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT);

            while (((type = parser.next()) != XmlPullParser.END_TAG
                    || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                if (TAG_ICON.equals(parser.getName())) {
                    String pkg = parser.getAttributeValue(null, ATTR_PACKAGE);
                    int iconId = parser.getAttributeResourceValue(null, ATTR_DRAWABLE, 0);
                    if (iconId != 0 && !TextUtils.isEmpty(pkg)) {
                        map.put(pkg, new ThemeData(res, iconId));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to parse icon map", e);
        }
        mThemedIconMap = map;
        return mThemedIconMap;
    }
}
