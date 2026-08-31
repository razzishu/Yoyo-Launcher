package com.yoyo.launcher.icons.mono

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.yoyo.launcher.icons.*
import com.yoyo.launcher.icons.ThemedBitmap.Companion.NOT_SUPPORTED
import com.yoyo.launcher.util.Themes

class MonoIconThemeController(val shouldForceThemeIcon: Boolean = true) : IconThemeController {
    override val themeID: String = "mono"
    override fun isThemed(): Boolean = true

    // No longer using grayscale filter, tinting is handled in ThemedBitmap using SRC_IN

    override fun createThemedBitmap(
        icon: Drawable,
        info: BitmapInfo,
        factory: BaseIconFactory,
        sourceHint: SourceHint?,
    ): ThemedBitmap {
        val pkgName = sourceHint?.key?.componentName?.packageName ?: ""
        
        // 1. Check for Custom Logo Overrides (Nekogram, Rapido)
        val customPath = CustomLogoManager.getCustomPath(pkgName)
        if (customPath != null) {
            val size = 100 
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.BLACK
            canvas.drawPath(customPath, paint)
            return factory.createThemedBitmap(BitmapDrawable(bitmap), info, isLegacy = false)
        }
        
        if (icon is AdaptiveIconDrawable) {
            // For Drive and Play Games, use solid silhouette of updated logos
            if (pkgName == "com.google.android.apps.docs" || pkgName == "com.google.android.play.games") {
                return factory.createThemedBitmap(icon.foreground, info, isLegacy = false)
            }
            
            val mono = if (android.os.Build.VERSION.SDK_INT >= 33) {
                icon.monochrome
            } else {
                val fg = icon.foreground
                if (fg is MonoChromeIconDrawable) fg else null
            }

            if (mono != null && mono.intrinsicWidth > 0) {
                return factory.createThemedBitmap(mono, info)
            }

            if (shouldForceThemeIcon) {
                // Adaptive icons usually handle foregrounds cleanly, but we can pass it as legacy
                // to get a clean silhouette if it doesn't have a monochrome layer.
                return factory.createThemedBitmap(icon.foreground, info, isLegacy = true)
            }
        } else if (shouldForceThemeIcon) {
            // For non-adaptive legacy icons, pass the raw icon with isLegacy = true. 
            // ThemedBitmap will extract its alpha channel to create a perfect monochrome silhouette.
            return factory.createThemedBitmap(icon, info, isLegacy = true)
        }

        return NOT_SUPPORTED
    }

    override fun createThemedAdaptiveIcon(
        context: Context,
        originalIcon: Drawable,
        info: BitmapInfo?,
    ): AdaptiveIconDrawable? {
        val colors = intArrayOf(
            Themes.getAttrColor(context, android.R.attr.colorAccent),
            Themes.getAttrColor(context, android.R.attr.colorBackground)
        )
        
        if (originalIcon is AdaptiveIconDrawable) {
            val mono = if (android.os.Build.VERSION.SDK_INT >= 33) {
                originalIcon.monochrome
            } else {
                val fg = originalIcon.foreground
                if (fg is MonoChromeIconDrawable) fg else null
            }

            if (mono != null && mono.intrinsicWidth > 0) {
                mono.mutate().setColorFilter(PorterDuffColorFilter(colors[0], PorterDuff.Mode.SRC_IN))
                return AdaptiveIconDrawable(ColorDrawable(colors[1]), mono)
            }

            if (shouldForceThemeIcon) {
                val fg = originalIcon.foreground.mutate()
                fg.setColorFilter(PorterDuffColorFilter(colors[0], PorterDuff.Mode.SRC_IN))
                return AdaptiveIconDrawable(ColorDrawable(colors[1]), fg)
            }
        } else if (shouldForceThemeIcon) {
            val tintedIcon = originalIcon.mutate()
            tintedIcon.setColorFilter(PorterDuffColorFilter(colors[0], PorterDuff.Mode.SRC_IN))
            return AdaptiveIconDrawable(ColorDrawable(colors[1]), tintedIcon)
        }

        return if (originalIcon is AdaptiveIconDrawable) originalIcon else null
    }
}

// Simple BitmapDrawable wrapper for createThemedBitmap
private class BitmapDrawable(val bitmap: Bitmap) : android.graphics.drawable.Drawable() {
    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, bounds, null)
    }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth(): Int = bitmap.width
    override fun getIntrinsicHeight(): Int = bitmap.height
}

// Wrapper to visually shift misaligned icons
private class ShiftedDrawable(val base: Drawable, val shiftX: Float, val shiftY: Float) : Drawable() {
    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(bounds.width() * shiftX, bounds.height() * shiftY)
        base.draw(canvas)
        canvas.restore()
    }
    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        base.setBounds(left, top, right, bottom)
    }
    override fun setAlpha(alpha: Int) { base.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { base.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = base.opacity
    override fun getIntrinsicWidth(): Int = base.intrinsicWidth
    override fun getIntrinsicHeight(): Int = base.intrinsicHeight
}
