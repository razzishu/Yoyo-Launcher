/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.yoyo.launcher.settings;

import static android.os.Process.myUserHandle;
import static android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED;

import static androidx.preference.PreferenceFragmentCompat.ARG_PREFERENCE_ROOT;

import static com.yoyo.launcher.BuildConfig.IS_DEBUG_DEVICE;
import static com.yoyo.launcher.BuildConfig.IS_STUDIO_BUILD;
import static com.yoyo.launcher.InvariantDeviceProfile.TYPE_MULTI_DISPLAY;
import static com.yoyo.launcher.InvariantDeviceProfile.TYPE_TABLET;
import static com.yoyo.launcher.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherApps;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toolbar;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroup.PreferencePositionCallback;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.yoyo.launcher.BuildConfig;
import com.yoyo.launcher.Flags;
import com.yoyo.launcher.InvariantDeviceProfile;
import com.yoyo.launcher.LauncherFiles;
import com.yoyo.launcher.LauncherPrefs;
import com.yoyo.launcher.R;
import com.yoyo.launcher.Utilities;
import com.yoyo.launcher.lineage.LineageUtils;
import com.yoyo.launcher.lineage.trust.TrustAppsActivity;
import com.yoyo.launcher.states.RotationHelper;
import com.yoyo.launcher.util.DisplayController;
import com.yoyo.launcher.util.SettingsCache;
import com.yoyo.launcher.util.Themes;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends FragmentActivity
        implements OnPreferenceStartFragmentCallback, OnPreferenceStartScreenCallback {

    @VisibleForTesting
    static final String DEVELOPER_OPTIONS_KEY = "pref_developer_options";

    public static final String FIXED_LANDSCAPE_MODE = "pref_fixed_landscape_mode";

    private static final String NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging";

    public static final String EXTRA_FRAGMENT_ARGS = ":settings:fragment_args";

    public static final String EXTRA_FRAGMENT = ":settings:fragment";

    // Intent extra to indicate the pref-key to highlighted when opening the settings activity
    public static final String EXTRA_FRAGMENT_HIGHLIGHT_KEY = ":settings:fragment_args_key";
    // Intent extra to indicate the pref-key of the root screen when opening the settings activity
    public static final String EXTRA_FRAGMENT_ROOT_KEY = ARG_PREFERENCE_ROOT;

    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    private static final String KEY_MINUS_ONE = "pref_enable_minus_one";
    private static final String SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";
    public static final String KEY_TRUST_APPS = "pref_trust_apps";

    private static final String KEY_SUGGESTIONS = "pref_suggestions";
    private static final String SUGGESTIONS_PACKAGE = "com.google.android.as";

    private static final int[] TAB_TITLES = new int[] {
            R.string.settings_general_title,
            R.string.settings_themes_title,
            R.string.settings_homescreen_title,
            R.string.settings_app_drawer_title
    };

    private static final int[] TAB_XML = new int[] {
            R.xml.launcher_general_preferences,
            R.xml.launcher_theme_preferences,
            R.xml.launcher_homescreen_preferences,
            R.xml.launcher_app_drawer_preferences
    };

    private Toolbar mSubToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        mSubToolbar = findViewById(R.id.sub_action_bar);
        if (mSubToolbar != null) {
            mSubToolbar.setVisibility(View.GONE);
            mSubToolbar.setNavigationOnClickListener(v -> handleBackPress());
        }



        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        View contentParent = findViewById(R.id.content_parent);
        if (contentParent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(contentParent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        Intent intent = getIntent();
        boolean hasRoot = intent.hasExtra(EXTRA_FRAGMENT_ROOT_KEY) || intent.hasExtra(EXTRA_FRAGMENT_ARGS)
                || intent.hasExtra(EXTRA_FRAGMENT_HIGHLIGHT_KEY);
        
        if (hasRoot) {
            if (mSubToolbar != null) {
                mSubToolbar.setVisibility(View.VISIBLE);
                mSubToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mSubToolbar.setNavigationOnClickListener(v -> handleBackPress());
            }
            View contentFrame = findViewById(R.id.content_frame);
            if (contentFrame != null) contentFrame.setVisibility(View.VISIBLE);

            if (savedInstanceState == null) {
                Bundle args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);
                if (args == null) {
                    args = new Bundle();
                }

                String highlight = intent.getStringExtra(EXTRA_FRAGMENT_HIGHLIGHT_KEY);
                if (!TextUtils.isEmpty(highlight)) {
                    args.putString(EXTRA_FRAGMENT_HIGHLIGHT_KEY, highlight);
                }
                String root = intent.getStringExtra(EXTRA_FRAGMENT_ROOT_KEY);
                if (!TextUtils.isEmpty(root)) {
                    args.putString(EXTRA_FRAGMENT_ROOT_KEY, root);
                }

                String fragmentName = intent.getStringExtra(EXTRA_FRAGMENT);
                if (TextUtils.isEmpty(fragmentName)) {
                    fragmentName = getString(R.string.settings_fragment_name);
                }

                final FragmentManager fm = getSupportFragmentManager();
                final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(), fragmentName);
                f.setArguments(args);
                // Display the fragment as the main content.
                fm.beginTransaction().replace(R.id.content_frame, f).commit();
            }
        } else {
            View contentFrame = findViewById(R.id.content_frame);
            if (contentFrame != null) contentFrame.setVisibility(View.VISIBLE);
            if (mSubToolbar != null) {
                mSubToolbar.setVisibility(View.GONE);
                mSubToolbar.setTitle("");
                mSubToolbar.setNavigationIcon(null);
            }

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new MainSettingsFragment())
                        .commit();
            }

            getSupportFragmentManager().addOnBackStackChangedListener(this::updateToolbarState);
            setupPredictiveBack();
        }
    }

    private OnBackAnimationCallback mFrameworkBackAnimationCallback;
    private OnBackInvokedCallback mFrameworkBackInvokedCallback;

    private void setupPredictiveBack() {
        if (Utilities.ATLEAST_U) {
            mFrameworkBackAnimationCallback = new OnBackAnimationCallback() {
                private boolean mIsSubFragmentBack = false;
                private View mSubFragmentView = null;
                private View mMainFragmentView = null;
                private View mRootContentView = null;
                private int mSwipeEdge = BackEvent.EDGE_LEFT;

                @Override
                public void onBackStarted(@NonNull BackEvent backEvent) {
                    mSwipeEdge = backEvent.getSwipeEdge();
                    int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
                    mIsSubFragmentBack = (backStackCount > 0);

                    if (mIsSubFragmentBack) {
                        ViewGroup container = findViewById(R.id.content_frame);
                        if (container != null && container.getChildCount() > 1) {
                            mSubFragmentView = container.getChildAt(container.getChildCount() - 1);
                            mMainFragmentView = container.getChildAt(container.getChildCount() - 2);
                            if (mMainFragmentView != null) {
                                mMainFragmentView.setVisibility(View.VISIBLE);
                                mMainFragmentView.setTranslationX(-mMainFragmentView.getWidth() * 0.25f);
                                mMainFragmentView.setAlpha(0.7f);
                            }
                        } else if (container != null && container.getChildCount() == 1) {
                            mSubFragmentView = container.getChildAt(0);
                            mMainFragmentView = null;
                        }
                    } else {
                        mRootContentView = findViewById(R.id.content_parent);
                        if (mRootContentView != null) {
                            mRootContentView.setPivotX(mSwipeEdge == BackEvent.EDGE_LEFT ? 0 : mRootContentView.getWidth());
                            mRootContentView.setPivotY(backEvent.getTouchY());
                        }
                    }
                }

                @Override
                public void onBackProgressed(@NonNull BackEvent backEvent) {
                    float progress = backEvent.getProgress();
                    float p = 1.0f - (float) Math.pow(1.0f - progress, 2);

                    if (mIsSubFragmentBack) {
                        if (mSubFragmentView != null) {
                            float width = mSubFragmentView.getWidth();
                            mSubFragmentView.setTranslationX(p * width);
                            mSubFragmentView.setAlpha(1.0f - (p * 0.3f));
                        }
                        if (mMainFragmentView != null) {
                            float width = mMainFragmentView.getWidth();
                            mMainFragmentView.setTranslationX(-width * 0.25f * (1.0f - p));
                            mMainFragmentView.setAlpha(0.7f + (0.3f * p));
                        }
                        if (mSubToolbar != null) {
                            mSubToolbar.setTranslationX(p * mSubToolbar.getWidth());
                            mSubToolbar.setAlpha(1.0f - p);
                        }
                    } else {
                        if (mRootContentView != null) {
                            float scale = 1.0f - (p * 0.12f);
                            mRootContentView.setScaleX(scale);
                            mRootContentView.setScaleY(scale);
                            float shift = (mSwipeEdge == BackEvent.EDGE_LEFT ? 1 : -1) * p * (mRootContentView.getWidth() * 0.05f);
                            mRootContentView.setTranslationX(shift);
                        }
                    }
                }

                @Override
                public void onBackInvoked() {
                    if (mIsSubFragmentBack) {
                        final View subViewToAnimate = mSubFragmentView;
                        final View mainViewToAnimate = mMainFragmentView;
                        if (subViewToAnimate != null) {
                            subViewToAnimate.animate()
                                    .translationX(subViewToAnimate.getWidth())
                                    .alpha(0f)
                                    .setDuration(180)
                                    .withEndAction(() -> {
                                        if (mainViewToAnimate != null) {
                                            mainViewToAnimate.setTranslationX(0f);
                                            mainViewToAnimate.setAlpha(1f);
                                        }
                                        getSupportFragmentManager().popBackStack();
                                    })
                                    .start();
                        } else {
                            getSupportFragmentManager().popBackStack();
                        }
                        if (mainViewToAnimate != null) {
                            mainViewToAnimate.animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .setDuration(180)
                                    .start();
                        }
                        if (mSubToolbar != null) {
                            mSubToolbar.setVisibility(View.GONE);
                            mSubToolbar.setTranslationX(0f);
                            mSubToolbar.setAlpha(1f);
                        }
                    } else {
                        if (mRootContentView != null) {
                            mRootContentView.animate()
                                    .scaleX(0.80f)
                                    .scaleY(0.80f)
                                    .alpha(0f)
                                    .setDuration(150)
                                    .withEndAction(() -> {
                                        finish();
                                        overridePendingTransition(0, 0);
                                    })
                                    .start();
                        } else {
                            finish();
                        }
                    }
                    mSubFragmentView = null;
                    mMainFragmentView = null;
                    mRootContentView = null;
                }

                @Override
                public void onBackCancelled() {
                    if (mIsSubFragmentBack) {
                        final View subViewToRestore = mSubFragmentView;
                        final View mainViewToRestore = mMainFragmentView;
                        if (subViewToRestore != null) {
                            subViewToRestore.animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start();
                        }
                        if (mainViewToRestore != null) {
                            mainViewToRestore.animate()
                                    .translationX(-mainViewToRestore.getWidth() * 0.25f)
                                    .alpha(0.7f)
                                    .setDuration(200)
                                    .withEndAction(() -> {
                                        mainViewToRestore.setVisibility(View.GONE);
                                    })
                                    .start();
                        }
                        if (mSubToolbar != null) {
                            mSubToolbar.animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start();
                        }
                    } else {
                        if (mRootContentView != null) {
                            mRootContentView.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .translationX(0f)
                                    .setDuration(200)
                                    .start();
                        }
                    }
                    mSubFragmentView = null;
                    mMainFragmentView = null;
                    mRootContentView = null;
                }
            };
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, mFrameworkBackAnimationCallback);
        } else if (Utilities.ATLEAST_T) {
            mFrameworkBackInvokedCallback = this::handleBackPress;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, mFrameworkBackInvokedCallback);
        }
    }

    public void handleBackPress() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Utilities.ATLEAST_U && mFrameworkBackAnimationCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(mFrameworkBackAnimationCallback);
        } else if (Utilities.ATLEAST_T && mFrameworkBackInvokedCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(mFrameworkBackInvokedCallback);
        }
    }

    public void updateToolbarState() {
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        boolean isMainScreen = (backStackCount == 0);
        if (isMainScreen) {
            if (mSubToolbar != null && mSubToolbar.getVisibility() == View.VISIBLE) {
                mSubToolbar.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            mSubToolbar.setVisibility(View.GONE);
                            mSubToolbar.setAlpha(1f);
                            mSubToolbar.setTranslationX(0f);
                            mSubToolbar.setTitle("");
                            mSubToolbar.setNavigationIcon(null);
                        })
                        .start();
            }
            ViewGroup container = findViewById(R.id.content_frame);
            if (container != null && container.getChildCount() > 0) {
                View mainView = container.getChildAt(0);
                if (mainView != null) {
                    mainView.setVisibility(View.VISIBLE);
                    mainView.setTranslationX(0f);
                    mainView.setAlpha(1f);
                }
            }
        } else {
            if (mSubToolbar != null) {
                mSubToolbar.animate().cancel();
                mSubToolbar.setVisibility(View.VISIBLE);
                mSubToolbar.setAlpha(1f);
                mSubToolbar.setTranslationX(0f);
                mSubToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mSubToolbar.setNavigationOnClickListener(v -> handleBackPress());
            }
        }
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackCount > 0 && mSubToolbar != null && !TextUtils.isEmpty(title)) {
            mSubToolbar.setTitle(title);
        }
    }

    public void openSubSettings(String fragmentClass, int xmlResId, String title) {
        if (getSupportFragmentManager().isStateSaved()) return;

        Fragment fragment;
        if (!TextUtils.isEmpty(fragmentClass)) {
            fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), fragmentClass);
        } else {
            fragment = LauncherSettingsFragment.newInstance(xmlResId);
        }

        Fragment current = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(
                R.anim.settings_fragment_enter,
                R.anim.settings_fragment_exit,
                R.anim.settings_fragment_pop_enter,
                R.anim.settings_fragment_pop_exit);
        if (current != null) {
            ft.hide(current);
        }
        ft.add(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();

        if (mSubToolbar != null) {
            mSubToolbar.animate().cancel();
            mSubToolbar.setVisibility(View.VISIBLE);
            mSubToolbar.setAlpha(1f);
            mSubToolbar.setTranslationX(0f);
            mSubToolbar.setTitle(title);
            mSubToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mSubToolbar.setNavigationOnClickListener(v -> handleBackPress());
        }
    }

    private boolean startPreference(String fragment, Bundle args, String key) {
        if (getSupportFragmentManager().isStateSaved()) {
            // Sometimes onClick can come after onPause because of being posted on the handler.
            // Skip starting new preferences in that case.
            return false;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(), fragment);
        if (f instanceof DialogFragment) {
            f.setArguments(args);
            ((DialogFragment) f).show(fm, key);
        } else {
            if (args != null) {
                f.setArguments(args);
            }
            Fragment current = fm.findFragmentById(R.id.content_frame);
            FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(
                    R.anim.settings_fragment_enter,
                    R.anim.settings_fragment_exit,
                    R.anim.settings_fragment_pop_enter,
                    R.anim.settings_fragment_pop_exit);
            if (current != null) {
                ft.hide(current);
            }
            ft.add(R.id.content_frame, f)
                    .addToBackStack(null)
                    .commit();

            if (mSubToolbar != null) {
                mSubToolbar.animate().cancel();
                mSubToolbar.setVisibility(View.VISIBLE);
                mSubToolbar.setAlpha(1f);
                mSubToolbar.setTranslationX(0f);
                mSubToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mSubToolbar.setNavigationOnClickListener(v -> handleBackPress());
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(
            PreferenceFragmentCompat preferenceFragment, Preference pref) {
        if (!TextUtils.isEmpty(pref.getFragment())) {
            try {
                Class<?> clazz = Class.forName(pref.getFragment());
                if (DialogFragment.class.isAssignableFrom(clazz)) {
                    return startPreference(pref.getFragment(), pref.getExtras(), pref.getKey());
                }
            } catch (ClassNotFoundException ignored) {
            }
            String title = pref.getTitle() != null ? pref.getTitle().toString() : "";
            openSubSettings(pref.getFragment(), 0, title);
            return true;
        }
        return startPreference(pref.getFragment(), pref.getExtras(), pref.getKey());
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        Log.d("SettingsActivity", "onPreferenceStartScreen: " + pref.getKey());
        Bundle args = new Bundle();
        args.putString(ARG_PREFERENCE_ROOT, pref.getKey());
        if (caller instanceof LauncherSettingsFragment) {
            args.putInt(LauncherSettingsFragment.EXTRA_PREFERENCE_XML,
                    caller.getArguments().getInt(LauncherSettingsFragment.EXTRA_PREFERENCE_XML));
        }
        boolean started = startPreference(getString(R.string.settings_fragment_name), args, pref.getKey());
        if (started && mSubToolbar != null && pref.getTitle() != null) {
            mSubToolbar.setTitle(pref.getTitle());
        }
        return started;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPress();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            theme.applyStyle(
                    com.android.settingslib.widget.theme.R.style.Theme_SubSettingsBase_Expressive,
                    true);
        }
        return theme;
    }

    private static class SettingsPagerAdapter extends FragmentStateAdapter {
        public SettingsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return LauncherSettingsFragment.newInstance(TAB_XML[position]);
        }

        @Override
        public int getItemCount() {
            return TAB_TITLES.length;
        }
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends SettingsBasePreferenceFragment implements
            SettingsCache.OnChangeListener {

        public static final String EXTRA_PREFERENCE_XML = "preference_xml";

        protected boolean mDeveloperOptionsEnabled = false;

        private boolean mRestartOnResume = false;

        private String mHighLightKey;
        private boolean mPreferenceHighlighted = false;

        public static LauncherSettingsFragment newInstance(int xmlResId) {
            LauncherSettingsFragment fragment = new LauncherSettingsFragment();
            Bundle args = new Bundle();
            args.putInt(EXTRA_PREFERENCE_XML, xmlResId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            if (IS_DEBUG_DEVICE) {
                Uri devUri = Settings.Global.getUriFor(DEVELOPMENT_SETTINGS_ENABLED);
                SettingsCache settingsCache = SettingsCache.INSTANCE.get(getContext());
                mDeveloperOptionsEnabled = settingsCache.getValue(devUri);
                settingsCache.register(devUri, this);
            }
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final Bundle args = getArguments();
            mHighLightKey = args == null ? null : args.getString(EXTRA_FRAGMENT_HIGHLIGHT_KEY);
            int xmlResId = args != null ? args.getInt(EXTRA_PREFERENCE_XML, R.xml.launcher_preferences)
                    : R.xml.launcher_preferences;

            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
            }

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(xmlResId, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
            }

            // If the target preference is not in the current preference screen, find the parent
            // preference screen that contains the target preference and set it as the preference
            // screen.
            if (mHighLightKey != null
                    && !isKeyInPreferenceGroup(mHighLightKey, screen)) {
                final PreferenceScreen parentPreferenceScreen =
                        findParentPreference(screen, mHighLightKey);
                if (parentPreferenceScreen != null && getActivity() != null) {
                    if (!TextUtils.isEmpty(parentPreferenceScreen.getTitle())) {
                        getActivity().setTitle(parentPreferenceScreen.getTitle());
                    }
                    setPreferenceScreen(parentPreferenceScreen);
                    return;
                }
            }

            if (getActivity() != null && !TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
                getActivity().setTitle(getPreferenceScreen().getTitle());
            }
        }

        private boolean isKeyInPreferenceGroup(String targetKey, PreferenceGroup parent) {
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference pref = parent.getPreference(i);
                if (pref.getKey() != null && pref.getKey().equals(targetKey)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Finds the parent preference screen for the given target key.
         *
         * @param parent    the parent preference screen
         * @param targetKey the key of the preference to find
         * @return the parent preference screen that contains the target preference
         */
        @Nullable
        private PreferenceScreen findParentPreference(PreferenceScreen parent, String targetKey) {
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference pref = parent.getPreference(i);
                if (pref instanceof PreferenceScreen) {
                    PreferenceScreen foundKey = findParentPreference((PreferenceScreen) pref,
                            targetKey);
                    if (foundKey != null) {
                        return foundKey;
                    }
                } else if (pref.getKey() != null && pref.getKey().equals(targetKey)) {
                    return parent;
                }
            }
            return null;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            View listView = getListView();
            final int bottomPadding = listView.getPaddingBottom();
            listView.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        bottomPadding + insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            });

            // Overriding Text Direction in the Androidx preference library to support RTL
            view.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        }

        private void applyCardStyle(Preference pref, int iconRes, int badgeColorRes) {
            pref.setLayoutResource(R.layout.preference_card_item);
            pref.setIcon(iconRes);
            pref.getExtras().putInt("badge_color", badgeColorRes);
        }

        private boolean checkUsageStatsPermission(Context context) {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }

        private void showUsageAccessPrompt(Context context, TwoStatePreference preference) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.usage_access_dialog_title)
                    .setMessage(R.string.usage_access_dialog_message)
                    .setPositiveButton(R.string.grant_permission, (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                            startActivity(intent);
                        } catch (Exception e) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        if (preference != null) {
                            preference.setChecked(false);
                        }
                    })
                    .setOnCancelListener(dialog -> {
                        if (preference != null) {
                            preference.setChecked(false);
                        }
                    })
                    .show();
        }

        private Preference findPreferenceAt(PreferenceGroup group, int position, int[] counter) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference pref = group.getPreference(i);
                if (pref.isVisible()) {
                    if (counter[0] == position) {
                        return pref;
                    }
                    counter[0]++;
                    if (pref instanceof PreferenceGroup) {
                        Preference child = findPreferenceAt((PreferenceGroup) pref, position, counter);
                        if (child != null) return child;
                    }
                }
            }
            return null;
        }

        @Override
        protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
            RecyclerView.Adapter adapter = super.onCreateAdapter(preferenceScreen);
            RecyclerView.Adapter wrapperAdapter = new RecyclerView.Adapter<PreferenceViewHolder>() {
                @NonNull
                @Override
                public PreferenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    return (PreferenceViewHolder) adapter.onCreateViewHolder(parent, viewType);
                }

                @Override
                public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
                    adapter.onBindViewHolder(holder, position);
                    Preference pref = findPreferenceAt(preferenceScreen, position, new int[]{0});

                    View badgeFrame = holder.findViewById(R.id.icon_badge_frame);
                    if (badgeFrame != null && pref != null && pref.getExtras().containsKey("badge_color")) {
                        int colorRes = pref.getExtras().getInt("badge_color");
                        badgeFrame.getBackground().setTint(holder.itemView.getContext().getColor(colorRes));
                    }
                    ImageView iconView = (ImageView) holder.findViewById(android.R.id.icon);
                    if (iconView != null) {
                        int iconColor = Themes.getAttrColor(holder.itemView.getContext(), android.R.attr.textColorPrimary);
                        iconView.setColorFilter(iconColor);
                    }
                }

                @Override
                public int getItemCount() {
                    return adapter.getItemCount();
                }

                @Override
                public int getItemViewType(int position) {
                    return adapter.getItemViewType(position);
                }

                @Override
                public long getItemId(int position) {
                    return adapter.getItemId(position);
                }
            };

            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    wrapperAdapter.notifyDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    wrapperAdapter.notifyItemRangeChanged(positionStart, itemCount);
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    wrapperAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    wrapperAdapter.notifyItemRangeInserted(positionStart, itemCount);
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    wrapperAdapter.notifyItemRangeRemoved(positionStart, itemCount);
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    wrapperAdapter.notifyItemMoved(fromPosition, toPosition);
                }
            });

            wrapperAdapter.setHasStableIds(adapter.hasStableIds());
            return wrapperAdapter;
        }

        /**
         * Initializes a preference. This is called for every preference. Returning false here
         * will remove that preference from the list.
         */
        protected boolean initPreference(Preference preference) {
            DisplayController.Info info = DisplayController.INSTANCE.get(getContext()).getInfo();
            LauncherApps launcherApps = getContext().getSystemService(LauncherApps.class);
            if (preference.getKey() == null) {
                return true;
            }

            switch (preference.getKey()) {
                case "pref_icon_pack_customization":
                    applyCardStyle(preference, R.drawable.ic_palette, R.color.badge_color_pink);
                    break;

                case NOTIFICATION_DOTS_PREFERENCE_KEY:
                    applyCardStyle(preference, R.drawable.ic_notification_dots, R.color.badge_color_orange);
                    return BuildConfig.NOTIFICATION_DOTS_ENABLED;

                case ALLOW_ROTATION_PREFERENCE_KEY:
                    applyCardStyle(preference, R.drawable.ic_screen_rotation, R.color.badge_color_teal);
                    if (Flags.oneGridSpecs() && !info.isRotationAllowed()) {
                        return false;
                    }
                    if (info.isTablet(info.realBounds)) {
                        return false;
                    }
                    preference.setDefaultValue(RotationHelper.getAllowRotationDefaultValue(info));
                    return true;

                case "pref_launcher_layout":
                    applyCardStyle(preference, R.drawable.ic_grid_layout, R.color.badge_color_blue);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        String layoutVal = (String) newValue;
                        Context ctx = pref.getContext();
                        LauncherPrefs.get(ctx).put(LauncherPrefs.LAUNCHER_LAYOUT, layoutVal);
                        String targetGrid = "5_columns".equals(layoutVal) ? "5_by_5" : "4_by_5";
                        InvariantDeviceProfile.INSTANCE.get(ctx).setCurrentGrid(targetGrid);
                        return true;
                    });
                    return true;

                case KEY_TRUST_APPS:
                    applyCardStyle(preference, R.drawable.ic_lock_security, R.color.badge_color_purple);
                    preference.setOnPreferenceClickListener(p -> {
                        LineageUtils.showLockScreen(getActivity(),
                                getString(R.string.trust_apps_manager_name), () -> {
                            Intent intent = new Intent(getActivity(), TrustAppsActivity.class);
                            startActivity(intent);
                        });
                        return true;
                    });
                    return true;

                case KEY_SUGGESTIONS:
                    applyCardStyle(preference, R.drawable.ic_suggestions, R.color.badge_color_amber);
                    if (launcherApps == null) {
                        return false;
                    }
                    boolean isInstalled = launcherApps.isPackageEnabled(SUGGESTIONS_PACKAGE, myUserHandle())
                            || launcherApps.isPackageEnabled("com.google.android.as.oss", myUserHandle());
                    if (!isInstalled) {
                        preference.setIntent(null);
                        preference.setOnPreferenceClickListener(p -> {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=" + SUGGESTIONS_PACKAGE));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=" 
                                                + SUGGESTIONS_PACKAGE)));
                            }
                            return true;
                        });
                        preference.setSummary(R.string.install_personalization_summary);
                    }
                    return true;

                case "pref_suggestions_all_apps":
                    applyCardStyle(preference, R.drawable.ic_grid_layout, R.color.badge_color_green);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean enabled = (boolean) newValue;
                        Context ctx = pref.getContext();
                        if (enabled && !checkUsageStatsPermission(ctx)) {
                            showUsageAccessPrompt(ctx, (TwoStatePreference) pref);
                            return false;
                        }
                        LauncherPrefs.get(ctx).putSync(LauncherPrefs.SUGGESTIONS_ALL_APPS, enabled);
                        return true;
                    });
                    break;

                case "pref_suggestions_hotseat":
                    applyCardStyle(preference, R.drawable.ic_lock_home, R.color.badge_color_blue);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean enabled = (boolean) newValue;
                        Context ctx = pref.getContext();
                        if (enabled && !checkUsageStatsPermission(ctx)) {
                            showUsageAccessPrompt(ctx, (TwoStatePreference) pref);
                            return false;
                        }
                        LauncherPrefs.get(ctx).putSync(LauncherPrefs.SUGGESTIONS_HOTSEAT, enabled);
                        return true;
                    });
                    break;

                case "pref_workspace_lock":
                    applyCardStyle(preference, R.drawable.ic_lock_home, R.color.badge_color_red);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean val = (boolean) newValue;
                        LauncherPrefs.get(pref.getContext()).putSync(LauncherPrefs.WORKSPACE_LOCK, val);
                        return true;
                    });
                    break;

                case "pref_add_icon_to_home":
                    applyCardStyle(preference, R.drawable.ic_add_home, R.color.badge_color_green);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean val = (boolean) newValue;
                        LauncherPrefs.get(pref.getContext()).putSync(LauncherPrefs.ADD_ICON_TO_HOME, val);
                        return true;
                    });
                    break;

                case KEY_MINUS_ONE:
                    applyCardStyle(preference, R.drawable.ic_google_feed, R.color.badge_color_sky_blue);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean val = (boolean) newValue;
                        LauncherPrefs.get(pref.getContext()).putSync(LauncherPrefs.ENABLE_MINUS_ONE, val);
                        return true;
                    });
                    return launcherApps != null &&
                            launcherApps.isPackageEnabled(SEARCH_PACKAGE, myUserHandle());

                case "pref_sleep_gesture":
                    applyCardStyle(preference, R.drawable.ic_sleep_gesture, R.color.badge_color_indigo);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean val = (boolean) newValue;
                        LauncherPrefs.get(pref.getContext()).putSync(LauncherPrefs.SLEEP_GESTURE, val);
                        return true;
                    });
                    break;

                case "pref_desktop_show_labels":
                    applyCardStyle(preference, R.drawable.ic_labels, R.color.badge_color_mint);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean val = (boolean) newValue;
                        LauncherPrefs.get(pref.getContext()).putSync(LauncherPrefs.SHOW_DESKTOP_LABELS, val);
                        return true;
                    });
                    break;

                case "pref_hotseat_qsb":
                    applyCardStyle(preference, R.drawable.ic_search_qsb, R.color.badge_color_peach);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean qsbVal = (boolean) newValue;
                        Context ctx = pref.getContext();
                        LauncherPrefs.get(ctx).putSync(LauncherPrefs.HOTSEAT_QSB, qsbVal);
                        InvariantDeviceProfile.INSTANCE.get(ctx).onConfigChanged();
                        return true;
                    });
                    break;

                case "pref_drawer_opacity":
                    return true;

                case "pref_drawer_open_keyboard":
                    applyCardStyle(preference, R.drawable.ic_keyboard, R.color.badge_color_light_green);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean val = (boolean) newValue;
                        LauncherPrefs.get(pref.getContext()).putSync(LauncherPrefs.DRAWER_OPEN_KEYBOARD, val);
                        return true;
                    });
                    break;

                case "pref_drawer_show_labels":
                    applyCardStyle(preference, R.drawable.ic_labels, R.color.badge_color_lilac);
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        boolean val = (boolean) newValue;
                        LauncherPrefs.get(pref.getContext()).putSync(LauncherPrefs.SHOW_DRAWER_LABELS, val);
                        return true;
                    });
                    break;

                default:
                    applyCardStyle(preference, R.drawable.ic_setting, R.color.badge_color_blue);
                    break;
            }
            return true;
        }

        @Override
        public void onResume() {
            super.onResume();

            if (isAdded() && !mPreferenceHighlighted) {
                PreferenceHighlighter highlighter = createHighlighter();
                if (highlighter != null) {
                    getView().postDelayed(highlighter, DELAY_HIGHLIGHT_DURATION_MILLIS);
                    mPreferenceHighlighted = true;
                }
            }

            if (mRestartOnResume) {
                recreateActivityNow();
            }
        }

        @Override
        public void onSettingsChanged(boolean isEnabled) {
            // Developer options changed, try recreate
            tryRecreateActivity();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (IS_DEBUG_DEVICE) {
                SettingsCache.INSTANCE.get(getContext())
                        .unregister(Settings.Global.getUriFor(DEVELOPMENT_SETTINGS_ENABLED), this);
            }
        }

        /**
         * Tries to recreate the preference
         */
        protected void tryRecreateActivity() {
            if (isResumed()) {
                recreateActivityNow();
            } else {
                mRestartOnResume = true;
            }
        }

        private void recreateActivityNow() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.recreate();
            }
        }

        private PreferenceHighlighter createHighlighter() {
            if (TextUtils.isEmpty(mHighLightKey)) {
                return null;
            }

            PreferenceScreen screen = getPreferenceScreen();
            if (screen == null) {
                return null;
            }

            RecyclerView list = getListView();
            PreferencePositionCallback callback = (PreferencePositionCallback) list.getAdapter();
            int position = callback.getPreferenceAdapterPosition(mHighLightKey);
            return position >= 0 ? new PreferenceHighlighter(
                    list, position, screen.findPreference(mHighLightKey))
                    : null;
        }
    }
}
