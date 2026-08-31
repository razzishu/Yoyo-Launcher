package com.yoyo.launcher.icons.cache

/**
 * Data class representing various options for icon cache lookup.
 */
@JvmRecord
data class CacheLookupFlag(private val flags: Int) {

    fun useLowRes(): Boolean = (flags and USE_LOW_RES) != 0
    fun usePackageIcon(): Boolean = (flags and USE_PACKAGE_ICON) != 0
    fun skipAddToMemCache(): Boolean = (flags and SKIP_ADD_TO_MEM_CACHE) != 0
    fun hasThemeIcon(): Boolean = (flags and LOAD_THEME_ICON) != 0

    @JvmOverloads
    fun withUseLowRes(enabled: Boolean = true): CacheLookupFlag = if (enabled) add(USE_LOW_RES) else remove(USE_LOW_RES)
    @JvmOverloads
    fun withUsePackageIcon(enabled: Boolean = true): CacheLookupFlag = if (enabled) add(USE_PACKAGE_ICON) else remove(USE_PACKAGE_ICON)
    fun withSkipAddToMemCache(): CacheLookupFlag = add(SKIP_ADD_TO_MEM_CACHE)
    @JvmOverloads
    fun withThemeIcon(enabled: Boolean = true): CacheLookupFlag = if (enabled) add(LOAD_THEME_ICON) else remove(LOAD_THEME_ICON)

    private fun add(flag: Int): CacheLookupFlag = CacheLookupFlag(flags or flag)
    private fun remove(flag: Int): CacheLookupFlag = CacheLookupFlag(flags and flag.inv())

    fun isVisuallyLessThan(other: CacheLookupFlag): Boolean {
        if (!useLowRes() && other.useLowRes()) return false
        if (useLowRes() && !other.useLowRes()) return true
        if (!hasThemeIcon() && other.hasThemeIcon()) return true
        return false
    }

    companion object {
        private const val USE_LOW_RES = 1 shl 0
        private const val USE_PACKAGE_ICON = 1 shl 1
        private const val SKIP_ADD_TO_MEM_CACHE = 1 shl 2
        private const val LOAD_THEME_ICON = 1 shl 3

        @JvmField
        val DEFAULT_LOOKUP_FLAG = CacheLookupFlag(0)
    }
}
