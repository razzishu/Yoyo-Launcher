package com.yoyo.launcher.icons

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import com.yoyo.launcher.icons.FastBitmapDrawableDelegate.DelegateFactory

class PlaceHolderDrawableDelegate(info: BitmapInfo, context: Context) : FastBitmapDrawableDelegate {
    private val fillColor = ColorUtils.compositeColors(
        GraphicsUtils.setColorAlphaBound(Color.WHITE, 100),
        info.color
    )

    override fun drawContent(info: BitmapInfo, iconShape: IconShape, canvas: Canvas, bounds: Rect, paint: Paint) {
        paint.color = fillColor
        canvas.drawPath(iconShape.path, paint)
    }
    
    fun animateIconUpdate(icon: Drawable) {}
    
    class PlaceHolderDelegateFactory(private val context: Context) : DelegateFactory {
        override fun newDelegate(bitmapInfo: BitmapInfo, iconShape: IconShape, paint: Paint, host: FastBitmapDrawable): FastBitmapDrawableDelegate {
            return PlaceHolderDrawableDelegate(bitmapInfo, context)
        }
    }
}
