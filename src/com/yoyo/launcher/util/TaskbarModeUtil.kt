/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.yoyo.launcher.util

import android.content.Context
import com.yoyo.launcher.LauncherPrefs
import com.yoyo.launcher.LauncherPrefs.Companion.TASKBAR_PINNING
import com.yoyo.launcher.config.FeatureFlags.enableTaskbarPinning
import com.yoyo.launcher.dagger.ApplicationContext
import com.yoyo.launcher.dagger.LauncherAppComponent
import com.yoyo.launcher.dagger.LauncherAppSingleton
import com.yoyo.launcher.util.window.WindowManagerProxy
import javax.inject.Inject

/**
 * A singleton class to be used in launcher code to identify Taskbar mode.
 *
 * Quickstep code will use TaskbarFeatureEvaluator class to evaluate taskbar mode.
 *
 * We need this class to support 3rd party launcher test cases and also to correctly initialize the
 * [InvariantDeviceProfile].
 */
@LauncherAppSingleton
class TaskbarModeUtil
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val displayController: DisplayController,
    private val windowManagerProxy: WindowManagerProxy,
    private val launcherPrefs: LauncherPrefs,
) {
    val isTransient: Boolean
        get() =
            if (displayController.info.navigationMode != NavigationMode.NO_BUTTON) {
                false
            } else if (enableTaskbarPinning()) {
                // If Launcher is visible on the freeform display, ensure the taskbar is pinned.
                if (
                    displayController.info.showLockedTaskbarOnHome() &&
                        displayController.info.isHomeVisible
                ) {
                    false
                } else {
                    !isPinned
                }
            } else {
                true
            }

    val isPinned: Boolean
        get() =
            if (
                windowManagerProxy.isInDesktopMode(windowManagerProxy.getDisplay(context).displayId)
            ) {
                true
            } else {
                launcherPrefs.get(TASKBAR_PINNING)
            }

    companion object {
        @JvmField val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getTaskbarModeUtil)
    }
}
