package com.yoyo.launcher.icons

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

interface IconThemeController {
    val themeID: String get() = ""
    fun isThemed(): Boolean = false
    
    fun createThemedBitmap(
        icon: Drawable,
        info: BitmapInfo,
        factory: BaseIconFactory,
        sourceHint: SourceHint? = null,
    ): ThemedBitmap = ThemedBitmap.NOT_SUPPORTED

    fun decode(
        bytes: ByteArray,
        info: BitmapInfo,
        factory: BaseIconFactory,
        sourceHint: SourceHint,
    ): ThemedBitmap = ThemedBitmap.NOT_SUPPORTED

    fun createThemedAdaptiveIcon(
        context: Context,
        originalIcon: Drawable,
        info: BitmapInfo?,
    ): AdaptiveIconDrawable? = if (originalIcon is AdaptiveIconDrawable) originalIcon else null
}
