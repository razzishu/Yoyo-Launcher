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

import android.content.Context
import com.yoyo.launcher.ConstantItem
import com.yoyo.launcher.LifecycleTracker
import com.yoyo.launcher.dagger.ApplicationContext
import com.yoyo.launcher.graphics.ThemeManager.Companion.ICON_FACTORY_DAGGER_KEY
import com.yoyo.launcher.graphics.theme.IconThemeFactory
import com.yoyo.launcher.graphics.theme.MonoIconThemeFactory
import com.yoyo.launcher.graphics.theme.MonoIconThemeFactory.MONO_FACTORY_ID
import com.yoyo.launcher.graphics.theme.ThemePreference.Companion.THEME_OVERRIDES_DAGGER_KEY
import com.yoyo.launcher.model.data.ItemInfo
import com.yoyo.launcher.model.ModelDelegate
import com.yoyo.launcher.model.PredictionModelDelegate
import com.yoyo.launcher.popup.PopupDataRepository
import com.yoyo.launcher.popup.PopupDataRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.Multibinds
import dagger.multibindings.StringKey
import javax.inject.Named

@Module
abstract class LauncherModelModule {
    @Binds abstract fun bindPopupDataRepository(impl: PopupDataRepositoryImpl): PopupDataRepository


    @Multibinds @Named("MODEL_ITEMS") abstract fun extraModelItems(): Set<ItemInfo>

    @Multibinds abstract fun lifecycleTrackers(): Set<LifecycleTracker>

    @Multibinds
    @Named(THEME_OVERRIDES_DAGGER_KEY)
    abstract fun legacyThemeKeys(): Map<String, ConstantItem<String>>

    companion object {

        @Provides
        @IntoMap
        @StringKey(MONO_FACTORY_ID)
        @Named(ICON_FACTORY_DAGGER_KEY)
        @JvmStatic
        fun monoIconFactory(): IconThemeFactory = MonoIconThemeFactory

        @Provides
        @LauncherAppSingleton
        @JvmStatic
        fun provideModelDelegate(@ApplicationContext context: Context): ModelDelegate = PredictionModelDelegate(context)
    }
}
