package com.yoyo.launcher.icons

import android.graphics.*
import android.graphics.Shader.TileMode.CLAMP
import android.view.View
import com.yoyo.launcher.LauncherAppState
import com.yoyo.launcher.LauncherPrefs
import com.yoyo.launcher.graphics.ThemeManager
import com.yoyo.launcher.icons.BitmapInfo.Companion.FLAG_FULL_BLEED
import com.yoyo.launcher.icons.GraphicsUtils.resizeToContentSize
import com.yoyo.launcher.icons.GraphicsUtils.transformed

interface FastBitmapDrawableDelegate {

    fun onBoundsChange(bounds: Rect) {}

    fun drawContent(
        info: BitmapInfo,
        iconShape: IconShape,
        canvas: Canvas,
        bounds: Rect,
        paint: Paint,
    )

    fun getIconColor(info: BitmapInfo): Int = info.color

    fun isThemed() = false

    fun setAlpha(alpha: Int) {}

    fun updateFilter(filter: ColorFilter?) {}

    fun onVisibilityChanged(isVisible: Boolean) {}

    fun onLevelChange(level: Int): Boolean = false

    fun interface DelegateFactory {
        fun newDelegate(
            bitmapInfo: BitmapInfo,
            iconShape: IconShape,
            paint: Paint,
            host: FastBitmapDrawable,
        ): FastBitmapDrawableDelegate
    }

    class FullBleedDrawableDelegate(bitmapInfo: BitmapInfo) : FastBitmapDrawableDelegate {
        private val shader = BitmapShader(bitmapInfo.icon, CLAMP, CLAMP).apply {
            val matrix = Matrix()
            val w = bitmapInfo.icon.width.toFloat()
            val h = bitmapInfo.icon.height.toFloat()
            if (w > 0 && h > 0) {
                matrix.setScale(100f / w, 100f / h)
            }
            setLocalMatrix(matrix)
        }

        override fun drawContent(
            info: BitmapInfo,
            iconShape: IconShape,
            canvas: Canvas,
            bounds: Rect,
            paint: Paint,
        ) {
            canvas.drawShaderInBounds(bounds, iconShape, paint, shader)
        }
    }

    /** Delegate for legacy icons that applies global shape clipping with a background plate */
    class ShapedLegacyDrawableDelegate(private val bitmap: Bitmap) : FastBitmapDrawableDelegate {
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        override fun drawContent(
            info: BitmapInfo,
            iconShape: IconShape,
            canvas: Canvas,
            bounds: Rect,
            paint: Paint,
        ) {
            // Only apply shape logic if it's not a simple circle or if explicitly requested
            // In this launcher, we always want the chosen shape
            canvas.transformed {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                
                // 1. Position and scale to 100x100 space
                translate(bounds.left.toFloat(), bounds.top.toFloat())
                scale(w / 100f, h / 100f)
                
                // 2. Draw Background Plate
                canvas.drawPath(iconShape.path, bgPaint)
                
                // 3. Clip to Shape
                canvas.clipPath(iconShape.path)
                
                // 4. Draw Icon scaled down (legacy icons are usually full-frame)
                // Use 0.85f to make them look proportional to adaptive icons
                val iconScale = 0.85f
                scale(iconScale, iconScale, 50f, 50f)
                
                val matrix = Matrix()
                matrix.setScale(100f / bitmap.width, 100f / bitmap.height)
                canvas.drawBitmap(bitmap, matrix, paint)
            }
        }
    }

    object SimpleDrawableDelegate : FastBitmapDrawableDelegate {
        override fun drawContent(
            info: BitmapInfo,
            iconShape: IconShape,
            canvas: Canvas,
            bounds: Rect,
            paint: Paint,
        ) {
            canvas.drawBitmap(info.icon, null, bounds, paint)
        }
    }

    object SimpleDelegateFactory : DelegateFactory {
        override fun newDelegate(
            bitmapInfo: BitmapInfo,
            iconShape: IconShape,
            paint: Paint,
            host: FastBitmapDrawable,
        ): FastBitmapDrawableDelegate {
            if ((bitmapInfo.flags and FLAG_FULL_BLEED) != 0) {
                return FullBleedDrawableDelegate(bitmapInfo)
            }

            if ((bitmapInfo.flags and BitmapInfo.FLAG_NO_SHAPING) != 0) {
                return SimpleDrawableDelegate
            }

            // For non-adaptive icons, use the shaped legacy delegate to ensure they match the system shape
            return ShapedLegacyDrawableDelegate(bitmapInfo.icon)
        }
    }

    companion object {
        fun Canvas.drawShaderInBounds(
            bounds: Rect,
            iconShape: IconShape,
            paint: Paint,
            shader: Shader?,
        ) {
            transformed {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                
                // 1. Draw shadow, scaled to bounds
                val shadowMatrix = Matrix()
                shadowMatrix.setScale(w / 100f, h / 100f)
                shadowMatrix.postTranslate(bounds.left.toFloat(), bounds.top.toFloat())
                
                val shadowPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                drawBitmap(iconShape.shadowLayer, shadowMatrix, shadowPaint)

                // 2. Draw path with shader
                translate(bounds.left.toFloat(), bounds.top.toFloat())
                scale(w / 100f, h / 100f)
                
                // Normalize area
                val normalization = IconNormalizer.ICON_VISIBLE_AREA_FACTOR
                scale(normalization, normalization, 50f, 50f)
                
                paint.shader = shader
                drawPath(iconShape.path, paint)
                paint.shader = null
            }
        }
    }
}
