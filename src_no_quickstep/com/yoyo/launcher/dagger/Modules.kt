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

import com.yoyo.launcher.dragndrop.SystemDragController
import com.yoyo.launcher.dragndrop.SystemDragControllerStub
import com.yoyo.launcher.homescreenfiles.HomeScreenFilesNoOpProvider
import com.yoyo.launcher.homescreenfiles.HomeScreenFilesProvider
import com.yoyo.launcher.util.window.RefreshRateTracker
import com.yoyo.launcher.util.window.RefreshRateTracker.RefreshRateTrackerImpl
import com.yoyo.launcher.widget.LauncherWidgetHolder.WidgetHolderFactory
import com.yoyo.launcher.widget.LauncherWidgetHolder.WidgetHolderFactoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides

private object Modules {}

@Module abstract class WindowManagerProxyModule {}

@Module abstract class ActivityContextModule {}

@Module abstract class ApiWrapperModule {}

@Module
abstract class WidgetModule {
    @Binds
    abstract fun bindWidgetHolderFactory(factor: WidgetHolderFactoryImpl): WidgetHolderFactory
}

@Module abstract class PluginManagerWrapperModule {}

@Module
abstract class StaticObjectModule {
    @Binds abstract fun bindRefreshRateTracker(tracker: RefreshRateTrackerImpl): RefreshRateTracker
}

@Module
object SystemDragModule {
    @Provides
    @LauncherAppSingleton
    fun provideSystemDragController(): SystemDragController = SystemDragControllerStub()
}

// Module containing bindings for the final derivative app
@Module abstract class AppModule {}

// Module containing bindings of [ActivityContext] for the final derivative app
@Module abstract class AppActivityContextModule {}

@Module abstract class PerDisplayModule {}

@Module abstract class LauncherConcurrencyModule {}

/** A dagger module responsible for managing files on the home screen. */
@Module
object HomeScreenFilesModule {
    @Provides
    @LauncherAppSingleton
    fun provideHomeScreenFilesProvider(): HomeScreenFilesProvider = HomeScreenFilesNoOpProvider()
}
