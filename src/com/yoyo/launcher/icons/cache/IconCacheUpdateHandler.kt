package com.yoyo.launcher.icons.cache

import android.os.UserHandle

/**
 * Ported/Stubbed IconCacheUpdateHandler for standalone build.
 */
class IconCacheUpdateHandler(private val iconCache: BaseIconCache) {

    fun interface OnUpdateCallback {
        fun onPackageIconsUpdated(updatedPackages: HashSet<String>, user: UserHandle)
    }

    fun updateIcons(apps: List<*>, cachingLogic: CachingLogic<*>, callback: OnUpdateCallback) {
        // No-op for now
    }
    
    fun addPackagesToIgnore(user: UserHandle, packageName: String) {
        // No-op for now
    }

    fun finish() {
        // No-op
    }
}
