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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.app.animation.Interpolators;
import com.yoyo.launcher.BubbleTextView;
import com.yoyo.launcher.DeviceProfile;
import com.yoyo.launcher.LauncherPrefChangeListener;
import com.yoyo.launcher.LauncherPrefs;
import com.yoyo.launcher.R;
import com.yoyo.launcher.Utilities;
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
        setGravity(Gravity.CENTER);
    }

    private final LauncherPrefChangeListener mPrefListener = key -> {
        if (LauncherPrefs.SUGGESTIONS_ALL_APPS.getSharedPrefKey().equals(key)) {
            post(this::onSuggestionsPrefChanged);
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LauncherPrefs.get(getContext()).addListener(mPrefListener, LauncherPrefs.SUGGESTIONS_ALL_APPS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LauncherPrefs.get(getContext()).removeListener(mPrefListener, LauncherPrefs.SUGGESTIONS_ALL_APPS);
    }

    public void onSuggestionsPrefChanged() {
        boolean enabled = LauncherPrefs.get(getContext()).get(LauncherPrefs.SUGGESTIONS_ALL_APPS);
        if (!enabled) {
            setVisibility(GONE);
            if (mParent != null) {
                mParent.onHeightUpdated();
            }
        } else {
            if (!mPredictedApps.isEmpty()) {
                setVisibility(VISIBLE);
                if (mParent != null) {
                    mParent.onHeightUpdated();
                }
            }
        }
    }

    @Override
    public void setup(FloatingHeaderView parent, FloatingHeaderRow[] allRows, boolean tabsHidden) {
        mParent = parent;
        mTabsHidden = tabsHidden;
        updateVisibility();
    }

    @Override
    public int getExpectedHeight() {
        if (!isVisible()) {
            return 0;
        }
        DeviceProfile dp = mActivityContext.getDeviceProfile();
        int iconSize = dp.getAllAppsProfile().getIconSizePx();
        int textHeight = Utilities.calculateTextHeight(dp.getAllAppsProfile().getIconTextSizePx());
        int drawablePadding = dp.getAllAppsProfile().getIconDrawablePaddingPx();
        int verticalPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());
        return iconSize + drawablePadding + textHeight + (verticalPadding * 2);
    }

    @Override
    public boolean shouldDraw() {
        return !mPredictedApps.isEmpty() && LauncherPrefs.get(getContext()).get(LauncherPrefs.SUGGESTIONS_ALL_APPS);
    }

    @Override
    public boolean hasVisibleContent() {
        return shouldDraw();
    }

    private ObjectAnimator mHideRevealAnimator;

    public void animateHide() {
        if (mHideRevealAnimator != null) {
            mHideRevealAnimator.cancel();
        }
        if (getVisibility() != VISIBLE) {
            return;
        }
        PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, getAlpha(), 0f);
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, getScaleX(), 0.82f);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, getScaleY(), 0.82f);
        PropertyValuesHolder pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, getTranslationY(), -Utilities.dpToPx(16));

        mHideRevealAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvhAlpha, pvhScaleX, pvhScaleY, pvhTransY);
        mHideRevealAnimator.setDuration(240);
        mHideRevealAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mHideRevealAnimator.addUpdateListener(animation -> {
            if (mParent != null && mParent.getParent() instanceof View) {
                ((View) mParent.getParent()).invalidate();
            }
        });
        mHideRevealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(GONE);
                mHideRevealAnimator = null;
            }
        });
        mHideRevealAnimator.start();
    }

    public void animateReveal() {
        if (mHideRevealAnimator != null) {
            mHideRevealAnimator.cancel();
        }
        if (!shouldDraw()) {
            return;
        }
        setVisibility(VISIBLE);
        setAlpha(0f);
        setScaleX(0.82f);
        setScaleY(0.82f);
        setTranslationY(-Utilities.dpToPx(16));

        PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f);
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.82f, 1f);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.82f, 1f);
        PropertyValuesHolder pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -Utilities.dpToPx(16), 0f);

        mHideRevealAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvhAlpha, pvhScaleX, pvhScaleY, pvhTransY);
        mHideRevealAnimator.setDuration(320);
        mHideRevealAnimator.setInterpolator(Interpolators.EMPHASIZED_DECELERATE);
        mHideRevealAnimator.addUpdateListener(animation -> {
            if (mParent != null && mParent.getParent() instanceof View) {
                ((View) mParent.getParent()).invalidate();
            }
        });
        mHideRevealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setAlpha(1f);
                setScaleX(1f);
                setScaleY(1f);
                setTranslationY(0f);
                mHideRevealAnimator = null;
            }
        });
        mHideRevealAnimator.start();
    }

    @Override
    public void setVerticalScroll(int scroll, boolean isScrolledOut) {
        if (!isVisible() && !isScrolledOut) {
            return;
        }
        if (mHideRevealAnimator != null && mHideRevealAnimator.isRunning()) {
            return;
        }
        int clampedScroll = Math.min(0, scroll);
        if (isScrolledOut) {
            setAlpha(0f);
            setScaleX(0.85f);
            setScaleY(0.85f);
            setTranslationY(clampedScroll);
        } else {
            int height = getExpectedHeight();
            float progress = height > 0 ? Math.min(1f, Math.max(0f, (float) -clampedScroll / height)) : 0f;
            float smoothAlpha = (float) Math.pow(1f - progress, 1.4);
            float smoothScale = 1f - (0.15f * progress);
            setAlpha(smoothAlpha);
            setScaleX(smoothScale);
            setScaleY(smoothScale);
            setTranslationY(clampedScroll);
        }
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
            int height = getExpectedHeight();
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
            lp.height = LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
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
        int sideMargin = getResources().getDimensionPixelSize(R.dimen.all_apps_list_margin_x);
        int sidePadding = Math.max(0, dp.allAppsPadding.left - sideMargin);
        setPadding(sidePadding, 0, sidePadding, 0);
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
