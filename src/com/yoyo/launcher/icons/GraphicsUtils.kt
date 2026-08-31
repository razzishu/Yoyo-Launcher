package com.yoyo.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Rect
import android.util.TypedValue
import androidx.core.graphics.ColorUtils

object GraphicsUtils {
    @JvmField
    var sOnNewBitmapRunnable: Runnable? = null

    fun getFloat(context: Context, attr: Int, defaultValue: Float): Float {
        val outValue = TypedValue()
        if (context.theme.resolveAttribute(attr, outValue, true)) {
            return outValue.float
        }
        return defaultValue
    }
    
    @JvmStatic
    fun getAttrColor(context: Context, attr: Int): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr, outValue, true)
        return outValue.data
    }

    fun noteNewBitmapCreated() {}

    inline fun Canvas.transformed(block: Canvas.() -> Unit) {
        val count = save()
        try {
            block()
        } finally {
            restoreToCount(count)
        }
    }

    fun Canvas.resizeToContentSize(bounds: Rect) {
        val scale = IconNormalizer.ICON_VISIBLE_AREA_FACTOR
        scale(scale, scale, bounds.exactCenterX(), bounds.exactCenterY())
    }

    fun Canvas.resizeToContentSize(bounds: Rect, scaleFactor: Float, block: () -> Unit) {
        transformed {
            val scale = IconNormalizer.ICON_VISIBLE_AREA_FACTOR * scaleFactor
            scale(scale, scale, bounds.exactCenterX(), bounds.exactCenterY())
            block()
        }
    }
    
    @JvmStatic
    fun flattenBitmap(bitmap: Bitmap): ByteArray {
        return ByteArray(0) // Stub
    }

    @JvmStatic
    fun createDefaultFlatBitmap(info: BitmapInfo): ByteArray {
        return flattenBitmap(info.icon)
    }

    @JvmStatic
    fun generateIconShape(path: Path): IconShape {
        return generateIconShape(IconShape.DEFAULT_PATH_SIZE, path)
    }

    @JvmStatic
    fun generateIconShape(size: Int): IconShape {
        val path = Path()
        path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        return generateIconShape(size, path)
    }
    
    @JvmStatic
    fun generateIconShape(size: Int, path: Path): IconShape {
        val shadow = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(shadow)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        canvas.drawPath(path, paint)
        
        val shadowGen = ShadowGenerator(size)
        val finalShadow = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
        shadowGen.drawShadow(shadow, Canvas(finalShadow))
        
        return IconShape(size, path, finalShadow)
    }

    @JvmStatic
    fun setColorAlphaBound(color: Int, alpha: Int): Int {
        return ColorUtils.setAlphaComponent(color, alpha)
    }
}
