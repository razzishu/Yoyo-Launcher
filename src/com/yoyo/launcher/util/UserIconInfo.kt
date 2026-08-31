package com.yoyo.launcher.util

import android.os.UserHandle
import com.yoyo.launcher.icons.BitmapInfo.Companion.FLAG_CLONE
import com.yoyo.launcher.icons.BitmapInfo.Companion.FLAG_PRIVATE
import com.yoyo.launcher.icons.BitmapInfo.Companion.FLAG_WORK
import com.yoyo.launcher.util.FlagOp

/**
 * Data class representing icon information for a specific user.
 */
data class UserIconInfo(
    @JvmField val user: UserHandle,
    @JvmField val type: Int,
    @JvmField val userSerial: Long = 0L
) {
    val isMain: Boolean get() = type == TYPE_MAIN
    val isWork: Boolean get() = type == TYPE_WORK
    val isCloned: Boolean get() = type == TYPE_CLONED
    val isPrivate: Boolean get() = type == TYPE_PRIVATE

    fun applyBitmapInfoFlags(op: FlagOp): FlagOp {
        var res = op
        if (isWork) res = res.addFlag(FLAG_WORK)
        if (isCloned) res = res.addFlag(FLAG_CLONE)
        if (isPrivate) res = res.addFlag(FLAG_PRIVATE)
        return res
    }

    companion object {
        const val TYPE_MAIN = 0
        const val TYPE_WORK = 1
        const val TYPE_CLONED = 2
        const val TYPE_PRIVATE = 3
    }
}
