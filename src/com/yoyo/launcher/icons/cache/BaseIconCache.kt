package com.yoyo.launcher.icons.cache

import android.content.ComponentName
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.text.TextUtils
import com.yoyo.launcher.icons.BaseIconFactory
import com.yoyo.launcher.icons.BitmapInfo
import com.yoyo.launcher.icons.IconProvider
import com.yoyo.launcher.util.ComponentKey
import com.yoyo.launcher.util.FlagOp
import com.yoyo.launcher.util.SQLiteCacheHelper
import java.util.function.Supplier

/**
 * Ported/Stubbed BaseIconCache for standalone build.
 */
abstract class BaseIconCache(
    @JvmField public val context: Context,
    protected val dbFileName: String?,
    protected val bgLooper: Looper,
    @JvmField protected val fillResIconDpi: Int,
    protected val iconBitmapSize: Int,
    protected val inMemoryCache: Boolean,
    @JvmField val iconProvider: IconProvider
) {
    @JvmField val workerHandler = Handler(bgLooper)
    @JvmField val iconDb: SQLiteCacheHelper = SQLiteCacheHelper(context, "", 0, "")
    protected val cache: MutableMap<ComponentKey, CacheEntry?> = HashMap(50)

    class CacheEntry {
        @JvmField var bitmap: BitmapInfo = BitmapInfo.LOW_RES_INFO
        @JvmField var title: CharSequence = ""
        @JvmField var contentDescription: CharSequence = ""
    }

    abstract fun getSerialNumberForUser(user: UserHandle): Long
    abstract val iconFactory: BaseIconFactory
    protected abstract fun isInstantApp(info: android.content.pm.ApplicationInfo): Boolean

    open fun getUpdateHandler(): IconCacheUpdateHandler {
        return IconCacheUpdateHandler(this)
    }

    open fun getUserBadgedLabel(label: CharSequence, user: UserHandle): CharSequence {
        return label
    }
    
    open fun removeIconsForPkg(packageName: String, user: UserHandle) {
        synchronized(this) {
            cache.entries.removeIf { it.key.componentName.packageName == packageName && it.key.user == user }
        }
    }

    open fun remove(componentName: ComponentName, user: UserHandle) {
        synchronized(this) {
            cache.remove(ComponentKey(componentName, user))
        }
    }

    open fun updateIconParams(fillResIconDpi: Int, iconBitmapSize: Int) {
        synchronized(this) {
            cache.clear()
            iconProvider.updateSystemState()
        }
    }

    open fun getDefaultIcon(user: UserHandle): BitmapInfo {
        return iconFactory.use { it.createBadgedIconBitmap(null) }
    }

    open fun isDefaultIcon(info: BitmapInfo, user: UserHandle): Boolean = info == BitmapInfo.LOW_RES_INFO
    
    protected open fun addIconToDBAndMemCache(item: Any, cachingLogic: CachingLogic<*>, userSerial: Long) {}

    @JvmOverloads
    @Synchronized
    open fun <T : Any> cacheLocked(
        componentName: ComponentName,
        user: UserHandle,
        infoProvider: Supplier<T?>,
        cachingLogic: CachingLogic<T>,
        lookupFlags: CacheLookupFlag,
        cursor: Cursor? = null
    ): CacheEntry {
        val cacheKey = ComponentKey(componentName, user)
        var entry = cache[cacheKey]
        if (entry == null || entry.bitmap.matchingLookupFlag.isVisuallyLessThan(lookupFlags)) {
            android.util.Log.d("BaseIconCache", "cacheLocked: MISS for $componentName flags=$lookupFlags")
            entry = CacheEntry()
            cache[cacheKey] = entry
// ...

            val obj: T? by lazy { infoProvider.get() }
            loadFallbackIcon(
                obj,
                entry,
                cachingLogic,
                lookupFlags,
                /* usePackageTitle= */ true,
                componentName,
                user,
            )
            if (TextUtils.isEmpty(entry.title)) {
                obj?.let { loadFallbackTitle(it, entry, cachingLogic, user) }
            }
        }
        return entry
    }

    @Synchronized
    open fun getEntryForPackageLocked(packageName: String, user: UserHandle, lookupFlag: CacheLookupFlag): CacheEntry {
        val cacheKey = ComponentKey(ComponentName(packageName, EMPTY_CLASS_NAME), user)
        var entry = cache[cacheKey]
        if (entry == null || entry.bitmap.matchingLookupFlag.isVisuallyLessThan(lookupFlag)) {
            entry = CacheEntry()
            cache[cacheKey] = entry
            
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val iconDrawable = iconProvider.getIcon(appInfo)
                entry.bitmap = iconFactory.use { it.createBadgedIconBitmap(iconDrawable) }
                entry.title = appInfo.loadLabel(context.packageManager)
            } catch (e: Exception) {
                entry.bitmap = getDefaultIcon(user)
                entry.title = packageName
            }
            entry.contentDescription = getUserBadgedLabel(entry.title, user)
        }
        return entry
    }
    
    open fun getFullResIcon(info: android.content.pm.ActivityInfo): android.graphics.drawable.Drawable? = null
    open fun getFullResIcon(info: android.content.pm.LauncherActivityInfo): android.graphics.drawable.Drawable? = null
    open fun getUserFlagOpLocked(user: UserHandle): FlagOp = FlagOp.NO_OP
    open fun getInMemoryPackageEntryLocked(packageName: String, user: UserHandle): CacheEntry? = cache[ComponentKey(ComponentName(packageName, EMPTY_CLASS_NAME), user)]
    open fun cachePackageInstallInfo(packageName: String, user: UserHandle, icon: Bitmap?, label: CharSequence?) {}
    open fun getEntryFromDBLocked(cacheKey: ComponentKey, entry: CacheEntry, lookupFlag: CacheLookupFlag, cachingLogic: CachingLogic<*>): Boolean = false
    
    open fun <T : Any> loadFallbackIcon(
        obj: T?,
        entry: CacheEntry,
        cachingLogic: CachingLogic<T>,
        lookupFlag: CacheLookupFlag,
        usePackageTitle: Boolean,
        componentName: ComponentName,
        user: UserHandle,
    ) {
        if (obj != null) {
            entry.bitmap = cachingLogic.loadIcon(context, this, obj)
        } else {
            if (lookupFlag.usePackageIcon()) {
                val packageEntry = getEntryForPackageLocked(componentName.packageName, user, lookupFlag)
                entry.bitmap = packageEntry.bitmap
                entry.contentDescription = packageEntry.contentDescription
                if (usePackageTitle) {
                    entry.title = packageEntry.title
                }
            }
        }
    }

    open fun <T : Any> loadFallbackTitle(
        obj: T,
        entry: CacheEntry,
        cachingLogic: CachingLogic<T>,
        user: UserHandle,
    ) {
        entry.title = cachingLogic.getLabel(obj).let {
            if (it.isNullOrEmpty()) cachingLogic.getComponent(obj).packageName else it
        }
        entry.contentDescription = getUserBadgedLabel(entry.title, user)
    }

    protected open fun logPersistently(message: String, e: Exception?) {}

    companion object {
        const val TAG = "BaseIconCache"
        const val EMPTY_CLASS_NAME = ".EMPTY_CLASS_NAME"
        @JvmField val COLUMN_COMPONENT = "componentName"
        @JvmField val COLUMN_USER = "profileId"
    }
    
    open fun toLookupColumns(lookupFlag: CacheLookupFlag): Array<String> = arrayOf(COLUMN_COMPONENT, COLUMN_USER)
}
