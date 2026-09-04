/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.yoyo.launcher.allapps.FloatingHeaderRow.NO_ROWS;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.yoyo.launcher.Flags;
import com.yoyo.launcher.Insettable;
import com.yoyo.launcher.R;
import com.yoyo.launcher.allapps.ActivityAllAppsContainerView.AdapterHolder;
import com.yoyo.launcher.util.PluginManagerWrapper;
import com.yoyo.launcher.views.ActivityContext;
import com.yoyo.launcher.workprofile.PersonalWorkSlidingTabStrip;
import com.android.systemui.plugins.AllAppsRow;
import com.android.systemui.plugins.AllAppsRow.OnHeightUpdatedListener;
import com.android.systemui.plugins.PluginListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class FloatingHeaderView extends LinearLayout implements
        ValueAnimator.AnimatorUpdateListener, PluginListener<AllAppsRow>, Insettable,
        OnHeightUpdatedListener {

    private final Rect mRVClip = new Rect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private final Rect mHeaderClip = new Rect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private final ValueAnimator mAnimator = ValueAnimator.ofInt(0, 0);
    private final Point mTempOffset = new Point();
    private final RecyclerView.OnScrollListener mOnScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {}

                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    if (rv != mCurrentRV) {
                        return;
                    }

                    if (mAnimator.isStarted()) {
                        mAnimator.cancel();
                    }

                    int current = -Math.max(0, mCurrentRV.computeVerticalScrollOffset());
                    boolean headerCollapsed = mHeaderCollapsed;
                    moved(current);
                    applyVerticalMove();
                    if (headerCollapsed != mHeaderCollapsed) {
                        ActivityAllAppsContainerView<?> parent =
                                (ActivityAllAppsContainerView<?>) getParent();
                        parent.invalidateHeader();
                    }
                }
            };

    protected final Map<AllAppsRow, PluginHeaderRow> mPluginRows = new ArrayMap<>();

    // These two values are necessary to ensure that the header protection is drawn correctly.
    private final int mTabsAdditionalPaddingTop;
    private final int mTabsAdditionalPaddingBottom;

    protected PersonalWorkSlidingTabStrip mTabLayout;
    private AllAppsRecyclerView mMainRV;
    private AllAppsRecyclerView mWorkRV;
    private SearchRecyclerView mSearchRV;
    private AllAppsRecyclerView mCurrentRV;
    protected int mSnappedScrolledY;
    private int mTranslationY;

    private boolean mForwardToRecyclerView;

    protected boolean mTabsHidden;
    protected int mMaxTranslation;

    // Whether the header has been scrolled off-screen.
    private boolean mHeaderCollapsed;
    // Whether floating rows like predicted apps are hidden.
    private boolean mFloatingRowsCollapsed;
    // Total height of all current floating rows. Collapsed rows == 0 height.
    private int mFloatingRowsHeight;
    // Offset of search bar. Adds to the floating view height when multi-line is supported.
    private int mSearchBarOffset = 0;

    // This is initialized once during inflation and stays constant after that. Fixed views
    // cannot be added or removed dynamically.
    private FloatingHeaderRow[] mFixedRows = NO_ROWS;

    // Array of all fixed rows and plugin rows. This is initialized every time a plugin is
    // enabled or disabled, and represent the current set of all rows.
    private FloatingHeaderRow[] mAllRows = NO_ROWS;

    public FloatingHeaderView(@NonNull Context context) {
        this(context, null);
    }

    public FloatingHeaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mTabsAdditionalPaddingTop = context.getResources()
                .getDimensionPixelSize(R.dimen.all_apps_header_top_adjustment);
        mTabsAdditionalPaddingBottom = context.getResources()
                .getDimensionPixelSize(R.dimen.all_apps_header_bottom_adjustment);
        mAnimator.addUpdateListener(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTabLayout = findViewById(R.id.tabs);

        // Find all floating header rows.
        ArrayList<FloatingHeaderRow> rows = new ArrayList<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof FloatingHeaderRow) {
                rows.add((FloatingHeaderRow) child);
            }
        }
        mFixedRows = rows.toArray(new FloatingHeaderRow[rows.size()]);
        mAllRows = mFixedRows;
        updateFloatingRowsHeight();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        PluginManagerWrapper.INSTANCE.get(getContext()).addPluginListener(this,
                AllAppsRow.class, true /* allowMultiple */);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PluginManagerWrapper.INSTANCE.get(getContext()).removePluginListener(this);
    }

    private void recreateAllRowsArray() {
        int pluginCount = mPluginRows.size();
        if (pluginCount == 0) {
            mAllRows = mFixedRows;
        } else {
            int count = mFixedRows.length;
            mAllRows = new FloatingHeaderRow[count + pluginCount];
            for (int i = 0; i < count; i++) {
                mAllRows[i] = mFixedRows[i];
            }

            for (PluginHeaderRow row : mPluginRows.values()) {
                mAllRows[count] = row;
                count++;
            }
        }
        updateFloatingRowsHeight();
    }

    @Override
    public void onPluginConnected(AllAppsRow allAppsRowPlugin, Context context) {
        if (mPluginRows.containsKey(allAppsRowPlugin)) {
            // Plugin has already been connected
            return;
        }
        PluginHeaderRow headerRow = new PluginHeaderRow(allAppsRowPlugin, this);
        addView(headerRow.mView, indexOfChild(mTabLayout));
        mPluginRows.put(allAppsRowPlugin, headerRow);
        recreateAllRowsArray();
        allAppsRowPlugin.setOnHeightUpdatedListener(this);
    }

    @Override
    public void onHeightUpdated() {
        int oldMaxHeight = mMaxTranslation;
        updateExpectedHeight();

        if (mMaxTranslation != oldMaxHeight || mFloatingRowsCollapsed) {
            ActivityAllAppsContainerView parent = (ActivityAllAppsContainerView) getParent();
            if (parent != null) {
                parent.setupHeader();
            }
        }
    }

    /**
     * Offset floating rows height by search bar
     */
    void updateSearchBarOffset(int offset) {
        mSearchBarOffset = offset;
        onHeightUpdated();
    }

    @Override
    public void onPluginDisconnected(AllAppsRow plugin) {
        PluginHeaderRow row = mPluginRows.get(plugin);
        if (row == null) {
            return;
        }
        removeView(row.mView);
        mPluginRows.remove(plugin);
        recreateAllRowsArray();
        onHeightUpdated();
    }

    @Override
    public View getFocusedChild() {
        for (FloatingHeaderRow row : mAllRows) {
            if (row.hasVisibleContent() && row.isVisible()) {
                return row.getFocusedChild();
            }
        }
        return null;
    }

    void setup(AllAppsRecyclerView mainRV, AllAppsRecyclerView workRV, SearchRecyclerView searchRV,
            int activeRV, boolean tabsHidden) {
        for (FloatingHeaderRow row : mAllRows) {
            row.setup(this, mAllRows, tabsHidden);
        }

        mTabsHidden = tabsHidden;
        maybeSetTabVisibility(VISIBLE);
        mMainRV = mainRV;
        mWorkRV = workRV;
        mSearchRV = searchRV;
        setActiveRV(activeRV);
        reset(false);
    }

    /** Whether this header has been set up previously. */
    boolean isSetUp() {
        return mMainRV != null;
    }

    /** Set the active AllApps RV which will adjust the alpha of the header when scrolled. */
    void setActiveRV(int rvType) {
        if (mCurrentRV != null) {
            mCurrentRV.removeOnScrollListener(mOnScrollListener);
        }
        mCurrentRV =
                rvType == AdapterHolder.MAIN ? mMainRV
                : rvType == AdapterHolder.WORK ? mWorkRV : mSearchRV;
        mCurrentRV.addOnScrollListener(mOnScrollListener);
        maybeSetTabVisibility(rvType == AdapterHolder.SEARCH ? GONE : VISIBLE);

        updateExpectedHeight();
    }

    /** Update tab visibility to the given state, only if tabs are active (work profile exists). */
    void maybeSetTabVisibility(int visibility) {
        mTabLayout.setVisibility(mTabsHidden ? GONE : visibility);
    }

    private void updateExpectedHeight() {
        updateFloatingRowsHeight();
        mMaxTranslation = 0;
        boolean shouldAddSearchBarHeight = mSearchBarOffset > 0 && !Flags.floatingSearchBar();
        if (shouldAddSearchBarHeight) {
            mMaxTranslation += mSearchBarOffset;
        }
        if (mFloatingRowsCollapsed) {
            return;
        }
        if (Flags.floatingSearchBar() && mFloatingRowsHeight > 0) {
            int gap = getResources().getDimensionPixelSize(R.dimen.all_apps_suggestions_bottom_gap);
            mMaxTranslation += mFloatingRowsHeight + gap;
        } else {
            mMaxTranslation += mFloatingRowsHeight;
        }

        // Add tab height if visible, as it's part of the scrollable header area
        if (!mTabsHidden && mTabLayout != null) {
            mMaxTranslation += getResources().getDimensionPixelSize(R.dimen.all_apps_header_pill_height)
                    + getResources().getDimensionPixelSize(R.dimen.all_apps_tabs_margin_top);
        }
    }

    int getMaxTranslation() {
        if (mMaxTranslation == 0 && (mTabsHidden || mFloatingRowsCollapsed)) {
            return Flags.floatingSearchBar() ? getPaddingTop()
                    : getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_bottom_padding);
        } else if (mMaxTranslation > 0) {
            return mMaxTranslation + getPaddingTop();
        } else {
            return mMaxTranslation;
        }
    }

    private boolean canSnapAt(int currentScrollY) {
        return Math.abs(currentScrollY) <= mMaxTranslation;
    }

    private void moved(final int currentScrollY) {
        final int clampedScrollY = Math.min(0, currentScrollY);
        // if we are snapping we shouldn't translate
        if (mSnappedScrolledY != -mMaxTranslation && !canSnapAt(clampedScrollY)) {
            return;
        }

        if (mMaxTranslation == 0) {
            mTranslationY = 0;
            return;
        }

        if (mFloatingRowsCollapsed) {
            mTranslationY = -mMaxTranslation;
            return;
        }

        if (mTabsHidden) {
            if (clampedScrollY <= -mMaxTranslation) {
                mHeaderCollapsed = true;
            } else {
                mHeaderCollapsed = false;
            }
            mTranslationY = clampedScrollY;
        } else {
            mTranslationY = clampedScrollY - mSnappedScrolledY - mMaxTranslation;

            // update state vars
            if (mTranslationY >= 0) { // expanded: must not move down further
                mTranslationY = 0;
                mSnappedScrolledY = clampedScrollY - mMaxTranslation;
            } else if (mTranslationY <= -mMaxTranslation) { // hide or stay hidden
                mHeaderCollapsed = true;
                mSnappedScrolledY = -mMaxTranslation;
            }
        }
    }

    protected void applyVerticalMove() {
        int uncappedTranslationY = Math.min(0, mTranslationY);
        mTranslationY = Math.min(0, Math.max(mTranslationY, -mMaxTranslation));

        if (mFloatingRowsCollapsed || uncappedTranslationY < mTranslationY - getPaddingTop()) {
            // we hide it completely if already capped (for opening search anim)
            for (FloatingHeaderRow row : mAllRows) {
                row.setVerticalScroll(0, true /* isScrolledOut */);
            }
        } else {
            for (FloatingHeaderRow row : mAllRows) {
                row.setVerticalScroll(uncappedTranslationY, false /* isScrolledOut */);
            }
        }

        mTabLayout.setTranslationY(mTranslationY);

        int clipTop = getPaddingTop() - mTabsAdditionalPaddingTop;
        if (mTabsHidden) {
            // Add back spacing that is otherwise covered by the tabs.
            clipTop += mTabsAdditionalPaddingTop;
        }

        int top = getTop() > 0 ? getTop()
                : ActivityContext.lookupContext(getContext()).getDeviceProfile().getInsets().top;
        int gap = getResources().getDimensionPixelSize(R.dimen.all_apps_suggestions_bottom_gap);
        int initialCardTop = mFloatingRowsHeight > 0 ? (top + mFloatingRowsHeight + gap) : top;
        int currentCardTop = Math.min(initialCardTop, Math.max(top, initialCardTop + mTranslationY));

        if (getParent() instanceof ActivityAllAppsContainerView && Flags.floatingSearchBar()) {
            ((ActivityAllAppsContainerView<?>) getParent()).setCardTop(currentCardTop);
        }

        mRVClip.left = 0;
        mRVClip.top = Flags.floatingSearchBar() ? Math.max(0, currentCardTop - top) : (mTabsHidden || mFloatingRowsCollapsed ? clipTop : 0);
        mHeaderClip.top = clipTop;
        // clipping on a draw might cause additional redraw
        setClipBounds(mHeaderClip);
        if (mMainRV != null) {
            mRVClip.right = mMainRV.getWidth() > 0 ? mMainRV.getWidth() : Integer.MAX_VALUE;
            mRVClip.bottom = mMainRV.getHeight() > 0 ? mMainRV.getHeight() : Integer.MAX_VALUE;
            mMainRV.setClipBounds(mRVClip);
        }
        if (mWorkRV != null) {
            mRVClip.right = mWorkRV.getWidth() > 0 ? mWorkRV.getWidth() : Integer.MAX_VALUE;
            mRVClip.bottom = mWorkRV.getHeight() > 0 ? mWorkRV.getHeight() : Integer.MAX_VALUE;
            mWorkRV.setClipBounds(mRVClip);
        }
        if (mSearchRV != null) {
            if (Flags.floatingSearchBar()) {
                mSearchRV.setClipBounds(null);
            } else {
                mRVClip.right = mSearchRV.getWidth() > 0 ? mSearchRV.getWidth() : Integer.MAX_VALUE;
                mRVClip.bottom = mSearchRV.getHeight() > 0 ? mSearchRV.getHeight() : Integer.MAX_VALUE;
                mSearchRV.setClipBounds(mRVClip);
            }
        }
    }

    /**
     * Hides all the floating rows
     */
    public void setFloatingRowsCollapsed(boolean collapsed) {
        if (mFloatingRowsCollapsed == collapsed) {
            return;
        }

        mFloatingRowsCollapsed = collapsed;
        onHeightUpdated();
    }

    public int getClipTop() {
        return mHeaderClip.top;
    }

    public void reset(boolean animate) {
        if (mAnimator.isStarted()) {
            mAnimator.cancel();
        }
        if (animate) {
            mAnimator.setIntValues(mTranslationY, 0);
            mAnimator.setDuration(150);
            mAnimator.start();
        } else {
            mTranslationY = 0;
            applyVerticalMove();
            if (getParent() instanceof ActivityAllAppsContainerView && Flags.floatingSearchBar()) {
                int top = getTop() > 0 ? getTop()
                        : ActivityContext.lookupContext(getContext()).getDeviceProfile().getInsets().top;
                int gap = getResources().getDimensionPixelSize(R.dimen.all_apps_suggestions_bottom_gap);
                int initialCardTop = mFloatingRowsHeight > 0 ? (top + mFloatingRowsHeight + gap) : top;
                ((ActivityAllAppsContainerView<?>) getParent()).setCardTop(initialCardTop);
            }
        }
        mHeaderCollapsed = false;
        mSnappedScrolledY = -mMaxTranslation;
        mCurrentRV.scrollToTop();
    }

    public boolean isExpanded() {
        return !mHeaderCollapsed;
    }

    /** Returns true if personal/work tabs are currently in use. */
    public boolean usingTabs() {
        return !mTabsHidden;
    }

    PersonalWorkSlidingTabStrip getTabLayout() {
        return mTabLayout;
    }

    /** Calculates the combined height of any floating rows (e.g. predicted apps, app divider). */
    private void updateFloatingRowsHeight() {
        mFloatingRowsHeight =
                Arrays.stream(mAllRows).mapToInt(FloatingHeaderRow::getExpectedHeight).sum();
    }

    /** Gets the combined height of any floating rows (e.g. predicted apps, app divider). */
    int getFloatingRowsHeight() {
        return mFloatingRowsHeight;
    }

    int getTabsAdditionalPaddingBottom() {
        return mTabsAdditionalPaddingBottom;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mTranslationY = (Integer) animation.getAnimatedValue();
        applyVerticalMove();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        calcOffset(mTempOffset);
        ev.offsetLocation(mTempOffset.x, mTempOffset.y);
        mForwardToRecyclerView = mCurrentRV.onInterceptTouchEvent(ev);
        ev.offsetLocation(-mTempOffset.x, -mTempOffset.y);
        return mForwardToRecyclerView || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mForwardToRecyclerView) {
            // take this view's and parent view's (view pager) location into account
            calcOffset(mTempOffset);
            event.offsetLocation(mTempOffset.x, mTempOffset.y);
            try {
                return mCurrentRV.onTouchEvent(event);
            } finally {
                event.offsetLocation(-mTempOffset.x, -mTempOffset.y);
            }
        } else {
            return super.onTouchEvent(event);
        }
    }

    private void calcOffset(Point p) {
        p.x = getLeft() - mCurrentRV.getLeft() - ((ViewGroup) mCurrentRV.getParent()).getLeft();
        p.y = getTop() - mCurrentRV.getTop() - ((ViewGroup) mCurrentRV.getParent()).getTop();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void setInsets(Rect insets) {
        if (Flags.floatingSearchBar()) {
            setPadding(0, 0, 0, 0);
        } else {
            Rect allAppsPadding = ActivityContext.lookupContext(getContext())
                    .getDeviceProfile().allAppsPadding;
            setPadding(allAppsPadding.left, getPaddingTop(), allAppsPadding.right, getPaddingBottom());
        }
    }

    public <T extends FloatingHeaderRow> T findFixedRowByType(Class<T> type) {
        for (FloatingHeaderRow row : mAllRows) {
            if (row.getTypeClass() == type) {
                return (T) row;
            }
        }
        return null;
    }

    /**
     * Returns visible height of FloatingHeaderView contents requiring header protection or the
     * expected header protection height.
     */
    int getPeripheralProtectionHeight(boolean expected) {
        if (expected) {
            return getTabLayout().getBottom() - getPaddingTop() + getPaddingBottom()
                    - mMaxTranslation;
        }
        // we only want to show protection when work tab is available and header is either
        // collapsed or animating to/from collapsed state
        if (mTabsHidden || mFloatingRowsCollapsed || !mHeaderCollapsed) {
            return 0;
        }
        return Math.max(0,
                getTabLayout().getBottom() - getPaddingTop() + getPaddingBottom() + mTranslationY);
    }
}
