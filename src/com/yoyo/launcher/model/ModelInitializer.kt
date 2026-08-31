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

package com.yoyo.launcher.model

import android.app.admin.DevicePolicyManager.ACTION_DEVICE_POLICY_RESOURCE_UPDATED
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ArchiveCompatibilityParams
import android.util.Log
import androidx.annotation.WorkerThread
import com.yoyo.launcher.BuildConfig
import com.yoyo.launcher.Flags
import com.yoyo.launcher.InvariantDeviceProfile
import com.yoyo.launcher.InvariantDeviceProfile.OnIDPChangeListener
import com.yoyo.launcher.LauncherModel
import com.yoyo.launcher.Utilities
import com.yoyo.launcher.dagger.ApplicationContext
import com.yoyo.launcher.graphics.ThemeManager
import com.yoyo.launcher.graphics.ThemeManager.ThemeChangeListener
import com.yoyo.launcher.homescreenfiles.HomeScreenFilesChangedTask
import com.yoyo.launcher.homescreenfiles.HomeScreenFilesProvider
import com.yoyo.launcher.icons.IconCache
import com.yoyo.launcher.icons.IconChangeTracker
import com.yoyo.launcher.icons.LauncherIcons.IconPool
import com.yoyo.launcher.logging.FileLog
import com.yoyo.launcher.model.tasks.PackageUpdatedTask
import com.yoyo.launcher.model.tasks.ShortcutsChangedTask
import com.yoyo.launcher.notification.NotificationListener
import com.yoyo.launcher.pm.InstallSessionHelper
import com.yoyo.launcher.pm.UserCache
import com.yoyo.launcher.shortcuts.ShortcutRequest
import com.yoyo.launcher.util.DaggerSingletonTracker
import com.yoyo.launcher.util.Executors.MAIN_EXECUTOR
import com.yoyo.launcher.util.Executors.MODEL_EXECUTOR
import com.yoyo.launcher.util.Executors.UI_HELPER_EXECUTOR
import com.yoyo.launcher.util.PackageUserKey
import com.yoyo.launcher.util.SettingsCache
import com.yoyo.launcher.util.SettingsCache.NOTIFICATION_BADGING_URI
import com.yoyo.launcher.util.SettingsCache.PRIVATE_SPACE_HIDE_WHEN_LOCKED_URI
import com.yoyo.launcher.util.SimpleBroadcastReceiver
import com.yoyo.launcher.util.SimpleBroadcastReceiver.Companion.actionsFilter
import com.yoyo.launcher.widget.custom.CustomWidgetManager
import javax.inject.Inject

