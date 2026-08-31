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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
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
import com.yoyo.launcher.lineage.LineageUtils;
import com.yoyo.launcher.lineage.trust.TrustAppsActivity;
import com.yoyo.launcher.states.RotationHelper;
import com.yoyo.launcher.util.DisplayController;
import com.yoyo.launcher.util.SettingsCache;
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
    private static final String KEY_SUGGESTIONS_USAGE_STATS = "pref_suggestions_usage_stats";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        setActionBar(findViewById(R.id.action_bar));
        setTitle(R.string.settings_button_text);
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
            getActionBar().setDisplayHomeAsUpEnabled(true);
            findViewById(R.id.tabs).setVisibility(View.GONE);
            findViewById(R.id.view_pager).setVisibility(View.GONE);
            findViewById(R.id.content_frame).setVisibility(View.VISIBLE);

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
            setTitle(R.string.settings_button_text);
            TabLayout tabs = findViewById(R.id.tabs);
            ViewPager2 viewPager = findViewById(R.id.view_pager);
            
            viewPager.setAdapter(new SettingsPagerAdapter(this));
            new TabLayoutMediator(tabs, viewPager, (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
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
            startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra(EXTRA_FRAGMENT, fragment)
                    .putExtra(EXTRA_FRAGMENT_ARGS, args));
        }
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(
            PreferenceFragmentCompat preferenceFragment, Preference pref) {
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
        return startPreference(getString(R.string.settings_fragment_name), args, pref.getKey());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
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
                case "pref_launcher_layout":
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        String layoutVal = (String) newValue;
                        Context ctx = pref.getContext();
                        LauncherPrefs.get(ctx).put(LauncherPrefs.LAUNCHER_LAYOUT, layoutVal);
                        String targetGrid = "5_columns".equals(layoutVal) ? "5_by_5" : "4_by_5";
                        InvariantDeviceProfile.INSTANCE.get(ctx).setCurrentGrid(targetGrid);
                        return true;
                    });
                    return true;
                case "pref_allapps_themed_icons":
                    preference.setOnPreferenceChangeListener((p, newValue) -> {
                        Preference homescreenOnly = findPreference("pref_themed_icons_homescreen_only");
                        if (homescreenOnly != null) {
                            homescreenOnly.setVisible((boolean) newValue);
                        }
                        return true;
                    });
                    return true;
                case "pref_themed_icons_homescreen_only":
                    Preference themedIcons = findPreference("pref_allapps_themed_icons");
                    if (themedIcons instanceof TwoStatePreference) {
                        preference.setVisible(((TwoStatePreference) themedIcons).isChecked());
                    }
                    return true;
                case NOTIFICATION_DOTS_PREFERENCE_KEY:
                    return BuildConfig.NOTIFICATION_DOTS_ENABLED;
                case ALLOW_ROTATION_PREFERENCE_KEY:
                    if (Flags.oneGridSpecs() && !info.isRotationAllowed()) {
                        return false;
                    }
                    if (info.isTablet(info.realBounds)) {
                        // Launcher supports rotation by default. No need to show this setting.
                        return false;
                    }
                    // Initialize the UI once
                    preference.setDefaultValue(RotationHelper.getAllowRotationDefaultValue(info));
                    return true;
                case DEVELOPER_OPTIONS_KEY:
                    if (IS_STUDIO_BUILD) {
                        preference.setOrder(0);
                    }
                    return mDeveloperOptionsEnabled;
                case FIXED_LANDSCAPE_MODE:
                    if (!Flags.oneGridSpecs()
                            // adding this condition until fixing b/378972567
                            || InvariantDeviceProfile.INSTANCE.get(getContext()).deviceType
                            == TYPE_MULTI_DISPLAY
                            || InvariantDeviceProfile.INSTANCE.get(getContext()).deviceType
                            == TYPE_TABLET
                            || info.isRotationAllowed()) {
                        return false;
                    }
                    // When the setting changes rotate the screen accordingly to showcase the result
                    // of the setting
                    preference.setOnPreferenceChangeListener(
                            (pref, newValue) -> {
                                getActivity().setRequestedOrientation(
                                        (boolean) newValue
                                                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                                : ActivityInfo.SCREEN_ORIENTATION_USER
                                );
                                return true;
                            }
                    );
                    return !info.isTablet(info.realBounds);
                case KEY_MINUS_ONE:
                    return launcherApps != null &&
                            launcherApps.isPackageEnabled(SEARCH_PACKAGE, myUserHandle());
                case KEY_TRUST_APPS:
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
                    if (launcherApps == null) {
                        return false;
                    }
                    boolean isInstalled = launcherApps.isPackageEnabled(SUGGESTIONS_PACKAGE, myUserHandle())
                            || launcherApps.isPackageEnabled("com.google.android.as.oss", myUserHandle());
                    Log.d("SettingsActivity", "Suggestions package installed: " + isInstalled);
                    if (!isInstalled) {
                        preference.setIntent(null); // Clear intent to handle click manually
                        preference.setOnPreferenceClickListener(p -> {
                            Log.d("SettingsActivity", "Redirecting to Play Store for suggestions");
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
                case KEY_SUGGESTIONS_USAGE_STATS:
                    AppOpsManager appOps = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
                    int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                            Process.myUid(), getContext().getPackageName());
                    if (mode == AppOpsManager.MODE_ALLOWED) {
                        return false; // Hide if already granted
                    }
                    preference.setOnPreferenceClickListener(p -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            intent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
                            startActivity(intent);
                        } catch (Exception e) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                        return true;
                    });
                    return true;
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
