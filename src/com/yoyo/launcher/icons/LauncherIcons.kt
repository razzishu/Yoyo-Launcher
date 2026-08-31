/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yoyo.launcher.icons

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.yoyo.launcher.Flags
import com.yoyo.launcher.InvariantDeviceProfile
import com.yoyo.launcher.dagger.ApplicationContext
import com.yoyo.launcher.dagger.LauncherAppSingleton
import com.yoyo.launcher.dagger.LauncherComponentProvider.appComponent
import com.yoyo.launcher.graphics.ThemeManager
import com.yoyo.launcher.pm.UserCache
import com.yoyo.launcher.util.UserIconInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

/**
 * Wrapper class to provide access to [BaseIconFactory] and also to provide pool of this class that
 * are threadsafe.
 */
class LauncherIcons
@AssistedInject
internal constructor(
    @ApplicationContext context: Context,
    idp: InvariantDeviceProfile,
    themeManager: ThemeManager,
    private var userCache: UserCache,
    @Assisted private val pool: ConcurrentLinkedQueue<LauncherIcons>,
) :
    BaseIconFactory(
        context,
        idp.fillResIconDpi,
        idp.iconBitmapSize,
        /* drawFullBleedIcons */ Flags.enableLauncherIconShapes(),
        themeManager.themeController,
    ),
    AutoCloseable {

    /** Recycles a LauncherIcons that may be in-use. */
    fun recycle() {
        clear()
        pool.add(this)
    }

    override fun getUserInfo(user: UserHandle): UserIconInfo {
        return userCache.getUserInfo(user)
    }

    override fun close() {
        recycle()
    }

    @AssistedFactory
    internal interface LauncherIconsFactory {
        fun create(pool: ConcurrentLinkedQueue<LauncherIcons>): LauncherIcons
    }

    @LauncherAppSingleton
    class IconPool @Inject internal constructor(private val factory: LauncherIconsFactory) {
        private var pool = ConcurrentLinkedQueue<LauncherIcons>()

        fun obtain(): LauncherIcons = pool.let { it.poll() ?: factory.create(it) }

        fun clear() {
            pool = ConcurrentLinkedQueue()
        }
    }

    companion object {

        /**
         * Return a new LauncherIcons instance from the global pool. Allows us to avoid allocating
         * new objects in many cases.
         */
        @JvmStatic
        fun obtain(context: Context): LauncherIcons = context.appComponent.iconPool.obtain()

        @JvmStatic fun clearPool(context: Context) = context.appComponent.iconPool.clear()

        @JvmStatic fun getBadgeSizeForIconSize(iconSize: Int): Float = iconSize * 0.444f
        
        @JvmStatic fun wrapToAdaptiveIcon(context: Context, drawable: Drawable): AdaptiveIconDrawable? {
            android.util.Log.d("LauncherIcons", "wrapToAdaptiveIcon: drawable=$drawable")
            if (drawable is AdaptiveIconDrawable) {
                return drawable
            }
            // Create a fake AdaptiveIconDrawable with a white background and the drawable as foreground
            // This ensures it has the right shape and is visible during drag.
            return AdaptiveIconDrawable(
                ColorDrawable(Color.WHITE),
                drawable
            )
        }
    }
}
