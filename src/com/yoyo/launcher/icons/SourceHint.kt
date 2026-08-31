package com.yoyo.launcher.icons

import com.yoyo.launcher.icons.cache.CachingLogic
import com.yoyo.launcher.util.ComponentKey

data class SourceHint(
    val key: ComponentKey,
    val logic: CachingLogic<*>,
    val freshnessId: String? = null,
    val isFileDrawable: Boolean = false
)
