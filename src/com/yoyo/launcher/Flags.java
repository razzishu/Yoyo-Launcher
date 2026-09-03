package com.yoyo.launcher;

import android.content.Context;

public final class Flags {
    private Flags() {}

    public static boolean allAppsBlur() { return false; }
    public static boolean enableSupportForArchiving() { return true; }
    public static boolean enablePrivateSpace() { return false; }
    public static boolean enableTaskbarPinning() { return false; }
    public static boolean enableLauncherVisualRefresh() { return false; }
    public static boolean forceMonochromeAppIcons() { return false; }
    public static boolean refactorTaskbarUiState() { return false; }
    public static boolean enableOverviewIconMenu() { return false; }
    public static boolean enableCategorizedWidgetSuggestion() { return false; }
    public static boolean enableWidgetPickerSearch() { return true; }
    public static boolean enableSmartspaceEnhanced() { return false; }
    public static boolean enableLauncherIconShapes() { return false; }
    public static boolean enableTwoPaneLauncherSettings() { return false; }
    public static boolean oneGridSpecs() { return false; }
    public static boolean enableWidgetPickerRefactor() { return false; }
    public static boolean homeScreenEditImprovements() { return false; }
    public static boolean msdlFeedback() { return false; }
    public static boolean simplifiedLauncherModelBinding() { return false; }
    public static boolean enableExpressiveFolderExpansion() { return false; }
    public static boolean restoreArchivedShortcuts() { return false; }
    public static boolean restoreArchivedAppIconsFromDb() { return false; }
    public static boolean privateSpaceRestrictItemDrag() { return false; }
    public static boolean newCustomizationPickerUi() { return false; }
    public static boolean enableScalabilityForDesktopExperience() { return false; }
    public static boolean enableRefactorTaskThumbnail() { return false; }
    public static boolean enableGridOnlyOverview() { return false; }
    public static boolean injectableModelItems() { return false; }
    public static boolean useNewIconForArchivedApps() { return false; }
    public static boolean enableLauncherBrMetricsFixed() { return false; }
    public static boolean floatingSearchBar() { return true; }
    public static boolean modelRepository() { return false; }
    public static boolean showFilesOnHomeScreen() { return false; }
    public static boolean enableFallbackOverviewInWindow() { return false; }
    public static boolean enableLauncherOverviewInWindow() { return false; }
    public static boolean enableTieredWidgetsByDefaultInPicker() { return false; }
    public static boolean removeAppsRefreshOnRightClick() { return false; }
    public static boolean enableStrictMode() { return false; }
    public static boolean enableContrastTiles() { return false; }
    public static boolean enableSystemDrag() { return false; }
    public static boolean clearScrimOnReset() { return false; }
    public static boolean enableExpandingPauseWorkButton() { return false; }
    public static boolean useSystemRadiusForAppWidgets() { return false; }
    public static boolean enableFocusOutline() { return false; }
    public static boolean enableOverviewOnConnectedDisplays() { return false; }
    public static boolean enableTwolineToggle() { return false; }
    public static boolean oneGridRotationHandling() { return false; }
    public static boolean allAppsSheetForHandheld() { return false; }
    public static boolean letterFastScroller() { return false; }
    public static boolean privateSpaceRestrictAccessibilityDrag() { return false; }
    public static boolean enableHomeTransitionListener() { return false; }
    public static boolean enableAllAppsButtonInHotseat() { return false; }
    public static boolean enableResponsiveWorkspace() { return false; }
    public static boolean enableQsbOnHotseat(Context context) {
        return LauncherPrefs.get(context).get(LauncherPrefs.HOTSEAT_QSB);
    }
    public static boolean enableQsbOnHotseat() { return true; }
    public static boolean enableRetrievableBubbles() { return false; }
    public static boolean showHomeBehindDesktop() { return false; }
    public static boolean privateSpaceAddFloatingMaskView() { return false; }
    public static boolean privateSpaceAnimation() { return false; }
    public static boolean workSchedulerInWorkProfile() { return false; }
    public static boolean privateSpaceSysAppsSeparation() { return false; }
    public static boolean enableMouseInteractionChanges() { return false; }
}
