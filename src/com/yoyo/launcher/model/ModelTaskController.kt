/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context
import com.yoyo.launcher.LauncherModel
import com.yoyo.launcher.LauncherModel.CallbackTask
import com.yoyo.launcher.celllayout.CellPosMapper
import com.yoyo.launcher.dagger.ApplicationContext
import com.yoyo.launcher.icons.IconCache
import com.yoyo.launcher.model.BgDataModel.FixedContainerItems
import com.yoyo.launcher.model.data.AppInfo
import com.yoyo.launcher.model.data.ItemInfo
import com.yoyo.launcher.util.Executors.MAIN_EXECUTOR
import com.yoyo.launcher.widget.model.WidgetsListBaseEntriesBuilder
import java.util.function.Predicate
import javax.inject.Inject

/** Class with utility methods and properties for running a LauncherModel Task */
class ModelTaskController
@Inject
constructor(
    @ApplicationContext val context: Context,
    val iconCache: IconCache,
    val dataModel: BgDataModel,
    val allAppsList: AllAppsList,
    val model: LauncherModel,
) {

    private val uiExecutor = MAIN_EXECUTOR

    /** Schedules a {@param task} to be executed on the current callbacks. */
    fun scheduleCallbackTask(task: CallbackTask) {
        for (cb in model.callbacks) {
            uiExecutor.execute { task.execute(cb) }
        }
    }

    /**
     * Updates from model task, do not deal with icon position in hotseat. Also no need to verify
     * changes as the ModelTasks always push the changes to callbacks
     */
    fun getModelWriter() = model.getWriter(false /* verifyChanges */, CellPosMapper.DEFAULT, null)

    fun bindUpdatedWorkspaceItems(allUpdates: Collection<ItemInfo>) {
        // Bind workspace items
        val workspaceUpdates = allUpdates.filter { it.id != ItemInfo.NO_ID }.toSet()
        if (workspaceUpdates.isNotEmpty()) {
            scheduleCallbackTask { it.bindItemsUpdated(workspaceUpdates) }
        }
    }

    fun bindExtraContainerItems(item: FixedContainerItems) {
        scheduleCallbackTask { it.bindExtraContainerItems(item) }
    }

    fun bindUpdatedWidgets(dataModel: BgDataModel) {
        val allWidgets =
            WidgetsListBaseEntriesBuilder(context)
                .build(dataModel.widgetsModel.widgetsByPackageItemForPicker)
        dataModel.notifyWidgetsUpdate(allWidgets)
        scheduleCallbackTask { it.bindAllWidgets(allWidgets) }
    }

    fun deleteAndBindComponentsRemoved(matcher: Predicate<ItemInfo?>, reason: String?) {
        getModelWriter().deleteItemsFromDatabase(matcher, reason)

        // Call the components-removed callback
        scheduleCallbackTask { it.bindWorkspaceComponentsRemoved(matcher) }
    }

    fun bindApplicationsIfNeeded() {
        if (allAppsList.getAndResetChangeFlag()) {
            // shallow copy
            val data = allAppsList.immutableData
            scheduleCallbackTask {
                it.bindAllApplications(data.apps, data.flags, data.packageUserKeyToUidMap)
            }
        }
    }

    fun bindIncrementalUpdates(updatedAppInfos: List<AppInfo>) {
        if (updatedAppInfos.isNotEmpty()) {
            updatedAppInfos.forEach { info ->
                scheduleCallbackTask { it.bindIncrementalDownloadProgressUpdated(info) }
            }
        }
    }
}
