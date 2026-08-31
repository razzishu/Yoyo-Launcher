package com.yoyo.launcher.icons.cache

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import android.util.Log
import com.yoyo.launcher.icons.BitmapInfo
import com.yoyo.launcher.icons.BaseIconFactory.IconOptions

object LauncherActivityCachingLogic : CachingLogic<LauncherActivityInfo> {
    private const val TAG = "LauncherActivityCachingLogic"

    override fun getComponent(item: LauncherActivityInfo): ComponentName = item.componentName
    override fun getUser(item: LauncherActivityInfo): UserHandle = item.user
    override fun getLabel(item: LauncherActivityInfo): CharSequence = item.label
    override fun getApplicationInfo(item: LauncherActivityInfo): ApplicationInfo = item.applicationInfo
    
    override fun loadIcon(context: Context, cache: BaseIconCache, info: LauncherActivityInfo): BitmapInfo {
        cache.iconFactory.use { li ->
            val iconOptions = IconOptions()
                .setUser(info.user)
                .setSourceHint(getSourceHint(info, cache))
            
            val iconDrawable = cache.iconProvider.getIcon(info, li.fullResIconDpi)
            
            if (context.packageManager.isDefaultApplicationIcon(iconDrawable)) {
                Log.w(TAG, "loadIcon: Default app icon returned from PackageManager. component=${info.componentName}, user=${info.user}")
                return cache.getDefaultIcon(info.user)
            }
            
            return li.createBadgedIconBitmap(iconDrawable, iconOptions)
        }
    }
}
