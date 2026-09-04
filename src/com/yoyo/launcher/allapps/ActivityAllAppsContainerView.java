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
package com.yoyo.launcher.allapps;

import static com.yoyo.launcher.Flags.clearScrimOnReset;
import static com.yoyo.launcher.Flags.enableExpandingPauseWorkButton;
import static com.yoyo.launcher.LauncherPrefs.ALLAPPS_THEMED_ICONS;
import static com.yoyo.launcher.LauncherPrefs.DRAWER_OPACITY;
import static com.yoyo.launcher.LauncherPrefs.SHOW_DESKTOP_LABELS;
import static com.yoyo.launcher.LauncherPrefs.SHOW_DRAWER_LABELS;
import static com.yoyo.launcher.LauncherPrefs.THEMED_ICONS_HOMESCREEN_ONLY;
import static com.yoyo.launcher.allapps.ActivityAllAppsContainerView.AdapterHolder.MAIN;
import static com.yoyo.launcher.allapps.ActivityAllAppsContainerView.AdapterHolder.SEARCH;
import static com.yoyo.launcher.allapps.ActivityAllAppsContainerView.AdapterHolder.WORK;
import static com.yoyo.launcher.allapps.BaseAllAppsAdapter.VIEW_TYPE_PRIVATE_SPACE_HEADER;
import static com.yoyo.launcher.allapps.BaseAllAppsAdapter.VIEW_TYPE_WORK_DISABLED_CARD;
import static com.yoyo.launcher.allapps.BaseAllAppsAdapter.VIEW_TYPE_WORK_EDU_CARD;
import static com.yoyo.launcher.logging.StatsLogManager.LauncherEvent.LAUNCHER_ALLAPPS_COUNT;
import static com.yoyo.launcher.logging.StatsLogManager.LauncherEvent.LAUNCHER_ALLAPPS_TAP_ON_PERSONAL_TAB;
import static com.yoyo.launcher.logging.StatsLogManager.LauncherEvent.LAUNCHER_ALLAPPS_TAP_ON_WORK_TAB;
import static com.yoyo.launcher.util.Executors.MAIN_EXECUTOR;
import static com.yoyo.launcher.util.ScrollableLayoutManager.PREDICTIVE_BACK_MIN_SCALE;
import static com.yoyo.launcher.views.RecyclerViewFastScroller.FastScrollerLocation.ALL_APPS_SCROLLER;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EdgeEffect;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import com.yoyo.launcher.DeviceProfile;
import com.yoyo.launcher.DeviceProfile.OnDeviceProfileChangeListener;
import com.yoyo.launcher.DragSource;
import com.yoyo.launcher.DropTarget.DragObject;
import com.yoyo.launcher.Flags;
import com.yoyo.launcher.Insettable;
import com.yoyo.launcher.InsettableFrameLayout;
import com.yoyo.launcher.Launcher;
import com.yoyo.launcher.LauncherState;
import com.yoyo.launcher.LauncherPrefChangeListener;
import com.yoyo.launcher.LauncherPrefs;
import com.yoyo.launcher.R;
import com.yoyo.launcher.Utilities;
import com.yoyo.launcher.util.SystemUiController;
import com.yoyo.launcher.allapps.BaseAllAppsAdapter.AdapterItem;
import com.yoyo.launcher.allapps.search.AllAppsSearchUiDelegate;
import com.yoyo.launcher.allapps.search.SearchAdapterProvider;
import com.yoyo.launcher.config.FeatureFlags;
import com.yoyo.launcher.keyboard.FocusedItemDecorator;
import com.yoyo.launcher.keyboard.ViewGroupFocusHelper;
import com.yoyo.launcher.model.StringCache;
import com.yoyo.launcher.model.data.ItemInfo;
import com.yoyo.launcher.pm.UserCache;
import com.yoyo.launcher.recyclerview.AllAppsRecyclerViewPool;
import com.yoyo.launcher.util.ItemInfoMatcher;
import com.yoyo.launcher.util.Preconditions;
import com.yoyo.launcher.util.Themes;
import com.yoyo.launcher.views.ActivityContext;
import com.yoyo.launcher.views.BaseDragLayer;
import com.yoyo.launcher.views.RecyclerViewFastScroller;
import com.yoyo.launcher.views.ScrimView;
import com.yoyo.launcher.views.SpringRelativeLayout;
import com.yoyo.launcher.workprofile.PersonalWorkSlidingTabStrip;
import com.android.systemui.plugins.AllAppsRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * All apps container view with search support for use in a dragging activity.
 *
 * @param <T> Type of context inflating all apps.
 */
