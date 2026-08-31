package com.yoyo.launcher.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.util.Log
import com.yoyo.launcher.BuildConfig
import com.yoyo.launcher.icons.BaseIconFactory.IconOptions
import com.yoyo.launcher.icons.cache.BaseIconCache
import com.yoyo.launcher.icons.cache.CachingLogic
import com.yoyo.launcher.shortcuts.ShortcutKey
import com.yoyo.launcher.util.ApiWrapper
import com.yoyo.launcher.util.ApplicationInfoWrapper
import com.yoyo.launcher.util.PackageUserKey
import com.yoyo.launcher.util.Themes

/** Wrapper over ShortcutInfo to provide extra information related to ShortcutInfo */
class CacheableShortcutInfo
@JvmOverloads
constructor(
    val shortcutInfo: ShortcutInfo,
    val appInfo: ApplicationInfoWrapper,
    val fallbackIconProvider: (BaseIconFactory) -> BitmapInfo? = { null },
) {

    @JvmOverloads
    constructor(
        info: ShortcutInfo,
        ctx: Context,
        fallbackIconProvider: (BaseIconFactory) -> BitmapInfo? = { null },
    ) : this(
        info,
        ApplicationInfoWrapper(ctx, info.getPackage(), info.userHandle),
        fallbackIconProvider,
    )

    companion object {
        private const val TAG = "CacheableShortcutInfo"

        /**
         * Similar to [LauncherApps.getShortcutIconDrawable] with additional Launcher specific
         * checks
         */
        @JvmStatic
        fun getIcon(context: Context, shortcutInfo: ShortcutInfo, density: Int): Drawable? {
            if (!BuildConfig.WIDGETS_ENABLED) {
                return null
            }
            try {
                return context
                    .getSystemService(LauncherApps::class.java)
                    .getShortcutIconDrawable(shortcutInfo, density)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get shortcut icon", e)
                return null
            }
        }

        /**
         * Converts the provided list of Shortcuts to CacheableShortcuts by using the application
         * info from the provided list of apps
         */
        @JvmStatic
        fun convertShortcutsToCacheableShortcuts(
            shortcuts: List<ShortcutInfo>,
            activities: List<LauncherActivityInfo>,
        ): List<CacheableShortcutInfo> {
            // Create a map of package to applicationInfo
            val appMap =
                activities.associateBy(
                    { PackageUserKey(it.componentName.packageName, it.user) },
                    { it.applicationInfo },
                )

            return shortcuts.map {
                CacheableShortcutInfo(
                    it,
                    ApplicationInfoWrapper(appMap[PackageUserKey(it.getPackage(), it.userHandle)]),
                )
            }
        }
    }
}

/** Caching logic for CacheableShortcutInfo. */
object CacheableShortcutCachingLogic : CachingLogic<CacheableShortcutInfo> {

    override fun getComponent(item: CacheableShortcutInfo): ComponentName =
        ShortcutKey.fromInfo(item.shortcutInfo).componentName

    override fun getUser(item: CacheableShortcutInfo): UserHandle = item.shortcutInfo.userHandle

    override fun getLabel(item: CacheableShortcutInfo): CharSequence? = item.shortcutInfo.shortLabel

    override fun getApplicationInfo(item: CacheableShortcutInfo) = item.appInfo.getInfo()

    override fun loadIcon(context: Context, cache: BaseIconCache, item: CacheableShortcutInfo) =
        cache.iconFactory.use { li ->
            CacheableShortcutInfo.getIcon(
                    context,
                    item.shortcutInfo,
                    li.fullResIconDpi,
                )
                ?.let { d ->
                    li.createBadgedIconBitmap(
                        d,
                        IconOptions()
                            .setExtractedColor(Themes.getColorAccent(context))
                            .setSourceHint(
                                getSourceHint(item, cache)
                                    .copy(
                                        isFileDrawable =
                                            ApiWrapper.INSTANCE[context].isFileDrawable(
                                                item.shortcutInfo
                                            )
                                    )
                            ),
                    )
                } ?: item.fallbackIconProvider.invoke(li) ?: BitmapInfo.LOW_RES_INFO
        }

    override fun getFreshnessIdentifier(
        item: CacheableShortcutInfo,
        provider: IconProvider,
    ): String? =
        // Manifest shortcuts get updated on every reboot. Don't include their change timestamp as
        // it gets covered by the app's version
        (if (item.shortcutInfo.isDeclaredInManifest) ""
        else item.shortcutInfo.lastChangedTimestamp.toString()) +
            "-" +
            provider.getStateForApp(getApplicationInfo(item))
}
