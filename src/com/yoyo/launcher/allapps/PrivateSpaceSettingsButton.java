/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.yoyo.launcher.allapps;

import static com.yoyo.launcher.LauncherSettings.Favorites.CONTAINER_PRIVATESPACE;
import static com.yoyo.launcher.logging.StatsLogManager.LauncherEvent.LAUNCHER_PRIVATE_SPACE_SETTINGS_TAP;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.yoyo.launcher.logging.StatsLogManager;
import com.yoyo.launcher.model.data.AppInfo;
import com.yoyo.launcher.util.ApiWrapper;
import com.yoyo.launcher.views.ActivityContext;

public class PrivateSpaceSettingsButton extends ImageButton implements View.OnClickListener {

    private final ActivityContext mActivityContext;
    private final StatsLogManager mStatsLogManager;
    private final Intent mPrivateSpaceSettingsIntent;

    public PrivateSpaceSettingsButton(Context context) {
        this(context, null, 0);
    }

    public PrivateSpaceSettingsButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PrivateSpaceSettingsButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivityContext = ActivityContext.lookupContext(context);
        mStatsLogManager = mActivityContext.getStatsLogManager();
        mPrivateSpaceSettingsIntent =
                ApiWrapper.INSTANCE.get(context).getPrivateSpaceSettingsIntent();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        mStatsLogManager.logger().log(LAUNCHER_PRIVATE_SPACE_SETTINGS_TAP);
        AppInfo privateSpaceSettingsItemInfo = createPrivateSpaceSettingsAppInfo();
        view.setTag(privateSpaceSettingsItemInfo);
        mActivityContext.startActivitySafely(
                view,
                mPrivateSpaceSettingsIntent,
                privateSpaceSettingsItemInfo);
    }

    AppInfo createPrivateSpaceSettingsAppInfo() {
        AppInfo itemInfo = new AppInfo();
        itemInfo.id = CONTAINER_PRIVATESPACE;
        if (mPrivateSpaceSettingsIntent != null) {
            itemInfo.componentName = mPrivateSpaceSettingsIntent.getComponent();
        }
        itemInfo.container = CONTAINER_PRIVATESPACE;
        return itemInfo;
    }
}
