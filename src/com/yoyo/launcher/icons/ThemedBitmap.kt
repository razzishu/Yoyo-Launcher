package com.yoyo.launcher.icons

import android.content.Context
import android.graphics.*
import com.yoyo.launcher.icons.FastBitmapDrawableDelegate.DelegateFactory
import com.yoyo.launcher.icons.FastBitmapDrawableDelegate.Companion.drawShaderInBounds
import com.yoyo.launcher.icons.GraphicsUtils.transformed
import com.yoyo.launcher.Utilities
import com.yoyo.launcher.util.Themes
import com.yoyo.launcher.R

/** Represents a themed version of a BitmapInfo */
interface ThemedBitmap {
    fun newDelegateFactory(info: BitmapInfo, context: Context): DelegateFactory
    fun serialize(): ByteArray

    companion object {
        @JvmField
        val NOT_SUPPORTED = object : ThemedBitmap {
            override fun newDelegateFactory(info: BitmapInfo, context: Context) = FastBitmapDrawableDelegate.SimpleDelegateFactory
            override fun serialize() = ByteArray(0)
        }
    }
}

/**
 * Unique "Vibrant Accent-Pair" Themed Icons.
 */
class BitmapThemedBitmap(val foreground: Bitmap, val isLegacy: Boolean = false) : ThemedBitmap {
    override fun newDelegateFactory(info: BitmapInfo, context: Context): DelegateFactory = object : DelegateFactory {
        override fun newDelegate(
            bitmapInfo: BitmapInfo,
            iconShape: IconShape,
            paint: Paint,
            host: FastBitmapDrawable,
        ): FastBitmapDrawableDelegate {
            return object : FastBitmapDrawableDelegate {
                private val fgShader = BitmapShader(foreground, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                
                override fun drawContent(
                    info: BitmapInfo,
                    iconShape: IconShape,
                    canvas: Canvas,
                    bounds: Rect,
                    paint: Paint,
                ) {
                    // host.callback is typically a View (like BubbleTextView). We must get its context.
                    val viewContext = (host.callback as? android.view.View)?.context
                    val drawContext = viewContext ?: context
                    
                    // Define 3 high-contrast Material You pairs from the system wallpaper palette
                    val backgrounds = intArrayOf(
                        drawContext.getColor(R.color.materialColorPrimaryContainer),
                        drawContext.getColor(R.color.materialColorSecondaryContainer),
                        drawContext.getColor(R.color.materialColorTertiaryContainer)
                    )
                    val logos = intArrayOf(
                        drawContext.getColor(R.color.materialColorOnPrimaryContainer),
                        drawContext.getColor(R.color.materialColorOnSecondaryContainer),
                        drawContext.getColor(R.color.materialColorOnTertiaryContainer)
                    )

                    // Deterministically pick a color based on the original icon's dominant color
                    val index = Math.abs(info.color) % 3
                    val colorBackground = backgrounds[index]
                    val colorLogo = logos[index]

                    // 1. Draw Clean Circular Container
                    paint.color = colorBackground
                    canvas.drawShaderInBounds(bounds, iconShape, paint, null)
                    
                    // 2. Logo Rendering
                    val fw = foreground.width.toFloat()
                    val fh = foreground.height.toFloat()
                    val shaderMatrix = Matrix()
                    shaderMatrix.setScale(100f / fw, 100f / fh)
                    fgShader.setLocalMatrix(shaderMatrix)
                    
                    val w = bounds.width().toFloat()
                    val h = bounds.height().toFloat()
                    
                    canvas.transformed {
                        translate(bounds.left.toFloat(), bounds.top.toFloat())
                        scale(w / 100f, h / 100f)
                        
                        val scale = if (isLegacy) 0.90f else 1.40f 
                        scale(scale, scale, 50f, 50f)
                        
                        paint.shader = fgShader
                        if (isLegacy) {
                            val rL = Color.red(colorLogo).toFloat()
                            val gL = Color.green(colorLogo).toFloat()
                            val bL = Color.blue(colorLogo).toFloat()
                            
                            // Boost the luminance heavily so the custom silhouette is clearly visible
                            val lum = 2.0f
                            val luminanceToAlpha = ColorMatrix(floatArrayOf(
                                0f, 0f, 0f, 0f, rL,
                                0f, 0f, 0f, 0f, gL,
                                0f, 0f, 0f, 0f, bL,
                                0.2126f * lum, 0.7152f * lum, 0.0722f * lum, 0f, 0f
                            ))
                            paint.colorFilter = ColorMatrixColorFilter(luminanceToAlpha)
                        } else {
                            // Adaptive monochrome layers are already perfect alpha silhouettes
                            paint.colorFilter = PorterDuffColorFilter(colorLogo, PorterDuff.Mode.SRC_IN)
                        }
                        
                        canvas.drawPath(iconShape.path, paint)

                        paint.shader = null
                        paint.colorFilter = null
                    }
                }
            }
        }
    }
    
    override fun serialize() = ByteArray(0)
}
