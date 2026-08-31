package com.yoyo.launcher.icons.cache

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import com.yoyo.launcher.icons.BaseIconFactory.IconOptions
import com.yoyo.launcher.icons.BitmapInfo
import com.yoyo.launcher.icons.IconProvider
import com.yoyo.launcher.icons.cache.BaseIconCache.Companion.EMPTY_CLASS_NAME

/** Caching logic for ApplicationInfo */
class AppInfoCachingLogic(
    private val pm: PackageManager,
    private val instantAppResolver: (ApplicationInfo) -> Boolean,
    private val errorLogger: (String, Exception?) -> Unit = { _, _ -> },
) : CachingLogic<ApplicationInfo> {

    override fun getComponent(info: ApplicationInfo) =
        ComponentName(info.packageName, info.packageName + EMPTY_CLASS_NAME)

    override fun getUser(info: ApplicationInfo) = UserHandle.getUserHandleForUid(info.uid)

    override fun getLabel(info: ApplicationInfo) = info.loadLabel(pm)

    override fun getApplicationInfo(info: ApplicationInfo) = info

    override fun loadIcon(
        context: Context,
        cache: BaseIconCache,
        info: ApplicationInfo,
    ): BitmapInfo {
        val appIcon = cache.iconProvider.getIcon(info)
        return cache.iconFactory.use { li ->
            li.createBadgedIconBitmap(
                appIcon,
                IconOptions()
                    .setUser(getUser(info))
                    .setInstantApp(instantAppResolver.invoke(info))
                    .setSourceHint(getSourceHint(info, cache)),
            )
        }
    }

    override fun getFreshnessIdentifier(item: ApplicationInfo, iconProvider: IconProvider) =
        iconProvider.getStateForApp(item)
}