public class ActivityAllAppsContainerView<T extends Context & ActivityContext>
        extends SpringRelativeLayout implements DragSource, Insettable,
        OnDeviceProfileChangeListener, PersonalWorkSlidingTabStrip.OnActivePageChangedListener,
        ScrimView.ScrimDrawingController {


    private static final String TAG = "ActivityAllAppsContainerView";
    public static final float PULL_MULTIPLIER = .02f;
    public static final float FLING_VELOCITY_MULTIPLIER = 1200f;
    protected static final String BUNDLE_KEY_CURRENT_PAGE = "launcher.allapps.current_page";
    // As of this writing, search transition does not seem to work properly, so set duration to 0.
    private static final long DEFAULT_SEARCH_TRANSITION_DURATION_MS = 0;
    // Render the header protection at all times to debug clipping issues.
    private static final boolean DEBUG_HEADER_PROTECTION = false;
    /** Context of an activity or window that is inflating this container. */

    protected final T mActivityContext;
    protected final List<AdapterHolder> mAH;
    protected final Predicate<ItemInfo> mPersonalMatcher = ItemInfoMatcher.ofUser(
            Process.myUserHandle());
    protected WorkProfileManager mWorkManager;
    protected final PrivateProfileManager mPrivateProfileManager;
    protected final Point mFastScrollerOffset = new Point();
    protected final int mScrimColor;
    protected final float mHeaderThreshold;
    protected final AllAppsSearchUiDelegate mSearchUiDelegate;

    private boolean mThemeAllAppsIcons;

    // Used to animate Search results out and A-Z apps in, or vice-versa.
    private final SearchTransitionController mSearchTransitionController;
    private final Paint mHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mInsets = new Rect();
    private final AllAppsStore mAllAppsStore;
    private final RecyclerView.OnScrollListener mScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    updateHeaderScroll(recyclerView.computeVerticalScrollOffset());
                }
            };
    private final Paint mNavBarScrimPaint;
    private final int mHeaderProtectionColor;
    private final int mPrivateSpaceBottomExtraSpace;
    private final Path mTmpPath = new Path();
    private final RectF mTmpRectF = new RectF();
    protected AllAppsPagedView mViewPager;
    protected FloatingHeaderView mHeader;
    protected final List<AllAppsRow> mAdditionalHeaderRows = new ArrayList<>();
    protected View mBottomSheetBackground;
    protected RecyclerViewFastScroller mFastScroller;
    private ConstraintLayout mFastScrollLetterLayout;

    /**
     * View that defines the search box. Result is rendered inside {@link #mSearchRecyclerView}.
     */
    protected View mSearchContainer;
    protected SearchUiManager mSearchUiManager;
    protected boolean mUsingTabs;
    protected RecyclerViewFastScroller mTouchHandler;

    /** {@code true} when rendered view is in search state instead of the scroll state. */
    private boolean mIsSearching;
    private boolean mRebindAdaptersAfterSearchAnimation;
    private int mNavBarScrimHeight = 0;
    private SearchRecyclerView mSearchRecyclerView;
    protected SearchAdapterProvider<?> mMainAdapterProvider;
    private View mBottomSheetHandleArea;
    private boolean mHasWorkApps;
    private boolean mHasPrivateApps;
    private float[] mBottomSheetCornerRadii;
    private ScrimView mScrimView;
    private int mHeaderColor;
    private int mBottomSheetBackgroundColorBlurFallback;
    private int mBottomSheetBackgroundColorOverBlur;
    private int mBottomSheetBackgroundColorLegacy;
    private int mTabsProtectionAlpha;
    @Nullable private AllAppsTransitionController mAllAppsTransitionController;

    private boolean mIsSettingUpHeader;
    private int mSearchImeBottom = 0;
    private Drawable mCardBackground;
    private Drawable mSuggestionsCardBackground;
    private int mCardTop = 0;
    private int mDrawerOpacity = 85;

    public ActivityAllAppsContainerView(Context context) {
        this(context, null);
    }

    public ActivityAllAppsContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActivityAllAppsContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivityContext = ActivityContext.lookupContext(context);
        mAllAppsStore = mActivityContext.getActivityComponent().getAppsStore();

        mThemeAllAppsIcons = ALLAPPS_THEMED_ICONS.get(context);
        int scrimColor = Themes.getAttrColor(context, R.attr.allAppsScrimColor);
        mScrimColor = scrimColor != 0 ? ColorUtils.setAlphaComponent(scrimColor, 230) : 0;
        mHeaderThreshold = getResources().getDimensionPixelSize(
                R.dimen.dynamic_grid_cell_border_spacing);
        mHeaderProtectionColor = Themes.getAttrColor(context, R.attr.allappsHeaderProtectionColor);

        mWorkManager = new WorkProfileManager(
                this,
                mActivityContext.getStatsLogManager(),
                UserCache.INSTANCE.get(mActivityContext));
        mPrivateProfileManager = new PrivateProfileManager(
                this,
                mActivityContext.getStatsLogManager(),
                UserCache.INSTANCE.get(mActivityContext));
        mPrivateSpaceBottomExtraSpace = context.getResources().getDimensionPixelSize(
                R.dimen.ps_extra_bottom_padding);
        mAH = Arrays.asList(null, null, null);
        mNavBarScrimPaint = new Paint();
        mNavBarScrimPaint.setColor(Themes.getNavBarScrimColor(mActivityContext));

        AllAppsStore.OnUpdateListener onAppsUpdated = this::onAppsUpdated;
        mAllAppsStore.addUpdateListener(onAppsUpdated);

        mCardBackground = context.getDrawable(R.drawable.bg_all_apps_list);
        mSuggestionsCardBackground = context.getDrawable(R.drawable.bg_all_apps_suggestions);

        // This is a focus listener that proxies focus from a view into the list view.  This is to
        // work around the search box from getting first focus and showing the cursor.
        setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && getActiveRecyclerView() != null) {
                getActiveRecyclerView().requestFocus();
            }
        });
        mSearchUiDelegate = createSearchUiDelegate();
        initContent();

        mSearchTransitionController = new SearchTransitionController(this);
    }

    /** Creates the delegate for initializing search. */
    protected AllAppsSearchUiDelegate createSearchUiDelegate() {
        return new AllAppsSearchUiDelegate(this);
    }

    public AllAppsSearchUiDelegate getSearchUiDelegate() {
        return mSearchUiDelegate;
    }

    /**
     * Initializes the view hierarchy and internal variables. Any initialization which actually uses
     * these members should be done in {@link #onFinishInflate()}.
     * In terms of subclass initialization, the following would be parallel order for activity:
     *   initContent -> onPreCreate
     *   constructor/init -> onCreate
     *   onFinishInflate -> onPostCreate
     */
    protected void initContent() {
        mMainAdapterProvider = mSearchUiDelegate.createMainAdapterProvider();

        mAH.set(MAIN, new AdapterHolder(MAIN,
                new AlphabeticalAppsList(mActivityContext,
                        mAllAppsStore,
                        null,
                        mPrivateProfileManager)));
        mAH.set(WORK, new AdapterHolder(WORK,
                new AlphabeticalAppsList(mActivityContext, mAllAppsStore, mWorkManager, null)));
        mAH.set(SEARCH, new AdapterHolder(SEARCH,
                new AlphabeticalAppsList(mActivityContext, null, null, null)));

        getLayoutInflater().inflate(R.layout.all_apps_content, this);
        mHeader = findViewById(R.id.all_apps_header);
        mAdditionalHeaderRows.clear();
        mAdditionalHeaderRows.addAll(getAdditionalHeaderRows());
        mBottomSheetBackground = findViewById(R.id.bottom_sheet_background);
        mBottomSheetHandleArea = findViewById(R.id.bottom_sheet_handle_area);
        mSearchRecyclerView = findViewById(R.id.search_results_list_view);
        mFastScroller = findViewById(R.id.fast_scroller);
        mFastScroller.setPopupView(findViewById(R.id.fast_scroller_popup));
        mFastScrollLetterLayout = findViewById(R.id.scroll_letter_layout);
        setClipChildren(false);

        mSearchContainer = inflateSearchBar();
        LayoutParams searchParams = (LayoutParams) mSearchContainer.getLayoutParams();
        if (searchParams == null) {
            searchParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        searchParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        searchParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mSearchContainer.setLayoutParams(searchParams);
        addView(mSearchContainer);
        mSearchContainer.setFocusedByDefault(true);
        mSearchUiManager = (SearchUiManager) mSearchContainer;
    }

    public List<AllAppsRow> getAdditionalHeaderRows() {
        return List.of();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mAH.get(SEARCH).setup(mSearchRecyclerView,
                /* Filter out A-Z apps */ itemInfo -> false);
        rebindAdapters(true /* force */);
        float cornerRadius = Themes.getDialogCornerRadius(getContext());
        mBottomSheetCornerRadii = new float[]{
                cornerRadius,
                cornerRadius, // Top left radius in px
                cornerRadius,
                cornerRadius, // Top right radius in px
                0,
                0, // Bottom right
                0,
                0 // Bottom left
        };

        if (Flags.allAppsBlur()) {
            int layerFg = getContext().getColor(R.color.blur_shade_panel_fg);
            int layerBg = getContext().getColor(R.color.blur_shade_panel_bg);
            mBottomSheetBackgroundColorOverBlur = ColorUtils.compositeColors(layerFg, layerBg);
            mBottomSheetBackgroundColorBlurFallback = getContext().getColor(
                    Utilities.isDarkTheme(getContext()) ? android.R.color.system_accent2_800
                            : android.R.color.system_accent2_200);
        }

        mBottomSheetBackgroundColorLegacy = getContext().getColor(R.color.materialColorSurfaceDim);

        updateBackgroundVisibility(mActivityContext.getDeviceProfile());
        mSearchUiManager.initializeSearch(this);
        updateDrawerOpacity();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isSearchBarFloating()) {
            mSearchUiDelegate.onInitializeSearchBar();
        }
        mActivityContext.addOnDeviceProfileChangeListener(this);
        LauncherPrefs.get(getContext()).addListener(mThemePrefListener, ALLAPPS_THEMED_ICONS, THEMED_ICONS_HOMESCREEN_ONLY);
        LauncherPrefs.get(getContext()).addListener(mOpacityPrefListener, DRAWER_OPACITY);
        updateDrawerOpacity();
    }

    private final LauncherPrefChangeListener mThemePrefListener = key -> {
        if (ALLAPPS_THEMED_ICONS.getSharedPrefKey().equals(key) || THEMED_ICONS_HOMESCREEN_ONLY.getSharedPrefKey().equals(key)) {
            mThemeAllAppsIcons = ALLAPPS_THEMED_ICONS.get(getContext());
            rebindAdapters(true);
        }
    };

    private final LauncherPrefChangeListener mOpacityPrefListener = key -> {
        if (DRAWER_OPACITY.getSharedPrefKey().equals(key)) {
            updateDrawerOpacity();
        }
    };

    public int getDrawerOpacity() {
        return mDrawerOpacity;
    }

    public float getCommonBgAlpha() {
        float p = mDrawerOpacity / 100.0f;
        if (p <= 0f) return 0f;
        if (p >= 1f) return 1f;
        return (float) Math.pow(p, 1.15) * 0.95f;
    }

    public float getCardBgAlpha() {
        float p = mDrawerOpacity / 100.0f;
        if (p <= 0f) return 0f;
        if (p >= 1f) return 1f;
        return Math.min(1.0f, (float) Math.sqrt(p) * 0.92f + 0.08f * p);
    }

    public float getSearchBgAlpha() {
        float p = mDrawerOpacity / 100.0f;
        if (p <= 0f) return 0f;
        if (p >= 1f) return 1f;
        return Math.min(1.0f, (float) Math.sqrt(p) * 0.95f + 0.05f * p);
    }

    public int getAdaptiveSystemUiFlags() {
        Context context = getContext();
        int drawerBgColor = Themes.getAttrColor(context, R.attr.allAppsContainerColor);
        boolean isWorkspaceDarkText = Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);
        int wallpaperColor = isWorkspaceDarkText ? Color.WHITE : Color.BLACK;

        float commonAlpha = getCommonBgAlpha();
        int effectiveStatusColor = ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(drawerBgColor, Math.round(commonAlpha * 255)),
                wallpaperColor
        );
        boolean isStatusLight = ColorUtils.calculateLuminance(effectiveStatusColor) > 0.45;

        int navColor = Themes.getAttrColor(context, R.attr.allAppsContainerColor);
        int effectiveNavColor = ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(navColor, Math.round(commonAlpha * 255)),
                wallpaperColor
        );
        boolean isNavLight = ColorUtils.calculateLuminance(effectiveNavColor) > 0.45;

        return (isStatusLight ? SystemUiController.FLAG_LIGHT_STATUS : SystemUiController.FLAG_DARK_STATUS)
                | (isNavLight ? SystemUiController.FLAG_LIGHT_NAV : SystemUiController.FLAG_DARK_NAV);
    }

    public void updateDrawerOpacity() {
        mDrawerOpacity = LauncherPrefs.get(getContext()).get(DRAWER_OPACITY);
        float commonAlpha = getCommonBgAlpha();
        int commonAlpha255 = Math.round(commonAlpha * 255);

        Drawable bg = getBackground();
        if (bg != null) {
            bg.mutate().setAlpha(commonAlpha255);
        }

        float searchAlpha = getSearchBgAlpha();
        if (mSearchContainer != null && mSearchContainer.getBackground() != null) {
            mSearchContainer.getBackground().mutate().setAlpha(Math.round(searchAlpha * 255));
        }

        if (mActivityContext instanceof Launcher) {
            Launcher launcher = (Launcher) mActivityContext;
            if (launcher.isInState(LauncherState.ALL_APPS)) {
                launcher.getSystemUiController().updateUiState(
                        SystemUiController.UI_STATE_ALL_APPS, getAdaptiveSystemUiFlags());
            }
        }

        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isSearchBarFloating()) {
            mSearchUiDelegate.onDestroySearchBar();
        }
        mActivityContext.removeOnDeviceProfileChangeListener(this);
        LauncherPrefs.get(getContext()).removeListener(mThemePrefListener, ALLAPPS_THEMED_ICONS, THEMED_ICONS_HOMESCREEN_ONLY);
        LauncherPrefs.get(getContext()).removeListener(mOpacityPrefListener, DRAWER_OPACITY);
    }

    public SearchUiManager getSearchUiManager() {
        return mSearchUiManager;
    }

    public View getSearchView() {
        return mSearchContainer;
    }

    /** Invoke when the current search session is finished. */
    public void onClearSearchResult() {
        getMainAdapterProvider().clearHighlightedItem();
        animateToSearchState(false);
        rebindAdapters();
    }

    /**
     * Sets results list for search
     */
    public void setSearchResults(ArrayList<AdapterItem> results) {
        getMainAdapterProvider().clearHighlightedItem();
        if (getSearchResultList().setSearchResults(results)) {
            getSearchRecyclerView().onSearchResultsChanged();
        }
        if (results != null) {
            animateToSearchState(true);
        }
    }

    /**
     * Sets results list for search.
     *
     * @param searchResultCode indicates if the result is final or intermediate for a given query
     *                         since we can get search results from multiple sources.
     */
    public void setSearchResults(ArrayList<AdapterItem> results, int searchResultCode) {
        setSearchResults(results);
        mSearchUiDelegate.onSearchResultsChanged(results, searchResultCode);
    }

    public void animateToSearchState(boolean goingToSearch) {
        animateToSearchState(goingToSearch, DEFAULT_SEARCH_TRANSITION_DURATION_MS);
    }

    public void setAllAppsTransitionController(
            AllAppsTransitionController allAppsTransitionController) {
        mAllAppsTransitionController = allAppsTransitionController;
    }

    void animateToSearchState(boolean goingToSearch, long durationMs) {
        if (!mSearchTransitionController.isRunning() && goingToSearch == isSearching()) {
            return;
        }
        mFastScroller.setVisibility(goingToSearch ? INVISIBLE : VISIBLE);
        if (goingToSearch) {
            // Fade out the button to pause work apps.
            mWorkManager.onActivePageChanged(SEARCH);
        } else if (mAllAppsTransitionController != null) {
            // If exiting search, revert predictive back scale on all apps
            mAllAppsTransitionController.animateAllAppsToNoScale();
        }
        setScrollbarVisibility(!goingToSearch);
        mSearchTransitionController.animateToState(goingToSearch, durationMs,
                /* onEndRunnable = */ () -> {
                    mIsSearching = goingToSearch;
                    updateSearchResultsVisibility();
                    int previousPage = getCurrentPage();
                    if (mRebindAdaptersAfterSearchAnimation) {
                        rebindAdapters(false);
                        mRebindAdaptersAfterSearchAnimation = false;
                    }

                    if (goingToSearch) {
                        mSearchUiDelegate.onAnimateToSearchStateCompleted();
                    } else {
                        setSearchResults(null);
                        if (mViewPager != null) {
                            mViewPager.setCurrentPage(previousPage);
                        }
                        onActivePageChanged(previousPage);
                    }
                });
    }

    public boolean shouldContainerScroll(MotionEvent ev) {
        BaseDragLayer dragLayer = mActivityContext.getDragLayer();
        // IF the MotionEvent is inside the search box or handle area, and the container keeps on
        // receiving touch input, container should move down.
        if (dragLayer.isEventOverView(mSearchContainer, ev)
                || dragLayer.isEventOverView(mBottomSheetHandleArea, ev)) {
            return true;
        }
        AllAppsRecyclerView rv = getActiveRecyclerView();
        if (rv == null) {
            return true;
        }
        if (rv.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING) {
            return false;
        }
        if (rv.getScrollbar() != null
                && rv.getScrollbar().getThumbOffsetY() >= 0
                && dragLayer.isEventOverView(rv.getScrollbar(), ev)) {
            return false;
        }
        // Scroll if not within the container view (e.g. over large-screen scrim).
        if (!dragLayer.isEventOverView(getVisibleContainerView(), ev)) {
            return true;
        }
        return rv.shouldContainerScroll(ev, dragLayer);
    }

    @Override
    public RecyclerView.EdgeEffectFactory createEdgeEffectFactory() {
        return new RecyclerView.EdgeEffectFactory() {
            @NonNull
            @Override
            protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
                EdgeEffect effect = super.createEdgeEffect(view, direction);
                effect.setColor(Themes.getAttrColor(view.getContext(), R.attr.allAppsContainerColor));
                return effect;
            }
        };
    }

    /**
     * Resets the UI to be ready for fresh interactions in the future. Exits search and returns to
     * A-Z apps list.
     *
     * @param animate Whether to animate the header during the reset (e.g. switching profile tabs).
     * @param clearScrim Scrim gets set during AllAppsTransitionController and should only be
     *                  {@code true} if All Apps is not visible.
     */
    public void reset(boolean animate, boolean clearScrim) {
        reset(animate, true, clearScrim);
    }

    /**
     * Resets the UI to be ready for fresh interactions in the future.
     *
     * @param animate Whether to animate the header during the reset (e.g. switching profile tabs).
     * @param exitSearch Whether to force exit the search state and return to A-Z apps list.
     * @param clearScrim Whether to clear the all apps scrim.
     */
    public void reset(boolean animate, boolean exitSearch, boolean clearScrim) {
        // Scroll Main and Work RV to top. Search RV is done in `resetSearch`.
        for (int i = 0; i < mAH.size(); i++) {
            if (i != SEARCH && mAH.get(i).mRecyclerView != null) {
                mAH.get(i).mRecyclerView.scrollToTop();
            }
        }
        if (mTouchHandler != null) {
            mTouchHandler.endFastScrolling();
        }
        if (mHeader != null && mHeader.getVisibility() == VISIBLE) {
            mHeader.reset(animate);
        }
        updateBackgroundVisibility(mActivityContext.getDeviceProfile());
        // Reset the base recycler view after transitioning home.
        updateHeaderScroll(0);
        if (exitSearch) {
            // Reset the search bar and search RV after transitioning home.
            MAIN_EXECUTOR.getHandler().post(mSearchUiManager::resetSearch);
        }
        if (isSearching()) {
            mWorkManager.reset();
        }
        if (clearScrimOnReset() && mScrimView != null && clearScrim) {
            mScrimView.setDrawingController(null);
        }
    }

    /**
     * Exits search and returns to A-Z apps list. Scroll to the private space header.
     */
    public void resetAndScrollToPrivateSpaceHeader() {
        // Animate to A-Z with 0 time to reset the animation with proper state management.
        // We can't rely on `animateToSearchState` with delay inside `resetSearch` because that will
        // conflict with following scrolling to bottom, so we need it with 0 time here.
        animateToSearchState(false, 0);

        MAIN_EXECUTOR.getHandler().post(() -> {
            // Reset the search bar after transitioning home.
            // When `resetSearch` is called after `animateToSearchState` is finished, the inside
            // `animateToSearchState` with delay is a just no-op and return early.
            mSearchUiManager.resetSearch();
            // Switch to the main tab
            switchToTab(MAIN);
            // Scroll to bottom
            if (mPrivateProfileManager != null) {
                mPrivateProfileManager.scrollForHeaderToBeVisibleInContainer(
                        getActiveAppsRecyclerView(),
                        getPersonalAppList().getAdapterItems(),
                        mPrivateProfileManager.getPsHeaderHeight(),
                        mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx());
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        mSearchUiManager.preDispatchKeyEvent(event);
        return super.dispatchKeyEvent(event);
    }

    public String getDescription() {
        if (!mUsingTabs && isSearching()) {
            return getContext().getString(R.string.all_apps_search_results);
        } else {
            StringCache cache = mActivityContext.getStringCache();
            if (mUsingTabs) {
                if (cache != null) {
                    return isPersonalTab()
                            ? cache.allAppsPersonalTabAccessibility
                            : cache.allAppsWorkTabAccessibility;
                } else {
                    return isPersonalTab()
                            ? getContext().getString(R.string.all_apps_button_personal_label)
                            : getContext().getString(R.string.all_apps_button_work_label);
                }
            }
            return getContext().getString(R.string.all_apps_button_label);
        }
    }

    public boolean isSearching() {
        return mIsSearching;
    }

    /**
     * @return {@code true} if back gesture should exit search rather than change launcher state.
      */
    public boolean shouldBackExitSearch() {
        return isSearching();
    }

    @Override
    public void onActivePageChanged(int currentActivePage) {
        if (mSearchTransitionController.isRunning()) {
            // Will be called at the end of the animation.
            return;
        }
        if (mAH.get(currentActivePage).mRecyclerView != null) {
            mAH.get(currentActivePage).mRecyclerView.bindFastScrollbar(mFastScroller,
                    ALL_APPS_SCROLLER);
        }
        // Header keeps track of active recycler view to properly render header protection.
        mHeader.setActiveRV(currentActivePage);

        mWorkManager.onActivePageChanged(currentActivePage);
    }

    public void rebindAdapters() {
        rebindAdapters(false /* force */);
    }

    public void rebindAdapters(boolean force) {
        Log.d(TAG, "rebindAdapters: force: " + force);
        if (mSearchTransitionController.isRunning()) {
            mRebindAdaptersAfterSearchAnimation = true;
            return;
        }
        updateSearchResultsVisibility();

        boolean showTabs = shouldShowTabs();
        if (showTabs == mUsingTabs && !force) {
            Log.d(TAG, "rebindAdapters: Not needed.");
            return;
        }

        // replaceAppsRVcontainer() needs to use both mUsingTabs value to remove the old view AND
        // showTabs value to create new view. Hence the mUsingTabs new value assignment MUST happen
        // after this call.
        replaceAppsRVContainer(showTabs);
        mUsingTabs = showTabs;

        mAllAppsStore.unregisterIconContainer(mAH.get(MAIN).mRecyclerView);
        mAllAppsStore.unregisterIconContainer(mAH.get(WORK).mRecyclerView);
        mAllAppsStore.unregisterIconContainer(mAH.get(SEARCH).mRecyclerView);

        final AllAppsRecyclerView mainRecyclerView;
        final AllAppsRecyclerView workRecyclerView;
        if (mUsingTabs) {
            mainRecyclerView = (AllAppsRecyclerView) mViewPager.getChildAt(0);
            workRecyclerView = (AllAppsRecyclerView) mViewPager.getChildAt(1);
            mAH.get(MAIN).setup(mainRecyclerView, mPersonalMatcher);
            mAH.get(WORK).setup(workRecyclerView, mWorkManager.getItemInfoMatcher());
            workRecyclerView.setId(R.id.apps_list_view_work);
            if (enableExpandingPauseWorkButton()
                    || FeatureFlags.ENABLE_EXPANDING_PAUSE_WORK_BUTTON.get()) {
                mAH.get(WORK).mRecyclerView.addOnScrollListener(
                        mWorkManager.newScrollListener());
            }
            mViewPager.getPageIndicator().setActiveMarker(MAIN);
            findViewById(R.id.tab_personal)
                    .setOnClickListener((View view) -> {
                        Log.d(TAG, "rebindAdapters: " + "Clicked personal tab.");
                        if (mViewPager.snapToPage(MAIN)) {
                            mActivityContext.getStatsLogManager().logger()
                                    .log(LAUNCHER_ALLAPPS_TAP_ON_PERSONAL_TAB);
                        }
                    });
            findViewById(R.id.tab_work)
                    .setOnClickListener((View view) -> {
                        Log.d(TAG, "rebindAdapters: " + "Clicked work tab.");
                        if (mViewPager.snapToPage(WORK)) {
                            mActivityContext.getStatsLogManager().logger()
                                    .log(LAUNCHER_ALLAPPS_TAP_ON_WORK_TAB);
                        }
                    });
            setDeviceManagementResources();
            if (mHeader.isSetUp()) {
                onActivePageChanged(mViewPager.getNextPage());
            }
        } else {
            mainRecyclerView = findViewById(R.id.apps_list_view);
            workRecyclerView = null;
            mAH.get(MAIN).setup(mainRecyclerView, mPersonalMatcher);
            mAH.get(WORK).mRecyclerView = null;
        }
        setUpCustomRecyclerViewPool(
                mainRecyclerView,
                workRecyclerView,
                mActivityContext.getActivityComponent().getSharedAppsPool());
        setupHeader();

        if (isSearchBarFloating()) {
            // Keep the scroller above the search bar.
            LayoutParams scrollerLayoutParams =
                    (LayoutParams) mFastScroller.getLayoutParams();
            int searchHeight = mSearchContainer != null ? mSearchContainer.getHeight() : 0;
            if (searchHeight == 0) {
                searchHeight = getResources().getDimensionPixelSize(
                        R.dimen.all_apps_search_bar_field_height);
            }
            int extraBottom = getResources().getDimensionPixelSize(
                    R.dimen.all_apps_search_bar_bottom_margin);
            scrollerLayoutParams.bottomMargin = searchHeight + extraBottom
                    + getResources().getDimensionPixelSize(
                            R.dimen.fastscroll_bottom_margin_floating_search)
                    + mInsets.bottom;
        }

        mAllAppsStore.registerIconContainer(mAH.get(MAIN).mRecyclerView);
        mAllAppsStore.registerIconContainer(mAH.get(WORK).mRecyclerView);
        mAllAppsStore.registerIconContainer(mAH.get(SEARCH).mRecyclerView);
    }

    /**
     * Wire custom {@link RecyclerView.RecycledViewPool} to main and work
     * {@link AllAppsRecyclerView}.
     *
     * Also update max pool size. This is because all apps rv's hidden visibility is changed to
     * {@link View#GONE} from {@link View#INVISIBLE}, thus we cannot rely on layout pass to update
     * pool size.
     */
    private static void setUpCustomRecyclerViewPool(
            @NonNull AllAppsRecyclerView mainRecyclerView,
            @Nullable AllAppsRecyclerView workRecyclerView,
            @NonNull AllAppsRecyclerViewPool recycledViewPool) {
        mainRecyclerView.setRecycledViewPool(recycledViewPool);
        if (workRecyclerView != null) {
            workRecyclerView.setRecycledViewPool(recycledViewPool);
        }
        mainRecyclerView.updatePoolSize();
    }

    private void replaceAppsRVContainer(boolean showTabs) {
        Log.d(TAG, "replaceAppsRVContainer: showTabs: " + showTabs);
        for (int i = MAIN; i <= WORK; i++) {
            AdapterHolder adapterHolder = mAH.get(i);
            if (adapterHolder.mRecyclerView != null) {
                adapterHolder.mRecyclerView.setLayoutManager(null);
                adapterHolder.mRecyclerView.setAdapter(null);
            }
        }
        View oldView = getAppsRecyclerViewContainer();
        int index = indexOfChild(oldView);
        removeView(oldView);
        int layout = showTabs ? R.layout.all_apps_tabs : R.layout.all_apps_rv_layout;
        final View rvContainer = getLayoutInflater().inflate(layout, this, false);
        addView(rvContainer, index);
        if (showTabs) {
            mViewPager = (AllAppsPagedView) rvContainer;
            mViewPager.initParentViews(this);
            mViewPager.getPageIndicator().setOnActivePageChangedListener(this);
            mViewPager.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    @Px final int bottomOffsetPx =
                            (int) (ActivityAllAppsContainerView.this.getMeasuredHeight()
                                    * PREDICTIVE_BACK_MIN_SCALE);
                    outline.setRect(
                            0,
                            0,
                            view.getMeasuredWidth(),
                            view.getMeasuredHeight() + bottomOffsetPx);
                }
            });

            mWorkManager.reset();
            post(() -> mAH.get(WORK).applyPadding());
        } else {
            mWorkManager.detachWorkUtilityViews();
            mViewPager = null;
        }

        removeCustomRules(rvContainer);
        removeCustomRules(getSearchRecyclerView());
        if (isSearchBarFloating()) {
            layoutAboveSearchContainer(rvContainer, showTabs);
            layoutAboveSearchContainer(getSearchRecyclerView(), /* tabs= */ false);
        } else {
            layoutBelowSearchContainer(rvContainer, showTabs);
            layoutBelowSearchContainer(getSearchRecyclerView(), /* tabs= */ false);
        }

        updateSearchResultsVisibility();
    }

    void setupHeader() {
        if (mIsSettingUpHeader) {
            return;
        }
        mIsSettingUpHeader = true;
        try {
            mAdditionalHeaderRows.forEach(row -> mHeader.onPluginDisconnected(row));

            mHeader.setVisibility(View.VISIBLE);
            boolean tabsHidden = !mUsingTabs;
            mHeader.setup(
                    mAH.get(MAIN).mRecyclerView,
                    mAH.get(WORK).mRecyclerView,
                    (SearchRecyclerView) mAH.get(SEARCH).mRecyclerView,
                    getCurrentPage(),
                    tabsHidden);

            int padding = mHeader.getMaxTranslation();
            int top = mHeader.getTop() > 0 ? mHeader.getTop() : mInsets.top;
            int gap = getResources().getDimensionPixelSize(R.dimen.all_apps_suggestions_bottom_gap);
            int floatingRowsHeight = mHeader.getFloatingRowsHeight();
            int initialCardTop = floatingRowsHeight > 0 ? (top + floatingRowsHeight + gap) : top;
            setCardTop(initialCardTop);

            int innerPadding = getResources().getDimensionPixelSize(R.dimen.all_apps_list_inner_padding_top);
            for (int i = 0; i < mAH.size(); i++) {
                final AdapterHolder adapterHolder = mAH.get(i);
                // Search and other adapters need to be handled a bit differently; otherwise, when
                // when leaving search, the All Apps view may be noticeably shifted downward because
                // its padding was unnecessarily impacted, and never restored, upon entering search.
                if (isSearchBarFloating()) {
                    if (i == SEARCH) {
                        adapterHolder.mPadding.top = innerPadding;
                    } else {
                        adapterHolder.mPadding.top = (initialCardTop - top) + innerPadding;
                    }
                } else if (i != SEARCH && !tabsHidden && mHeader.getFloatingRowsHeight() == 0) {
                    // Only the Search adapter needs padding when there are tabs but no floating rows.
                    adapterHolder.mPadding.top = 0;
                } else {
                    adapterHolder.mPadding.top = padding;
                }
                adapterHolder.applyPadding();
                if (adapterHolder.mRecyclerView != null) {
                    adapterHolder.mRecyclerView.scrollToTop();
                }
            }
            mAdditionalHeaderRows.forEach(row -> mHeader.onPluginConnected(row, mActivityContext));

            removeCustomRules(mHeader);
            if (isSearchBarFloating()) {
                alignParentTop(mHeader, false /* includeTabsMargin */);
            } else {
                layoutBelowSearchContainer(mHeader, false /* includeTabsMargin */);
            }
        } finally {
            mIsSettingUpHeader = false;
        }
    }

    /**
     * Force header height update with an offset. Used by SearchView to
     * request {@link FloatingHeaderView} to update its maxTranslation for multiline search bar.
     */
    public void forceUpdateHeaderHeight(int offset) {
        mHeader.updateSearchBarOffset(offset);
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> arrayList) {
        super.addChildrenForAccessibility(arrayList);
        if (!Flags.floatingSearchBar()) {
            // Searchbox container is visually at the top of the all apps UI but it's present in
            // end of the children list.
            // We need to move the searchbox to the top in a11y tree for a11y services to read the
            // all apps screen in same as visual order.
            arrayList.stream().filter(v -> v.getId() == R.id.search_container_all_apps)
                    .findFirst().ifPresent(v -> {
                        arrayList.remove(v);
                        arrayList.add(0, v);
                    });
        }
    }

    protected void updateHeaderScroll(int scrolledOffset) {
        int clampedOffset = Math.max(0, scrolledOffset);
        float prog = Utilities.boundToRange((float) clampedOffset / mHeaderThreshold, 0f, 1f);
        int headerColor = getHeaderColor(prog);
        int tabsAlpha = mHeader.getPeripheralProtectionHeight(/* expectedHeight */ false) == 0 ? 0
                : (int) (Utilities.boundToRange(
                        (clampedOffset + mHeader.mSnappedScrolledY) / mHeaderThreshold, 0f, 1f)
                        * 255);
        if (headerColor != mHeaderColor || mTabsProtectionAlpha != tabsAlpha) {
            mHeaderColor = headerColor;
            mTabsProtectionAlpha = tabsAlpha;
            invalidateHeader();
        }
        if (mSearchUiManager.getEditText() == null) {
            return;
        }

        if (isSearchBarFloating()) {
            mSearchUiManager.setBackgroundVisibility(true, 1f);
        } else {
            boolean bgVisible = mSearchUiManager.getBackgroundVisibility();
            if (clampedOffset == 0 && !isSearching()) {
                bgVisible = true;
            } else if (clampedOffset > mHeaderThreshold) {
                bgVisible = false;
            }
            mSearchUiManager.setBackgroundVisibility(bgVisible, 1 - prog);
        }
    }

    protected int getHeaderColor(float blendRatio) {
        if (!mActivityContext.getDeviceProfile().shouldShowAllAppsOnSheet()) {
            return ColorUtils.setAlphaComponent(
                    ColorUtils.blendARGB(mScrimColor, mHeaderProtectionColor, blendRatio),
                    (int) (mSearchContainer.getAlpha() * 255 * getCommonBgAlpha()));
        }
        return isBackgroundBlurEnabled()
                ? ColorUtils.setAlphaComponent(mHeaderProtectionColor, (int) (blendRatio * 255))
                : ColorUtils.blendARGB(getBackgroundColor(), mHeaderProtectionColor, blendRatio);
    }

    private int getBackgroundColor() {
        return mActivityContext.getDeviceProfile().shouldShowAllAppsOnSheet()
                ? getBottomSheetBackgroundColor() : mScrimColor;
    }

    int getBottomSheetBackgroundColor() {
        if (!Flags.allAppsBlur()) {
            return mBottomSheetBackgroundColorLegacy;
        }
        if (!mActivityContext.isAllAppsBackgroundBlurEnabled()) {
            // Don't apply any alpha if the blur is disabled.
            return mBottomSheetBackgroundColorBlurFallback;
        }
        return mBottomSheetBackgroundColorOverBlur;
    }

    boolean isBackgroundBlurEnabled() {
        return Flags.allAppsBlur() && mActivityContext.isAllAppsBackgroundBlurEnabled();
    }

    /**
     * @return true if the search bar is floating above this container (at the bottom of the screen)
     */
    protected boolean isSearchBarFloating() {
        return mSearchUiDelegate.isSearchBarFloating();
    }

    /**
     * Whether the <em>floating</em> search bar should appear as a small pill when not focused.
     * <p>
     * Note: This method mirrors one in LauncherState. For subclasses that use Launcher, it likely
     * makes sense to use that method to derive an appropriate value for the current/target state.
     */
    public boolean shouldFloatingSearchBarBePillWhenUnfocused() {
        return false;
    }

    /**
     * How far from the bottom of the screen the <em>floating</em> search bar should rest when the
     * IME is not present.
     * <p>
     * To hide offscreen, use a negative value.
     * <p>
     * Note: if the provided value is non-negative but less than the current bottom insets, the
     * insets will be applied. As such, you can use 0 to default to this.
     * <p>
     * Note: This method mirrors one in LauncherState. For subclasses that use Launcher, it likely
     * makes sense to use that method to derive an appropriate value for the current/target state.
     */
    public int getFloatingSearchBarRestingMarginBottom() {
        return 0;
    }

    /**
     * How far from the start of the screen the <em>floating</em> search bar should rest.
     * <p>
     * To use original margin, return a negative value.
     * <p>
     * Note: This method mirrors one in LauncherState. For subclasses that use Launcher, it likely
     * makes sense to use that method to derive an appropriate value for the current/target state.
     */
    public int getFloatingSearchBarRestingMarginStart() {
        DeviceProfile dp = mActivityContext.getDeviceProfile();
        return dp.allAppsLeftRightMargin + dp.getAllAppsIconStartMargin(mActivityContext);
    }

    /**
     * How far from the end of the screen the <em>floating</em> search bar should rest.
     * <p>
     * To use original margin, return a negative value.
     * <p>
     * Note: This method mirrors one in LauncherState. For subclasses that use Launcher, it likely
     * makes sense to use that method to derive an appropriate value for the current/target state.
     */
    public int getFloatingSearchBarRestingMarginEnd() {
        DeviceProfile dp = mActivityContext.getDeviceProfile();
        return dp.allAppsLeftRightMargin + dp.getAllAppsIconStartMargin(mActivityContext);
    }

    private void layoutBelowSearchContainer(View v, boolean includeTabsMargin) {
        if (!(v.getLayoutParams() instanceof LayoutParams)) {
            return;
        }

        LayoutParams layoutParams = (LayoutParams) v.getLayoutParams();
        layoutParams.addRule(RelativeLayout.BELOW, R.id.search_container_all_apps);

        int topMargin = getContext().getResources().getDimensionPixelSize(
                R.dimen.all_apps_search_bar_bottom_adjustment);
        if (includeTabsMargin) {
            topMargin += getContext().getResources().getDimensionPixelSize(
                    R.dimen.all_apps_header_pill_height)
                    + getContext().getResources().getDimensionPixelSize(
                    R.dimen.all_apps_tabs_margin_top);
        }
        layoutParams.topMargin = topMargin;
    }

    private void alignParentTop(View v, boolean includeTabsMargin) {
        if (!(v.getLayoutParams() instanceof LayoutParams)) {
            return;
        }

        LayoutParams layoutParams = (LayoutParams) v.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.topMargin =
                includeTabsMargin
                        ? getContext().getResources().getDimensionPixelSize(
                        R.dimen.all_apps_header_pill_height)
                        : 0;
    }

    public void setCardTop(int cardTop) {
        if (mCardTop != cardTop) {
            mCardTop = cardTop;
            invalidate();
        }
    }

    public int getCardTop() {
        return mCardTop;
    }

    private void layoutAboveSearchContainer(View v, boolean includeTabsMargin) {
        if (!(v.getLayoutParams() instanceof LayoutParams)) {
            return;
        }

        LayoutParams layoutParams = (LayoutParams) v.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ABOVE, R.id.search_container_all_apps);
        layoutParams.topMargin =
                includeTabsMargin
                        ? getContext().getResources().getDimensionPixelSize(
                        R.dimen.all_apps_header_pill_height)
                        : 0;
        layoutParams.bottomMargin = getContext().getResources().getDimensionPixelSize(
                R.dimen.all_apps_list_bottom_spacing);
        int sideMargin = getContext().getResources().getDimensionPixelSize(
                R.dimen.all_apps_list_margin_x);
        layoutParams.leftMargin = sideMargin;
        layoutParams.rightMargin = sideMargin;
    }

    private void removeCustomRules(View v) {
        if (!(v.getLayoutParams() instanceof LayoutParams)) {
            return;
        }

        LayoutParams layoutParams = (LayoutParams) v.getLayoutParams();
        layoutParams.removeRule(RelativeLayout.ABOVE);
        layoutParams.removeRule(RelativeLayout.ALIGN_TOP);
        layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.removeRule(RelativeLayout.BELOW);
        layoutParams.topMargin = 0;
        layoutParams.bottomMargin = 0;
        layoutParams.leftMargin = 0;
        layoutParams.rightMargin = 0;
    }

    protected BaseAllAppsAdapter createAdapter(AlphabeticalAppsList appsList) {
        return new AllAppsGridAdapter(mActivityContext, getLayoutInflater(), appsList,
                mMainAdapterProvider);
    }

    public boolean isInAllApps() {
        // TODO: Make this abstract
        return true;
    }

    /**
     * Inflates the search bar
     */
    protected View inflateSearchBar() {
        return mSearchUiDelegate.inflateSearchBar();
    }

    /** The adapter provider for the main section. */
    public final SearchAdapterProvider<?> getMainAdapterProvider() {
        return mMainAdapterProvider;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        try {
            // Many slice view id is not properly assigned, and hence throws null
            // pointer exception in the underneath method. Catching the exception
            // simply doesn't restore these slice views. This doesn't have any
            // user visible effect because because we query them again.
            super.dispatchRestoreInstanceState(sparseArray);
        } catch (Exception e) {
            Log.e("AllAppsContainerView", "restoreInstanceState viewId = 0", e);
        }

        Bundle state = (Bundle) sparseArray.get(R.id.work_tab_state_id, null);
        if (state != null) {
            int currentPage = state.getInt(BUNDLE_KEY_CURRENT_PAGE, 0);
            if (currentPage == WORK && mViewPager != null) {
                mViewPager.setCurrentPage(currentPage);
                rebindAdapters();
            } else {
                reset(true, false /* clearScrim */);
            }
        }
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);
        Bundle state = new Bundle();
        state.putInt(BUNDLE_KEY_CURRENT_PAGE, getCurrentPage());
        container.put(R.id.work_tab_state_id, state);
    }

    /** @deprecated Get it from {@link ActivityContext#getActivityComponent()} */
    @Deprecated
    public AllAppsStore getAppsStore() {
        return mAllAppsStore;
    }

    public WorkProfileManager getWorkManager() {
        return mWorkManager;
    }

    /** Returns whether Private Profile has been setup. */
    public boolean hasPrivateProfile() {
        return mHasPrivateApps;
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        for (AdapterHolder holder : mAH) {
            holder.mAdapter.setAppsPerRow(dp.numShownAllAppsColumns);
            holder.mAppsList.setNumAppsPerRowAllApps(dp.numShownAllAppsColumns);
            if (holder.mRecyclerView != null) {
                // Remove all views and clear the pool, while keeping the data same. After this
                // call, all the viewHolders will be recreated.
                holder.mRecyclerView.swapAdapter(holder.mRecyclerView.getAdapter(), true);
                holder.mRecyclerView.getRecycledViewPool().clear();
            }
        }
        updateBackgroundVisibility(dp);

        int navBarScrimColor = Themes.getNavBarScrimColor(mActivityContext);
        if (mNavBarScrimPaint.getColor() != navBarScrimColor) {
            mNavBarScrimPaint.setColor(navBarScrimColor);
            invalidate();
        }
    }

    protected void updateBackgroundVisibility(DeviceProfile deviceProfile) {
        mBottomSheetBackground.setVisibility(
                deviceProfile.shouldShowAllAppsOnSheet() ? View.VISIBLE : View.GONE);
        // Note: The opaque sheet background and header protection are added in drawOnScrim.
        // For the taskbar entrypoint, the scrim is drawn by its abstract slide in view container,
        // so its header protection is derived from this scrim instead.
    }

    @VisibleForTesting
    public void onAppsUpdated() {
        Log.d(TAG, "onAppsUpdated; number of apps: " + mAllAppsStore.getApps().length);
        mHasWorkApps = Stream.of(mAllAppsStore.getApps())
                .anyMatch(mWorkManager.getItemInfoMatcher());
        mHasPrivateApps = Stream.of(mAllAppsStore.getApps())
                .anyMatch(mPrivateProfileManager.getItemInfoMatcher());
        if (!isSearching()) {
            rebindAdapters();
        }
        if (mHasWorkApps) {
            mWorkManager.reset();
        }
        if (mHasPrivateApps) {
            mPrivateProfileManager.reset();
        }

        mActivityContext.getStatsLogManager().logger()
                .withCardinality(mAllAppsStore.getApps().length)
                .log(LAUNCHER_ALLAPPS_COUNT);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // The AllAppsContainerView houses the QSB and is hence visible from the Workspace
        // Overview states. We shouldn't intercept for the scrubber in these cases.
        if (!isInAllApps()) {
            mTouchHandler = null;
            return false;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            AllAppsRecyclerView rv = getActiveRecyclerView();
            if (rv != null && rv.getScrollbar() != null
                    && rv.getScrollbar().isHitInParent(ev.getX(), ev.getY(), mFastScrollerOffset)) {
                mTouchHandler = rv.getScrollbar();
            } else {
                mTouchHandler = null;
            }
        }
        if (mTouchHandler != null) {
            return mTouchHandler.handleTouchEvent(ev, mFastScrollerOffset);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isInAllApps()) {
            return false;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            AllAppsRecyclerView rv = getActiveRecyclerView();
            if (rv != null && rv.getScrollbar() != null
                    && rv.getScrollbar().isHitInParent(ev.getX(), ev.getY(), mFastScrollerOffset)) {
                mTouchHandler = rv.getScrollbar();
            } else {
                mTouchHandler = null;

            }
        }
        if (mTouchHandler != null) {
            mTouchHandler.handleTouchEvent(ev, mFastScrollerOffset);
            return true;
        }
        if (isSearching()
                && mActivityContext.getDragLayer().isEventOverView(getVisibleContainerView(), ev)) {
            // if in search state, consume touch event.
            return true;
        }
        return false;
    }

    /** The current active recycler view (A-Z list from one of the profiles, or search results). */
    public AllAppsRecyclerView getActiveRecyclerView() {
        if (isSearching()) {
            return getSearchRecyclerView();
        }
        return getActiveAppsRecyclerView();
    }

    /** Run some code on all the recycler views. */
    protected void forAllRecyclerViews(Consumer<AllAppsRecyclerView> consumer) {
        for (AdapterHolder holder : mAH) {
            if (holder.mRecyclerView == null) {
                continue;
            }
            consumer.accept(holder.mRecyclerView);
        }
    }

    /** The current focus change listener in the search container. */
    public OnFocusChangeListener getSearchFocusChangeListener() {
        return mAH.get(SEARCH).mOnFocusChangeListener;
    }

    /** The current apps recycler view in the container. */
    private AllAppsRecyclerView getActiveAppsRecyclerView() {
        if (!mUsingTabs || isPersonalTab()) {
            return mAH.get(MAIN).mRecyclerView;
        } else {
            return mAH.get(WORK).mRecyclerView;
        }
    }

    /**
     * The container for A-Z apps (the ViewPager for main+work tabs, or main RV). This is currently
     * hidden while searching.
     */
    public ViewGroup getAppsRecyclerViewContainer() {
        return mViewPager != null ? mViewPager : findViewById(R.id.apps_list_view);
    }

    /** The RV for search results, which is hidden while A-Z apps are visible. */
    public SearchRecyclerView getSearchRecyclerView() {
        return mSearchRecyclerView;
    }

    protected boolean isPersonalTab() {
        return mViewPager == null || mViewPager.getNextPage() == 0;
    }

    /**
     * Switches the current page to the provided {@code tab} if tabs are supported, otherwise does
     * nothing.
     */
    public void switchToTab(int tab) {
        if (mUsingTabs) {
            mViewPager.setCurrentPage(tab);
        }
    }

    public LayoutInflater getLayoutInflater() {
        return mSearchUiDelegate.getLayoutInflater();
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {}

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
        DeviceProfile grid = mActivityContext.getDeviceProfile();

        applyAdapterSideAndBottomPaddings(grid);

        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        // Ignore left/right insets on tablet because we are already centered in-screen.
        if (grid.getDeviceProperties().isTablet()) {
            mlp.leftMargin = mlp.rightMargin = 0;
        } else {
            mlp.leftMargin = insets.left;
            mlp.rightMargin = insets.right;
        }
        setLayoutParams(mlp);

        if (!grid.isVerticalBarLayout() || FeatureFlags.enableResponsiveWorkspace()) {
            int topPadding = grid.allAppsPadding.top;
            if (isSearchBarFloating()) {
                topPadding = insets.top;
            }
            setPadding(grid.allAppsLeftRightMargin, topPadding, grid.allAppsLeftRightMargin, 0);
        }
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    /**
     * Returns a padding in case a scrim is shown on the bottom of the view and a padding is needed.
     */
    protected int computeNavBarScrimHeight(WindowInsets insets) {
        return 0;
    }

    /**
     * Returns the current height of nav bar scrim
     */
    public int getNavBarScrimHeight() {
        return mNavBarScrimHeight;
    }

    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        mNavBarScrimHeight = computeNavBarScrimHeight(insets);
        applyAdapterSideAndBottomPaddings(mActivityContext.getDeviceProfile());
        return super.dispatchApplyWindowInsets(insets);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isSearchBarFloating()) {
            View rvContainer = getAppsRecyclerViewContainer();
            SearchRecyclerView searchRV = getSearchRecyclerView();
            float cardAlpha = getCardBgAlpha();

            // 1. Suggestions Row Card
            if (mSuggestionsCardBackground != null && mHeader != null
                    && mHeader.getVisibility() == VISIBLE && mHeader.getFloatingRowsHeight() > 0) {
                FloatingHeaderRow predictionRow = mHeader.findFixedRowByType(PredictionRowView.class);
                if (predictionRow != null && predictionRow.hasVisibleContent()) {
                    View predView = (View) predictionRow;
                    if (predView.getVisibility() == VISIBLE && predView.getAlpha() > 0.01f) {
                        int left = rvContainer != null ? rvContainer.getLeft() : 0;
                        int right = rvContainer != null ? rvContainer.getRight() : getWidth();
                        int suggestionsTop = (int) (mHeader.getTop() + predView.getTop()
                                + mHeader.getTranslationY() + predView.getTranslationY());
                        int suggestionsBottom = suggestionsTop + predView.getHeight();
                        float scale = predView.getScaleX();
                        int centerX = (left + right) / 2;
                        int halfW = (int) ((right - left) * scale / 2f);
                        int centerY = (suggestionsTop + suggestionsBottom) / 2;
                        int halfH = (int) (predView.getHeight() * scale / 2f);

                        mSuggestionsCardBackground.setBounds(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH);
                        mSuggestionsCardBackground.setAlpha((int) (255 * cardAlpha * predView.getAlpha()));
                        mSuggestionsCardBackground.draw(canvas);
                    }
                }
            }

            // 2. A-Z Apps Card
            if (mCardBackground != null && rvContainer != null
                    && rvContainer.getVisibility() == VISIBLE && rvContainer.getAlpha() > 0.01f) {
                int left = rvContainer.getLeft();
                int right = rvContainer.getRight();
                float scale = rvContainer.getScaleX();
                int centerX = (left + right) / 2;
                int halfW = (int) ((right - left) * scale / 2f);
                int centerY = (mCardTop + rvContainer.getBottom()) / 2;
                int halfH = (int) ((rvContainer.getBottom() - mCardTop) * scale / 2f);

                mCardBackground.setBounds(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH);
                mCardBackground.setAlpha((int) (255 * cardAlpha * rvContainer.getAlpha()));
                mCardBackground.draw(canvas);
            }

            // 3. Search Results Card
            if (mCardBackground != null && searchRV != null
                    && searchRV.getVisibility() == VISIBLE && searchRV.getAlpha() > 0.01f) {
                int left = searchRV.getLeft() > 0 ? searchRV.getLeft() : (rvContainer != null ? rvContainer.getLeft() : 0);
                int right = searchRV.getRight() > 0 ? searchRV.getRight() : (rvContainer != null ? rvContainer.getRight() : getWidth());
                int bottom = searchRV.getBottom() > 0 ? searchRV.getBottom() : (rvContainer != null ? rvContainer.getBottom() : getHeight());
                int top = mHeader != null && mHeader.getTop() > 0 ? mHeader.getTop() : mInsets.top;
                float scale = searchRV.getScaleX();
                int transY = (int) searchRV.getTranslationY();
                int searchTop = top + transY;
                int searchBottom = bottom + transY;
                int centerX = (left + right) / 2;
                int halfW = (int) ((right - left) * scale / 2f);
                int centerY = (searchTop + searchBottom) / 2;
                int halfH = (int) ((searchBottom - searchTop) * scale / 2f);

                mCardBackground.setBounds(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH);
                mCardBackground.setAlpha((int) (255 * cardAlpha * searchRV.getAlpha()));
                mCardBackground.draw(canvas);
            }
        }
        super.dispatchDraw(canvas);

        if (mNavBarScrimHeight > 0) {
            float left = (getWidth() - getWidth() / getScaleX()) / 2;
            float top = getHeight() / 2f + (getHeight() / 2f - mNavBarScrimHeight) / getScaleY();
            canvas.drawRect(left, top, getWidth() / getScaleX(),
                    top + mNavBarScrimHeight / getScaleY(), mNavBarScrimPaint);
        }
    }

    protected void setScrollbarVisibility(boolean visible) {
        AllAppsRecyclerView rv = getActiveRecyclerView();
        if (rv != null && rv.getScrollbar() != null) {
            rv.getScrollbar().setVisibility(visible ? VISIBLE : GONE);
        }
    }

    protected void updateSearchResultsVisibility() {
        if (isSearching()) {
            getSearchRecyclerView().setVisibility(VISIBLE);
            getAppsRecyclerViewContainer().setVisibility(GONE);
            mHeader.setVisibility(GONE);
        } else {
            getSearchRecyclerView().setVisibility(GONE);
            getAppsRecyclerViewContainer().setVisibility(VISIBLE);
            mHeader.setVisibility(VISIBLE);
        }
        if (mHeader.isSetUp()) {
            mHeader.setActiveRV(getCurrentPage());
        }
    }

    public void updateSearchRecyclerViewImePadding(int imeBottom) {
        if (mSearchImeBottom != imeBottom) {
            mSearchImeBottom = imeBottom;
            if (mAH != null && mAH.size() > SEARCH) {
                AdapterHolder searchHolder = mAH.get(SEARCH);
                if (searchHolder != null) {
                    searchHolder.applyPadding();
                }
            }
        }
    }

    private void applyAdapterSideAndBottomPaddings(DeviceProfile grid) {
        int bottomPadding = isSearchBarFloating()
                ? getResources().getDimensionPixelSize(R.dimen.all_apps_list_bottom_spacing)
                : Math.max(mInsets.bottom, mNavBarScrimHeight);
        int sideMargin = isSearchBarFloating()
                ? getResources().getDimensionPixelSize(R.dimen.all_apps_list_margin_x)
                : 0;
        mAH.forEach(adapterHolder -> {
            adapterHolder.mPadding.bottom = bottomPadding;
            adapterHolder.mPadding.left = Math.max(0, grid.allAppsPadding.left - sideMargin);
            adapterHolder.mPadding.right = Math.max(0, grid.allAppsPadding.right - sideMargin);
            adapterHolder.applyPadding();
        });
    }

    private void setDeviceManagementResources() {
        if (mActivityContext.getStringCache() != null) {
            Button personalTab = findViewById(R.id.tab_personal);
            personalTab.setText(mActivityContext.getStringCache().allAppsPersonalTab);

            Button workTab = findViewById(R.id.tab_work);
            workTab.setText(mActivityContext.getStringCache().allAppsWorkTab);
        }
    }

    /**
     * Returns true if the container has work apps.
     */
    public boolean shouldShowTabs() {
        return mHasWorkApps;
    }

    // Used by tests only
    private boolean isDescendantViewVisible(int viewId) {
        final View view = findViewById(viewId);
        if (view == null) return false;

        if (!view.isShown()) return false;

        return view.getGlobalVisibleRect(new Rect());
    }

    /** Called in Launcher#bindStringCache() to update the UI when cache is updated. */
    public void updateWorkUI() {
        setDeviceManagementResources();
        if (mWorkManager.getWorkUtilityView() != null) {
            mWorkManager.getWorkUtilityView().updateStringFromCache();
        }
        inflateWorkCardsIfNeeded();
    }

    private void inflateWorkCardsIfNeeded() {
        AllAppsRecyclerView workRV = mAH.get(WORK).mRecyclerView;
        if (workRV != null) {
            for (int i = 0; i < workRV.getChildCount(); i++) {
                View currentView  = workRV.getChildAt(i);
                int currentItemViewType = workRV.getChildViewHolder(currentView).getItemViewType();
                if (currentItemViewType == VIEW_TYPE_WORK_EDU_CARD) {
                    ((WorkEduCard) currentView).updateStringFromCache();
                } else if (currentItemViewType == VIEW_TYPE_WORK_DISABLED_CARD) {
                    ((WorkPausedCard) currentView).updateStringFromCache();
                }
            }
        }
    }

    @VisibleForTesting
    public void setWorkManager(WorkProfileManager workManager) {
        mWorkManager = workManager;
    }

    @VisibleForTesting
    public boolean isPersonalTabVisible() {
        return isDescendantViewVisible(R.id.tab_personal);
    }

    @VisibleForTesting
    public boolean isWorkTabVisible() {
        return isDescendantViewVisible(R.id.tab_work);
    }

    public AlphabeticalAppsList getSearchResultList() {
        return mAH.get(SEARCH).mAppsList;
    }

    public AlphabeticalAppsList getPersonalAppList() {
        return mAH.get(MAIN).mAppsList;
    }

    public AlphabeticalAppsList getWorkAppList() {
        return mAH.get(WORK).mAppsList;
    }

    public FloatingHeaderView getFloatingHeaderView() {
        return mHeader;
    }

    @VisibleForTesting
    public View getContentView() {
        return isSearching() ? getSearchRecyclerView() : getAppsRecyclerViewContainer();
    }

    /** The current page visible in all apps. */
    public int getCurrentPage() {
        return isSearching()
                ? SEARCH
                : mViewPager == null ? MAIN : mViewPager.getNextPage();
    }

    public PrivateProfileManager getPrivateProfileManager() {
        return mPrivateProfileManager;
    }

    /**
     * Adds an update listener to animator that adds springs to the animation.
     */
    public void addSpringFromFlingUpdateListener(ValueAnimator animator,
            float velocity /* release velocity */,
            float progress /* portion of the distance to travel*/) {
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                float distance = (1 - progress) * getHeight(); // px
                float settleVelocity = Math.min(0, distance
                        / (AllAppsTransitionController.INTERP_COEFF * animator.getDuration())
                        + velocity);
                absorbSwipeUpVelocity(Math.max(1000, Math.abs(
                        Math.round(settleVelocity * FLING_VELOCITY_MULTIPLIER))));
            }
        });
    }

    /** Invoked when the container is pulled. */
    public void onPull(float deltaDistance, float displacement) {
        absorbPullDeltaDistance(PULL_MULTIPLIER * deltaDistance, PULL_MULTIPLIER * displacement);
        // Current motion spec is to actually push and not pull
        // on this surface. However, until EdgeEffect.onPush (b/190612804) is
        // implemented at view level, we will simply pull
    }

    @Override
    public void getDrawingRect(Rect outRect) {
        super.getDrawingRect(outRect);
        outRect.offset(0, (int) getTranslationY());
    }

    @Override
    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        invalidateHeader();
    }

    @Override
    public void setScaleY(float scaleY) {
        super.setScaleY(scaleY);
        if (mNavBarScrimHeight > 0) {
            // Call invalidate to prevent navbar scrim from scaling. The navbar scrim is drawn
            // directly onto the canvas. To prevent it from being scaled with the canvas, there's a
            // counter scale applied in dispatchDraw.
            invalidate(20, getHeight() - mNavBarScrimHeight, getWidth(), getHeight());
        }
    }

    /**
     * Set {@link Animator.AnimatorListener} on {@link #mAllAppsTransitionController} to observe
     * animation of backing out of all apps search view to all apps view.
     */
    public void setAllAppsSearchBackAnimatorListener(Animator.AnimatorListener listener) {
        Preconditions.assertNotNull(mAllAppsTransitionController);
        if (mAllAppsTransitionController == null) {
            return;
        }
        mAllAppsTransitionController.setAllAppsSearchBackAnimationListener(listener);
    }

    public void setScrimView(ScrimView scrimView) {
        mScrimView = scrimView;
    }

    @Override
    public void drawOnScrimWithScaleAndBottomOffset(
            Canvas canvas, float scale, @Px int bottomOffsetPx) {
        final View panel = mBottomSheetBackground;
        final boolean hasBottomSheet = panel.getVisibility() == VISIBLE;
        final float translationY = ((View) panel.getParent()).getTranslationY();

        final float horizontalScaleOffset = (1 - scale) * panel.getWidth() / 2;
        final float verticalScaleOffset = (1 - scale) * (panel.getHeight() - getHeight() / 2);
        // Left and right insets can be applied to this container, as well as the panel.
        float left = getLeft() + panel.getLeft();
        float right = left + panel.getWidth();

        final float topNoScale = panel.getTop() + translationY;
        final float topWithScale = topNoScale + verticalScaleOffset;
        final float leftWithScale = left + horizontalScaleOffset;
        final float rightWithScale = right - horizontalScaleOffset;
        final float bottomWithOffset = panel.getBottom() + bottomOffsetPx;
        // Draw full background panel if presenting on a sheet.
        int bottomSheetBackgroundColor = getBottomSheetBackgroundColor();
        float bottomSheetBackgroundAlpha = (Color.alpha(bottomSheetBackgroundColor) / 255.0f) * getCommonBgAlpha();
        if (hasBottomSheet) {
            mHeaderPaint.setColor(bottomSheetBackgroundColor);
            mHeaderPaint.setAlpha((int) (bottomSheetBackgroundAlpha * 255));

            mTmpRectF.set(
                    leftWithScale,
                    topWithScale,
                    rightWithScale,
                    bottomWithOffset);
            mTmpPath.reset();
            mTmpPath.addRoundRect(mTmpRectF, mBottomSheetCornerRadii, Direction.CW);
            canvas.drawPath(mTmpPath, mHeaderPaint);

            // When the background panel is blurred (or fallback), we don't add header protection.
            // TODO (b/414671116): Apply header protection whenever search bar is focused.
            if (Flags.allAppsBlur()) {
                return;
            }
        }

        if (DEBUG_HEADER_PROTECTION) {
            mHeaderPaint.setColor(Color.MAGENTA);
            mHeaderPaint.setAlpha(255);
        } else {
            mHeaderPaint.setColor(mHeaderColor);
            mHeaderPaint.setAlpha((int) (getAlpha() * Color.alpha(mHeaderColor)));
        }

        // If header is not visible or only differs from the background with alpha, don't draw it.
        int headerWithoutAlpha = ColorUtils.setAlphaComponent(mHeaderPaint.getColor(), 0);
        int backgroundWithoutAlpha = ColorUtils.setAlphaComponent(getBackgroundColor(), 0);
        if (headerWithoutAlpha == backgroundWithoutAlpha || mHeaderPaint.getColor() == 0) {
            return;
        }

        if (hasBottomSheet) {
            mHeaderPaint.setAlpha((int) (mHeaderPaint.getAlpha() * bottomSheetBackgroundAlpha));
        }

        // Draw header on background panel
        final float headerBottomNoScale =
                getHeaderBottom() + getVisibleContainerView().getPaddingTop();
        final float headerHeightNoScale = headerBottomNoScale - topNoScale;
        final float headerBottomWithScaleOnTablet = topWithScale + headerHeightNoScale * scale;
        final float headerBottomOffset = (getVisibleContainerView().getHeight() * (1 - scale) / 2);
        final float headerBottomWithScaleOnPhone = headerBottomNoScale * scale + headerBottomOffset;
        final FloatingHeaderView headerView = getFloatingHeaderView();
        if (hasBottomSheet) {
            // Start adding header protection if search bar or tabs will attach to the top.
            if (!isSearchBarFloating() || mUsingTabs) {
                mTmpRectF.set(
                        leftWithScale,
                        topWithScale,
                        rightWithScale,
                        headerBottomWithScaleOnTablet);
                mTmpPath.reset();
                mTmpPath.addRoundRect(mTmpRectF, mBottomSheetCornerRadii, Direction.CW);
                canvas.drawPath(mTmpPath, mHeaderPaint);
            }
        } else {
            if (!isSearchBarFloating()) {
                canvas.drawRect(0, 0, canvas.getWidth(), headerBottomWithScaleOnPhone, mHeaderPaint);
            }
        }

        // If tab exist (such as work profile), extend header with tab height
        final int tabsHeight = headerView.getPeripheralProtectionHeight(/* expectedHeight */ false);
        if (mTabsProtectionAlpha > 0 && tabsHeight != 0) {
            if (DEBUG_HEADER_PROTECTION) {
                mHeaderPaint.setColor(Color.BLUE);
                mHeaderPaint.setAlpha(255);
            } else {
                float tabAlpha = getAlpha() * mTabsProtectionAlpha;
                if (hasBottomSheet) {
                    tabAlpha *= bottomSheetBackgroundAlpha;
                }
                mHeaderPaint.setAlpha((int) tabAlpha);
            }
            left = 0f;
            right = canvas.getWidth();
            if (hasBottomSheet) {
                left = leftWithScale;
                right = rightWithScale;
            }

            final float tabTopWithScale = hasBottomSheet
                    ? headerBottomWithScaleOnTablet
                    : headerBottomWithScaleOnPhone;
            final float tabBottomWithScale = tabTopWithScale + tabsHeight * scale;

            canvas.drawRect(
                    left,
                    tabTopWithScale,
                    right,
                    tabBottomWithScale,
                    mHeaderPaint);
        }
    }

    /**
     * The height of the header protection as if the user scrolled down the app list.
     */
    float getHeaderProtectionHeight() {
        float headerBottom = getHeaderBottom() - getTranslationY();
        if (mUsingTabs) {
            return headerBottom + mHeader.getPeripheralProtectionHeight(/* expectedHeight */ true);
        } else {
            return headerBottom;
        }
    }

    ConstraintLayout getFastScrollerLetterList() {
        return mFastScrollLetterLayout;
    }

    /**
     * redraws header protection
     */
    public void invalidateHeader() {
        if (mScrimView != null) {
            mScrimView.invalidate();
        }
    }

    /** Returns the position of the bottom edge of the header */
    public int getHeaderBottom() {
        int bottom = (int) getTranslationY() + mHeader.getClipTop();
        if (isSearchBarFloating()) {
            if (mActivityContext.getDeviceProfile().shouldShowAllAppsOnSheet()) {
                return bottom + mBottomSheetBackground.getTop();
            }
            return bottom;
        }
        return bottom + mHeader.getTop();
    }

    boolean isUsingTabs() {
        return mUsingTabs;
    }

    /**
     * Returns a view that denotes the visible part of all apps container view.
     */
    public View getVisibleContainerView() {
        return mBottomSheetBackground.getVisibility() == VISIBLE ? mBottomSheetBackground : this;
    }

    protected void onInitializeRecyclerView(RecyclerView rv) {
        rv.addOnScrollListener(mScrollListener);
        mSearchUiDelegate.onInitializeRecyclerView(rv);
    }

    /** Returns the instance of @{code SearchTransitionController}. */
    public SearchTransitionController getSearchTransitionController() {
        return mSearchTransitionController;
    }

    /** Holds a {@link BaseAllAppsAdapter} and related fields. */
    public class AdapterHolder {
        public static final int MAIN = 0;
        public static final int WORK = 1;
        public static final int SEARCH = 2;

        private final int mType;
        public final BaseAllAppsAdapter mAdapter;
        final RecyclerView.LayoutManager mLayoutManager;
        final AlphabeticalAppsList mAppsList;
        final Rect mPadding = new Rect();
        AllAppsRecyclerView mRecyclerView;
        private OnFocusChangeListener mOnFocusChangeListener;

        AdapterHolder(int type, AlphabeticalAppsList appsList) {
            mType = type;
            mAppsList = appsList;
            mAdapter = createAdapter(mAppsList);
            mAppsList.setAdapter(mAdapter);
            mLayoutManager = mAdapter.getLayoutManager();
        }

        void setup(@NonNull View rv, @Nullable Predicate<ItemInfo> matcher) {
            mAppsList.updateItemFilter(matcher);
            mRecyclerView = (AllAppsRecyclerView) rv;
            mRecyclerView.bindFastScrollbar(mFastScroller, ALL_APPS_SCROLLER);
            mRecyclerView.setEdgeEffectFactory(createEdgeEffectFactory());
            mRecyclerView.setApps(mAppsList);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setHasFixedSize(true);
            if (isSearch()) {
                mRecyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
            } else {
                // No animations will occur when changes occur to the items in this RecyclerView.
                mRecyclerView.setItemAnimator(null);
            }
            onInitializeRecyclerView(mRecyclerView);
            // Use ViewGroupFocusHelper for SearchRecyclerView to draw focus outline for the
            // buttons in the view (e.g. query builder button and setting button)
            FocusedItemDecorator focusedItemDecorator = isSearch() ? new FocusedItemDecorator(
                    new ViewGroupFocusHelper(mRecyclerView)) : new FocusedItemDecorator(
                    mRecyclerView);
            mRecyclerView.addItemDecoration(focusedItemDecorator);
            mOnFocusChangeListener = focusedItemDecorator.getFocusListener();
            mAdapter.setIconFocusListener(mOnFocusChangeListener);
            applyPadding();
        }

        void applyPadding() {
            if (mRecyclerView != null) {
                int bottomOffset = 0;
                if (isWork() && mWorkManager.getWorkUtilityView() != null) {
                    bottomOffset =
                            mInsets.bottom + mWorkManager.getWorkUtilityView().getTotalHeight();
                } else if (isMain() && mPrivateProfileManager != null) {
                    Optional<AdapterItem> privateSpaceHeaderItem = mAppsList.getAdapterItems()
                            .stream()
                            .filter(item -> item.viewType == VIEW_TYPE_PRIVATE_SPACE_HEADER)
                            .findFirst();
                    if (privateSpaceHeaderItem.isPresent()) {
                        bottomOffset = mPrivateSpaceBottomExtraSpace;
                    }
                }
                if (isSearchBarFloating()) {
                    if (isSearch() && mSearchImeBottom > 0) {
                        int navBottom = mInsets.bottom;
                        if (mSearchImeBottom > navBottom) {
                            bottomOffset += (mSearchImeBottom - navBottom);
                        }
                    }
                }
                mRecyclerView.setPadding(mPadding.left, mPadding.top, mPadding.right,
                        mPadding.bottom + bottomOffset);
            }
        }

        private boolean isWork() {
            return mType == WORK;
        }

        private boolean isSearch() {
            return mType == SEARCH;
        }

        private boolean isMain() {
            return mType == MAIN;
        }
    }
}
