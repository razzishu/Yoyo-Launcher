package com.yoyo.launcher.icons.cache

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.UserHandle
import com.yoyo.launcher.icons.BitmapInfo
import com.yoyo.launcher.icons.IconProvider
import com.yoyo.launcher.icons.SourceHint
import com.yoyo.launcher.util.ComponentKey

interface CachingLogic<T> {
    fun getComponent(item: T): ComponentName
    fun getUser(item: T): UserHandle
    fun getLabel(item: T): CharSequence?
    fun getApplicationInfo(item: T): ApplicationInfo?
    fun loadIcon(context: Context, cache: BaseIconCache, item: T): BitmapInfo
    
    fun getFreshnessIdentifier(item: T, iconProvider: IconProvider): String? {
        return null
    }

    fun getSourceHint(item: T, cache: BaseIconCache): SourceHint {
        return SourceHint(
            key = ComponentKey(getComponent(item), getUser(item)),
            logic = this,
            freshnessId = getFreshnessIdentifier(item, cache.iconProvider)
        )
    }
}