/** Utility class for initializing all model callbacks */
class ModelInitializer
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val iconPool: IconPool,
    private val iconCache: IconCache,
    private val idp: InvariantDeviceProfile,
    private val themeManager: ThemeManager,
    private val userCache: UserCache,
    private val settingsCache: SettingsCache,
    private val customWidgetManager: CustomWidgetManager,
    private val installSessionHelper: InstallSessionHelper,
    private val homeScreenFilesProvider: HomeScreenFilesProvider,
    private val lifeCycle: DaggerSingletonTracker,
    private val homeScreenFilesChangedTask: HomeScreenFilesChangedTask.Factory,
    private val iconChangeTracker: IconChangeTracker,
) {

    fun initialize(model: LauncherModel) {
        initializeDisplayEvents(model)

        // System changes
        val modelCallbacks = model.newModelCallbacks()
        val launcherApps = context.getSystemService(LauncherApps::class.java)!!
        launcherApps.registerCallback(modelCallbacks, MODEL_EXECUTOR.handler)
        lifeCycle.addCloseable {
            launcherApps.unregisterCallback(modelCallbacks)
        }

        if (Utilities.ATLEAST_V && Flags.enableSupportForArchiving()) {
            launcherApps.setArchiveCompatibility(
                ArchiveCompatibilityParams().apply {
                    setEnableUnarchivalConfirmation(false)
                    setEnableIconOverlay(!Flags.useNewIconForArchivedApps())
                }
            )
        }

        // Device profile policy changes
        val dpUpdateReceiver =
            SimpleBroadcastReceiver(
                context = context,
                executor = UI_HELPER_EXECUTOR,
                callbackExecutor = MODEL_EXECUTOR,
            ) {
                model.enqueueModelUpdateTask(ReloadStringCacheTask())
            }
        dpUpdateReceiver.register(actionsFilter(ACTION_DEVICE_POLICY_RESOURCE_UPDATED))
        lifeCycle.addCloseable(dpUpdateReceiver)

        // Development helper
        if (BuildConfig.IS_STUDIO_BUILD) {
            val reloadReceiver =
                SimpleBroadcastReceiver(context, UI_HELPER_EXECUTOR) { model.forceReload() }
            reloadReceiver.register(actionsFilter(ACTION_FORCE_RELOAD), Context.RECEIVER_EXPORTED)
            lifeCycle.addCloseable(reloadReceiver)
        }

        // User changes
        lifeCycle.addCloseable(userCache.addUserEventListener(model::onUserEvent))

        // Private space settings changes
        val psSettingsListener = SettingsCache.OnChangeListener { model.forceReload() }
        settingsCache.register(PRIVATE_SPACE_HIDE_WHEN_LOCKED_URI, psSettingsListener)
        lifeCycle.addCloseable {
            settingsCache.unregister(PRIVATE_SPACE_HIDE_WHEN_LOCKED_URI, psSettingsListener)
        }

        // Notification dots changes
        val notificationChanges =
            SettingsCache.OnChangeListener { dotsEnabled ->
                if (dotsEnabled)
                    NotificationListener.requestRebind(
                        ComponentName(context, NotificationListener::class.java)
                    )
            }
        settingsCache.register(NOTIFICATION_BADGING_URI, notificationChanges)
        notificationChanges.onSettingsChanged(settingsCache.getValue(NOTIFICATION_BADGING_URI))
        lifeCycle.addCloseable {
            settingsCache.unregister(NOTIFICATION_BADGING_URI, notificationChanges)
        }

        // Custom widgets
        lifeCycle.addCloseable(customWidgetManager.addWidgetRefreshCallback(model::rebindCallbacks))

        // Install session changes
        lifeCycle.addCloseable(installSessionHelper.registerInstallTracker(modelCallbacks))

        // Fallback for package updates using BroadcastReceiver
        val packageReceiver = SimpleBroadcastReceiver(context, MODEL_EXECUTOR) { intent ->
            val action = intent.action
            val packageName = intent.data?.schemeSpecificPart
            if (packageName != null) {
                Log.i("ModelInitializer", "Package event from fallback: $action for $packageName")
                val user = android.os.Process.myUserHandle()
                when (action) {
                    android.content.Intent.ACTION_PACKAGE_ADDED -> {
                        val replacing = intent.getBooleanExtra(android.content.Intent.EXTRA_REPLACING, false)
                        if (!replacing) {
                            modelCallbacks.onPackageAdded(packageName, user)
                        } else {
                            modelCallbacks.onPackageChanged(packageName, user)
                        }
                    }
                    android.content.Intent.ACTION_PACKAGE_REMOVED,
                    android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                        val replacing = intent.getBooleanExtra(android.content.Intent.EXTRA_REPLACING, false)
                        if (!replacing) {
                            modelCallbacks.onPackageRemoved(packageName, user)
                        }
                    }
                    android.content.Intent.ACTION_PACKAGE_CHANGED,
                    android.content.Intent.ACTION_PACKAGE_REPLACED -> {
                        modelCallbacks.onPackageChanged(packageName, user)
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PACKAGE_ADDED)
            addAction(android.content.Intent.ACTION_PACKAGE_REMOVED)
            addAction(android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addAction(android.content.Intent.ACTION_PACKAGE_CHANGED)
            addAction(android.content.Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        packageReceiver.register(filter, Context.RECEIVER_EXPORTED)
        lifeCycle.addCloseable(packageReceiver)

        // Monitor changes to files shown on homescreen.
        lifeCycle.addCloseable(
            homeScreenFilesProvider.fileChanges.forEach(MODEL_EXECUTOR) {
                model.enqueueModelUpdateTask(homeScreenFilesChangedTask.create(it))
            }
        )
    }

    fun initializeDisplayEvents(model: LauncherModel) {
        fun refreshAndReloadLauncher() {
            android.util.Log.d("ModelInitializer", "refreshAndReloadLauncher: clearPool and reload. themeEnabled=" + themeManager.isIconThemeEnabled)
            iconPool.clear()
            iconCache.updateIconParams(idp.fillResIconDpi, idp.iconBitmapSize)
            model.forceReload()
        }

        // IDP changes
        val idpChangeListener = OnIDPChangeListener { modelChanged ->
            if (modelChanged) refreshAndReloadLauncher()
        }
        idp.addOnChangeListener(idpChangeListener)
        lifeCycle.addCloseable { idp.removeOnChangeListener(idpChangeListener) }

        // Shape changes
        lifeCycle.addCloseable(
            themeManager.iconShapeData.forEach(MAIN_EXECUTOR) { model.rebindCallbacks() }
        )

        // Theme changes
        val themeChangeListener = ThemeChangeListener { refreshAndReloadLauncher() }
        themeManager.addChangeListener(themeChangeListener)
        lifeCycle.addCloseable { themeManager.removeChangeListener(themeChangeListener) }

        // Icon changes
        lifeCycle.addCloseable(
            iconChangeTracker.changes.forEach(MODEL_EXECUTOR) { onAppIconChanged(model, it) }
        )
    }

    /** Called when the icon for an app changes, outside of package event */
    @WorkerThread
    private fun onAppIconChanged(model: LauncherModel, event: PackageUserKey) {
        // Update the icon for the calendar package
        Log.d(TAG, "onAppIconChanged: ${event.mPackageName}")
        model.enqueueModelUpdateTask(
            PackageUpdatedTask(PackageUpdatedTask.OP_UPDATE, event.mUser, event.mPackageName)
        )
        ShortcutRequest(context, event.mUser)
            .forPackage(event.mPackageName)
            .query(ShortcutRequest.PINNED)
            .let {
                if (it.isNotEmpty()) {
                    model.enqueueModelUpdateTask(
                        ShortcutsChangedTask(event.mPackageName, it, event.mUser, false)
                    )
                }
            }
    }

    companion object {
        private const val TAG = "ModelInitializer"
        private const val ACTION_FORCE_RELOAD = "force-reload-launcher"
    }
}
