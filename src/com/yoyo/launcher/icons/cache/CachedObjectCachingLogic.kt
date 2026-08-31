package com.yoyo.launcher.icons.cache

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.UserHandle
import com.yoyo.launcher.icons.BitmapInfo
import com.yoyo.launcher.icons.BaseIconFactory.IconOptions

object CachedObjectCachingLogic : CachingLogic<CachedObject> {
    override fun getComponent(item: CachedObject): ComponentName = item.component
    override fun getUser(item: CachedObject): UserHandle = item.user
    override fun getLabel(item: CachedObject): CharSequence? = item.label
    override fun getApplicationInfo(item: CachedObject): ApplicationInfo? = item.applicationInfo
    
    override fun loadIcon(context: Context, cache: BaseIconCache, item: CachedObject): BitmapInfo {
        val d = item.getFullResIcon(cache) ?: return BitmapInfo.LOW_RES_INFO
        cache.iconFactory.use { li ->
            return li.createBadgedIconBitmap(
                d,
                IconOptions().setUser(item.user).setSourceHint(getSourceHint(item, cache))
            )
        }
    }
}
