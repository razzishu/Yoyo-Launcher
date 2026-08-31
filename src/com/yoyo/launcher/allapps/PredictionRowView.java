/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.yoyo.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yoyo.launcher.BubbleTextView;
import com.yoyo.launcher.DeviceProfile;
import com.yoyo.launcher.LauncherPrefs;
import com.yoyo.launcher.R;
import com.yoyo.launcher.model.BgDataModel.FixedContainerItems;
import com.yoyo.launcher.model.data.ItemInfo;
import com.yoyo.launcher.model.data.WorkspaceItemInfo;
import com.yoyo.launcher.util.LauncherBindableItemsContainer;
import com.yoyo.launcher.views.ActivityContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A row of app predictions in the all-apps view.
 */
public class PredictionRowView extends LinearLayout implements FloatingHeaderRow,
        LauncherBindableItemsContainer {

    private final ActivityContext mActivityContext;
    private final List<WorkspaceItemInfo> mPredictedApps = new ArrayList<>();
    private FloatingHeaderView mParent;

    private boolean mTabsHidden;

    public PredictionRowView(@NonNull Context context) {
        this(context, null);
    }

    public PredictionRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mActivityContext = ActivityContext.lookupContext(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.TOP);
    }

    @Override
    public void setup(FloatingHeaderView parent, FloatingHeaderRow[] allRows, boolean tabsHidden) {
        mParent = parent;
        mTabsHidden = tabsHidden;
        updateVisibility();
    }

    @Override
    public int getExpectedHeight() {
        return isVisible() ? (mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx() 
                + getPaddingTop() + getPaddingBottom()) : 0;
    }

    @Override
    public boolean shouldDraw() {
        return !mPredictedApps.isEmpty() && LauncherPrefs.get(getContext()).get(LauncherPrefs.SUGGESTIONS_ALL_APPS);
    }

    @Override
    public boolean hasVisibleContent() {
        return shouldDraw();
    }

    @Override
    public void setVerticalScroll(int scroll, boolean isScrolledOut) {
        if (!isVisible()) {
            return;
        }
        setTranslationY(scroll);
        setAlpha(isScrolledOut ? 0 : 1);
    }

    @Override
    public Class<? extends FloatingHeaderRow> getTypeClass() {
        return PredictionRowView.class;
    }

    @Override
    public View getFocusedChild() {
        return getChildAt(0);
    }

    public void bindExtraContainerItems(FixedContainerItems item) {
        if (item.containerId == CONTAINER_ALL_APPS_PREDICTION) {
            boolean prefEnabled = LauncherPrefs.get(getContext()).get(LauncherPrefs.SUGGESTIONS_ALL_APPS);
            Log.d("PredictionRowView", "Binding " + item.items.size() + " apps. Pref enabled: " + prefEnabled);
            mPredictedApps.clear();
            for (ItemInfo info : item.items) {
                if (info instanceof WorkspaceItemInfo) {
                    mPredictedApps.add((WorkspaceItemInfo) info);
                }
            }
            rebindItems();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpec = heightMeasureSpec;
        if (isVisible()) {
            int height = mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx()
                    + getPaddingTop() + getPaddingBottom();
            heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightSpec);
    }

    private void rebindItems() {
        removeAllViews();
        if (mPredictedApps.isEmpty()) {
            updateVisibility();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        DeviceProfile dp = mActivityContext.getDeviceProfile();
        int count = Math.min(mPredictedApps.size(), dp.numShownAllAppsColumns);

        for (int i = 0; i < count; i++) {
            BubbleTextView icon = (BubbleTextView) inflater.inflate(
                    R.layout.all_apps_prediction_row_icon, this, false);
            WorkspaceItemInfo info = mPredictedApps.get(i);
            icon.applyFromWorkspaceItem(info);
            icon.setOnClickListener(mActivityContext.getItemOnClickListener());
            icon.setOnLongClickListener(mActivityContext.getAllAppsItemLongClickListener());
            
            LayoutParams lp = (LayoutParams) icon.getLayoutParams();
            lp.width = 0;
            lp.weight = 1;
            addView(icon);
        }
        updateVisibility();
    }

    private void updateVisibility() {
        setVisibility(shouldDraw() ? VISIBLE : GONE);
        if (mParent != null) {
            mParent.onHeightUpdated();
        }
    }

    public void setInsets(Rect insets) {
        DeviceProfile dp = mActivityContext.getDeviceProfile();
        setPadding(dp.allAppsLeftRightMargin, getPaddingTop(), dp.allAppsLeftRightMargin,
                getPaddingBottom());
    }

    @Nullable
    @Override
    public View mapOverItems(ItemOperator op) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (op.evaluate((ItemInfo) child.getTag(), child)) {
                return child;
            }
        }
        return null;
    }
}
