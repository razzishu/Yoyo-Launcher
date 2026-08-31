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

package com.yoyo.launcher.dagger

import com.yoyo.launcher.allapps.AllAppsStore
import com.yoyo.launcher.popup.PopupDataProvider
import com.yoyo.launcher.qsb.OseWidgetOptionsProvider
import com.yoyo.launcher.recyclerview.AllAppsRecyclerViewPool
import com.yoyo.launcher.recyclerview.AllAppsRecyclerViewPool.Companion.PRELOAD_ALL_APPS_DAGGER_KEY
import com.yoyo.launcher.secondarydisplay.SecondaryDisplayDelegate
import com.yoyo.launcher.views.ActivityContext
import dagger.BindsInstance
import javax.inject.Named

/** Base component for ActivityContext Dagger injection. */
interface BaseActivityContextComponent {

    fun getSecondaryDisplayDelegate(): SecondaryDisplayDelegate

    fun getOseWidgetOptionsProvider(): OseWidgetOptionsProvider

    val appsStore: AllAppsStore
    val popupDataProvider: PopupDataProvider
    val sharedAppsPool: AllAppsRecyclerViewPool

    /** Builder for BaseActivityContextComponent. */
    interface Builder {
        @BindsInstance fun activityContext(activityContext: ActivityContext): Builder

        @BindsInstance
        fun setAllAppsPreloaded(
            @Named(PRELOAD_ALL_APPS_DAGGER_KEY) preloadAllApps: Boolean
        ): Builder

        fun build(): BaseActivityContextComponent
    }
}
