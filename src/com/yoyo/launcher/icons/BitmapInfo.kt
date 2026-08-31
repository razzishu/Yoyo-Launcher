package com.yoyo.launcher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.IntDef
import com.yoyo.launcher.util.FlagOp
import com.yoyo.launcher.icons.cache.CacheLookupFlag
import com.yoyo.launcher.icons.FastBitmapDrawableDelegate.DelegateFactory

/**
 * Data class that holds all the information needed to create an icon drawable.
 */
data class BitmapInfo(
    @JvmField val icon: Bitmap,
    @JvmField val color: Int,
    @BitmapInfoFlags val flags: Int = 0,
    val defaultIconShape: IconShape = IconShape.EMPTY,
    val themedBitmap: ThemedBitmap? = null,
    val badgeInfo: BitmapInfo? = null,
    val delegateFactory: DelegateFactory = FastBitmapDrawableDelegate.SimpleDelegateFactory,
    @JvmField val matchingLookupFlag: CacheLookupFlag = CacheLookupFlag.DEFAULT_LOOKUP_FLAG
) {

    @IntDef(
        flag = true,
        value = [FLAG_WORK, FLAG_INSTANT, FLAG_CLONE, FLAG_PRIVATE, FLAG_FULL_BLEED, FLAG_NO_SHAPING],
    )
    internal annotation class BitmapInfoFlags

    @IntDef(flag = true, value = [FLAG_THEMED, FLAG_NO_BADGE, FLAG_SKIP_USER_BADGE, FLAG_CUSTOM_SHAPE])
    annotation class DrawableCreationFlags

    fun withBadgeInfo(badgeInfo: BitmapInfo?) = copy(badgeInfo = badgeInfo)

    fun withFlags(op: FlagOp): BitmapInfo = if (op === FlagOp.NO_OP) this else copy(flags = op.apply(this.flags))

    val isLowRes: Boolean get() = icon == LOW_RES_ICON

    @JvmOverloads
    fun newIcon(
        context: Context,
        @DrawableCreationFlags creationFlags: Int = 0,
        iconShape: IconShape? = null
    ): FastBitmapDrawable {
        val result = FastBitmapDrawable(
            this,
            iconShape ?: defaultIconShape,
            when {
                isLowRes -> PlaceHolderDrawableDelegate.PlaceHolderDelegateFactory(context)
                (creationFlags and FLAG_THEMED) != 0 && themedBitmap != null && themedBitmap !== ThemedBitmap.NOT_SUPPORTED -> themedBitmap.newDelegateFactory(this, context)
                else -> delegateFactory
            },
            if ((creationFlags and FLAG_NO_BADGE) == 0) getBadgeDrawable(context, (creationFlags and FLAG_THEMED) != 0, (creationFlags and FLAG_SKIP_USER_BADGE) != 0) else null,
            GraphicsUtils.getFloat(context, com.yoyo.launcher.R.attr.disabledIconAlpha, 1f)
        )
        result.creationFlags = if (iconShape != null) creationFlags or FLAG_CUSTOM_SHAPE else creationFlags
        return result
    }

    fun applyBitmapInfoFlags(op: FlagOp): BitmapInfo = withFlags(op)

    @JvmOverloads
    fun getBadgeDrawable(context: Context, isThemed: Boolean, skipUserBadge: Boolean = false): Drawable? {
        return null
    }
    
    fun getMatchingLookupFlag(): CacheLookupFlag = matchingLookupFlag
    
    fun isFullBleed(): Boolean = (flags and FLAG_FULL_BLEED) != 0

    companion object {
        const val FLAG_WORK: Int = 1 shl 0
        const val FLAG_INSTANT: Int = 1 shl 1
        const val FLAG_CLONE: Int = 1 shl 2
        const val FLAG_PRIVATE: Int = 1 shl 3
        const val FLAG_FULL_BLEED: Int = 1 shl 4
        const val FLAG_NO_SHAPING: Int = 1 shl 5

        const val FLAG_THEMED: Int = 1 shl 0
        const val FLAG_NO_BADGE: Int = 1 shl 1
        const val FLAG_SKIP_USER_BADGE: Int = 1 shl 2
        const val FLAG_CUSTOM_SHAPE: Int = 1 shl 3

        @JvmField val LOW_RES_ICON: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        @JvmField val LOW_RES_INFO: BitmapInfo = fromBitmap(LOW_RES_ICON)

        @JvmStatic
        fun fromBitmap(bitmap: Bitmap): BitmapInfo {
            return of(bitmap, 0, IconShape.EMPTY)
        }

        @JvmStatic
        fun of(bitmap: Bitmap, color: Int, defaultShape: IconShape = IconShape.EMPTY): BitmapInfo {
            return BitmapInfo(icon = bitmap, color = color, defaultIconShape = defaultShape)
        }
    }
}
