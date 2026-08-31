package com.yoyo.launcher.icons

import android.graphics.*
import android.graphics.drawable.Drawable
import com.yoyo.launcher.icons.BitmapInfo.Companion.FLAG_THEMED
import com.yoyo.launcher.icons.FastBitmapDrawableDelegate.DelegateFactory

/**
 * Ported/Stubbed FastBitmapDrawable for standalone build.
 */
@SuppressWarnings("unused")
open class FastBitmapDrawable
@JvmOverloads
constructor(
    @JvmField val bitmapInfo: BitmapInfo,
    @JvmField val iconShape: IconShape = bitmapInfo.defaultIconShape,
    @JvmField val delegateFactory: DelegateFactory = FastBitmapDrawableDelegate.SimpleDelegateFactory,
    @JvmField val badge: Drawable? = null,
    @JvmField val disabledAlpha: Float = 1f
) : Drawable() {

    protected val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    @JvmField
    var isDisabled: Boolean = false
    
    @JvmField
    var creationFlags: Int = 0
    
    @JvmField
    val delegate: FastBitmapDrawableDelegate = delegateFactory.newDelegate(bitmapInfo, iconShape, paint, this)

    @JvmOverloads
    constructor(bitmap: Bitmap, color: Int = Color.TRANSPARENT) : this(BitmapInfo.of(bitmap, color))

    override fun draw(canvas: Canvas) {
        delegate.drawContent(bitmapInfo, iconShape, canvas, bounds, paint)
        badge?.let {
            setBadgeBounds(it, bounds)
            it.draw(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        badge?.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        badge?.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = bitmapInfo.icon.width
    override fun getIntrinsicHeight(): Int = bitmapInfo.icon.height

    open fun isThemed(): Boolean = (creationFlags and FLAG_THEMED) != 0 && bitmapInfo.themedBitmap != null
    open fun getAnimatedScale(): Float = 1.0f
    
    fun setDisabled(disabled: Boolean) { isDisabled = disabled }
    fun isSameInfo(other: BitmapInfo): Boolean = bitmapInfo == other
    fun setAnimationEnabled(enabled: Boolean) {}
    fun setHoverScaleEnabledForDisplay(enabled: Boolean) {}
    fun resetScale() {}
    
    fun getDelegate(): FastBitmapDrawableDelegate = delegate
    fun getIconColor(): Int = bitmapInfo.color
    fun getBadge(): Drawable? = badge
    fun isCreatedForTheme(): Boolean = false

    override fun getConstantState(): FastBitmapConstantState {
        return FastBitmapConstantState(
            bitmapInfo = bitmapInfo,
            isDisabled = isDisabled,
            iconShape = iconShape,
            level = level,
            badge = badge,
            disabledAlpha = disabledAlpha,
            delegateFactory = delegateFactory
        )
    }

    data class FastBitmapConstantState(
        @JvmField val bitmapInfo: BitmapInfo,
        @JvmField var isDisabled: Boolean = false,
        @JvmField val iconShape: IconShape = IconShape.EMPTY,
        @JvmField val creationFlags: Int = 0,
        @JvmField var level: Int = 0,
        @JvmField val badge: Drawable? = null,
        @JvmField val disabledAlpha: Float = 1f,
        @JvmField val delegateFactory: DelegateFactory = FastBitmapDrawableDelegate.SimpleDelegateFactory
    ) : ConstantState() {
        override fun newDrawable(): FastBitmapDrawable = FastBitmapDrawable(bitmapInfo, iconShape, delegateFactory, badge, disabledAlpha).apply {
            isDisabled = this@FastBitmapConstantState.isDisabled
            level = this@FastBitmapConstantState.level
        }
        override fun getChangingConfigurations(): Int = 0
    }
    
    companion object {
        const val WHITE_SCRIM_ALPHA = 100
        @JvmStatic
        fun getDisabledColorFilter(): ColorFilter? = null
        @JvmStatic
        fun setBadgeBounds(badge: Drawable, bounds: Rect) {
            val size = (bounds.width() * 0.444f).toInt()
            val offset = bounds.width() - size
            badge.setBounds(
                bounds.left + offset,
                bounds.top + offset,
                bounds.right,
                bounds.bottom
            )
        }
    }
}
