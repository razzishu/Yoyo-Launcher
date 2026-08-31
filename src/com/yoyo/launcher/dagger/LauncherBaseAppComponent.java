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

package com.yoyo.launcher.dagger;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.yoyo.launcher.InvariantDeviceProfile;
import com.yoyo.launcher.LauncherAppState;
import com.yoyo.launcher.LauncherPrefs;
import com.yoyo.launcher.MainProcessInitializer;
import com.yoyo.launcher.RemoveAnimationSettingsTracker;
import com.yoyo.launcher.backuprestore.LauncherRestoreEventLogger;
import com.yoyo.launcher.compose.core.widgetpicker.WidgetPickerComposeWrapper;
import com.yoyo.launcher.dragndrop.SystemDragController;
import com.yoyo.launcher.folder.FolderNameSuggestionLoader;
import com.yoyo.launcher.graphics.GridCustomizationsProxy;
import com.yoyo.launcher.graphics.ThemeManager;
import com.yoyo.launcher.graphics.theme.ThemePreference;
import com.yoyo.launcher.homescreenfiles.HomeScreenFilesProvider;
import com.yoyo.launcher.icons.IconChangeTracker;
import com.yoyo.launcher.icons.LauncherIcons.IconPool;
import com.yoyo.launcher.logging.DumpManager;
import com.yoyo.launcher.logging.StatsLogManager;
import com.yoyo.launcher.model.GridSizeMigrationLogic;
import com.yoyo.launcher.model.ItemInstallQueue;
import com.yoyo.launcher.model.LayoutParserFactory;
import com.yoyo.launcher.model.LoaderCursor.LoaderCursorFactory;
import com.yoyo.launcher.model.TestableModelState;
import com.yoyo.launcher.notification.NotificationRepository;
import com.yoyo.launcher.pm.InstallSessionHelper;
import com.yoyo.launcher.pm.UserCache;
import com.yoyo.launcher.popup.PopupDataRepository;
import com.yoyo.launcher.qsb.OSEManager;
import com.yoyo.launcher.qsb.OseWidgetManager;
import com.yoyo.launcher.qsb.QsbAppWidgetHost;
import com.yoyo.launcher.testing.TestInformationHandler;
import com.yoyo.launcher.util.ApiWrapper;
import com.yoyo.launcher.util.DaggerSingletonTracker;
import com.yoyo.launcher.util.DisplayController;
import com.yoyo.launcher.util.DynamicResource;
import com.yoyo.launcher.util.InstantAppResolver;
import com.yoyo.launcher.util.LayoutImportExportHelper;
import com.yoyo.launcher.util.LockedUserState;
import com.yoyo.launcher.util.MSDLPlayerWrapper;
import com.yoyo.launcher.util.PackageManagerHelper;
import com.yoyo.launcher.util.PluginManagerWrapper;
import com.yoyo.launcher.util.ScreenOnTracker;
import com.yoyo.launcher.util.SettingsCache;
import com.yoyo.launcher.util.TaskbarModeUtil;
import com.yoyo.launcher.util.VibratorWrapper;
import com.yoyo.launcher.util.WallpaperColorHints;
import com.yoyo.launcher.util.window.RefreshRateTracker;
import com.yoyo.launcher.util.window.WindowManagerProxy;
import com.yoyo.launcher.widget.LauncherWidgetHolder.WidgetHolderFactory;
import com.yoyo.launcher.widget.custom.CustomWidgetManager;
import com.yoyo.launcher.widget.util.WidgetSizeHandler;

import dagger.BindsInstance;

import javax.inject.Named;

/**
 * Launcher base component for Dagger injection.
 *
 * This class is not actually annotated as a Dagger component, since it is not used directly as one.
 * Doing so generates unnecessary code bloat.
 *
 * See {@link LauncherAppComponent} for the one actually used by AOSP.
 */
public interface LauncherBaseAppComponent {
    DaggerSingletonTracker getDaggerSingletonTracker();
    ApiWrapper getApiWrapper();
    CustomWidgetManager getCustomWidgetManager();
    DynamicResource getDynamicResource();
    InstallSessionHelper getInstallSessionHelper();
    ItemInstallQueue getItemInstallQueue();
    RefreshRateTracker getRefreshRateTracker();
    ScreenOnTracker getScreenOnTracker();
    SettingsCache getSettingsCache();
    PackageManagerHelper getPackageManagerHelper();
    PluginManagerWrapper getPluginManagerWrapper();
    VibratorWrapper getVibratorWrapper();
    MSDLPlayerWrapper getMSDLPlayerWrapper();
    WindowManagerProxy getWmProxy();
    LauncherPrefs getLauncherPrefs();
    ThemeManager getThemeManager();
    UserCache getUserCache();
    DisplayController getDisplayController();
    WallpaperColorHints getWallpaperColorHints();
    LockedUserState getLockedUserState();
    InvariantDeviceProfile getIDP();
    IconPool getIconPool();
    RemoveAnimationSettingsTracker getRemoveAnimationSettingsTracker();
    LauncherAppState getLauncherAppState();
    LauncherRestoreEventLogger getLauncherRestoreEventLogger();
    GridCustomizationsProxy getGridCustomizationsProxy();
    FolderNameSuggestionLoader getFolderNameSuggestionLoader();
    LoaderCursorFactory getLoaderCursorFactory();
    WidgetHolderFactory getWidgetHolderFactory();
    RefreshRateTracker getFrameRateProvider();
    InstantAppResolver getInstantAppResolver();
    DumpManager getDumpManager();
    StatsLogManager.StatsLogManagerFactory getStatsLogManagerFactory();
    ActivityContextComponent.Builder getActivityContextComponentBuilder();
    WidgetPickerComposeWrapper getWidgetPickerComposeWrapper();
    WidgetSizeHandler getWidgetSizeHandler();
    MainProcessInitializer getMainProcessInitializer();
    OseWidgetManager getOseWidgetManager();
    OSEManager getOseManager();
    QsbAppWidgetHost getQsbAppWidgetHost();
    TestInformationHandler getTestInformationHandler();
    TaskbarModeUtil getTaskbarModeUtil();
    SystemDragController getSystemDragController();

    /** Utility class for importing/exporting launcher layout */
    LayoutImportExportHelper getLayoutImportExportHelper();
    /** Returns the layout parser factory for default layout parsing */
    LayoutParserFactory getLayoutParserFactory();

    @VisibleForTesting
    GridSizeMigrationLogic createNewGridSizeMigrationLogic();
    /** Returns reference to various model objects used for test verification */
    TestableModelState getTestableModelState();

    PopupDataRepository getPopupDataRepository();
    NotificationRepository getNotificationRepository();
    HomeScreenFilesProvider getHomeScreenFilesProvider();

    /** Preferences for icon theme */
    ThemePreference getThemePreference();

    /** Tracker for any app icon changes */
    IconChangeTracker getIconChangeTracker();

    /** Builder for LauncherBaseAppComponent. */
    interface Builder {
        @BindsInstance Builder appContext(@ApplicationContext Context context);
        @BindsInstance Builder iconsDbName(@Nullable @Named("ICONS_DB") String dbFileName);
        @BindsInstance Builder setSafeModeEnabled(@Named("SAFE_MODE") boolean safeModeEnabled);
        LauncherBaseAppComponent build();
    }
}
