package com.yoyo.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.core.graphics.PathParser
import com.yoyo.launcher.LauncherPrefs
import com.yoyo.launcher.graphics.ThemeManager
import com.yoyo.launcher.shapes.ShapesProvider
import com.yoyo.launcher.util.UserIconInfo

/**
 * Ported/Stubbed BaseIconFactory for standalone build.
 */
open class BaseIconFactory(
    @JvmField val context: Context,
    protected val fillResIconDpi: Int,
    protected val iconBitmapSize: Int,
    protected val drawFullBleedIcons: Boolean,
    protected val themeController: Any? = null
) : AutoCloseable {

    private val mShadowGenerator = ShadowGenerator(iconBitmapSize)

    val fullResIconDpi: Int get() = fillResIconDpi

    override fun close() {}

    fun clear() {}

    open fun getUserInfo(user: UserHandle): UserIconInfo {
        return UserIconInfo(user, UserIconInfo.TYPE_MAIN, 0L)
    }

    fun createBadgedIconBitmap(icon: Drawable?): BitmapInfo {
        return createBadgedIconBitmap(icon, IconOptions())
    }

    fun createBadgedIconBitmap(icon: Drawable?, options: IconOptions): BitmapInfo {
        if (icon == null) {
            return BitmapInfo.fromBitmap(Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888))
        }
        
        val bitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val scale = ShadowGenerator.ICON_SCALE_FOR_SHADOWS
        val size = (iconBitmapSize * scale).toInt()
        val offset = (iconBitmapSize - size) / 2
        
        val prefs = LauncherPrefs.get(context)
        val iconPack = prefs.get(LauncherPrefs.ICON_PACK) ?: "default"
        val isDefaultOrThemed = iconPack == "default" || iconPack == "themed"

        val tempBitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)
        val oldBounds = icon.bounds

        if (isDefaultOrThemed) {
            val shapeKey = prefs.get(ThemeManager.PREF_ICON_SHAPE) ?: ""
            val shapeModel = ShapesProvider.iconShapes.firstOrNull { it.key == shapeKey }
            val pathString = shapeModel?.pathString
            if (!pathString.isNullOrEmpty()) {
                val rawPath = PathParser.createPathFromPathData(pathString)
                val matrix = Matrix()
                matrix.setScale(iconBitmapSize / 100f, iconBitmapSize / 100f)
                val scaledPath = Path()
                rawPath.transform(matrix, scaledPath)
                tempCanvas.clipPath(scaledPath)
            }
        }

        if (icon is AdaptiveIconDrawable && isDefaultOrThemed) {
            icon.background?.let { bg ->
                bg.setBounds(0, 0, iconBitmapSize, iconBitmapSize)
                bg.draw(tempCanvas)
            }
            icon.foreground?.let { fg ->
                val fgScale = 1.26f
                val fgSize = (iconBitmapSize * fgScale).toInt()
                val fgOffset = (iconBitmapSize - fgSize) / 2
                fg.setBounds(fgOffset, fgOffset, fgOffset + fgSize, fgOffset + fgSize)
                fg.draw(tempCanvas)
            }
        } else {
            icon.setBounds(offset, offset, offset + size, offset + size)
            icon.draw(tempCanvas)
        }
        
        // Draw shadow from icon content
        mShadowGenerator.drawShadow(tempBitmap, canvas)
        // Draw icon on top
        canvas.drawBitmap(tempBitmap, 0f, 0f, null)
        
        icon.bounds = oldBounds
        tempBitmap.recycle()
        
        val color = options.extractedColor ?: ColorExtractor.findDominantColorByHue(bitmap)
        var flags = 0
        if (icon is AdaptiveIconDrawable) {
            flags = flags or BitmapInfo.FLAG_FULL_BLEED
        }

        // Set NO_SHAPING flag if an icon pack is active
        if (!isDefaultOrThemed) {
            flags = flags or BitmapInfo.FLAG_NO_SHAPING
        }

        var info = BitmapInfo.of(bitmap, color, IconShape.EMPTY).copy(flags = flags)

        if (themeController != null) {
            val tc = themeController as IconThemeController
            val themedBitmap = tc.createThemedBitmap(icon, info, this, options.getSourceHint())
            android.util.Log.d("BaseIconFactory", "createBadgedIconBitmap: themed icons enabled. icon=$icon themedBitmap=$themedBitmap")
            if (themedBitmap !== ThemedBitmap.NOT_SUPPORTED) {
                info = info.copy(themedBitmap = themedBitmap)
            }
        }
        
        return info
    }

    fun createThemedBitmap(icon: Drawable, info: BitmapInfo, isLegacy: Boolean = false): ThemedBitmap {
        val bitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        if (isLegacy) {
            // Scale legacy icons down so they don't fill the entire container
            val scale = 0.85f 
            val size = (iconBitmapSize * scale).toInt()
            val offset = (iconBitmapSize - size) / 2
            icon.setBounds(offset, offset, offset + size, offset + size)
        } else {
            icon.setBounds(0, 0, iconBitmapSize, iconBitmapSize)
        }
        
        icon.draw(canvas)
        return BitmapThemedBitmap(bitmap, isLegacy)
    }
    
    fun createIconBitmap(bitmap: Bitmap, isFullBleed: Boolean): BitmapInfo {
        return BitmapInfo.fromBitmap(bitmap)
    }
    
    fun createIconBitmap(shortcut: android.content.Intent.ShortcutIconResource): BitmapInfo {
        return createBadgedIconBitmap(null)
    }
    
    fun createScaledBitmap(icon: Drawable, mode: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        icon.setBounds(0, 0, iconBitmapSize, iconBitmapSize)
        icon.draw(canvas)
        return bitmap
    }

    class IconOptions {
        private var mSourceHint: SourceHint? = null
        var extractedColor: Int? = null

        fun setUser(user: UserHandle): IconOptions = this
        fun setInstantApp(enabled: Boolean): IconOptions = this
        fun setExtractedColor(color: Int): IconOptions {
            extractedColor = color
            return this
        }
        fun assumeFullBleedIcon(enabled: Boolean): IconOptions = this
        
        fun setSourceHint(hint: SourceHint?): IconOptions {
            mSourceHint = hint
            return this
        }

        fun getSourceHint(): SourceHint? = mSourceHint
    }
    
    companion object {
        const val MODE_DEFAULT = 0
    }
}
