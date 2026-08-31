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

package com.yoyo.launcher.homescreenfiles

import android.content.ContentResolver.NOTIFY_INSERT
import android.content.ContentResolver.NOTIFY_UPDATE
import android.net.Uri
import android.os.UserHandle
import com.yoyo.launcher.LauncherModel
import com.yoyo.launcher.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.yoyo.launcher.LauncherSettings.Favorites.ITEM_TYPE_FILE_SYSTEM_FILE
import com.yoyo.launcher.LauncherSettings.Favorites.ITEM_TYPE_FILE_SYSTEM_FOLDER
import com.yoyo.launcher.icons.IconCache
import com.yoyo.launcher.model.AllAppsList
import com.yoyo.launcher.model.BgDataModel
import com.yoyo.launcher.model.ModelTaskController
import com.yoyo.launcher.model.WorkspaceItemSpaceFinder
import com.yoyo.launcher.model.data.WorkspaceItemInfo
import com.yoyo.launcher.util.IntSet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Handles changes in file items shown on the home screen. */
class HomeScreenFilesChangedTask
@AssistedInject
constructor(
    @Assisted private val fileChange: HomeScreenFilesProvider.FileChange,
    private val iconCache: IconCache,
    private val workspaceItemSpaceFinder: WorkspaceItemSpaceFinder,
) : LauncherModel.ModelUpdateTask {
    override fun execute(
        taskController: ModelTaskController,
        dataModel: BgDataModel,
        apps: AllAppsList,
    ) {
        val isInsert = fileChange.flags and NOTIFY_INSERT == NOTIFY_INSERT
        val isUpdate = fileChange.flags and NOTIFY_UPDATE == NOTIFY_UPDATE
        val file = kotlin.runCatching { fileChange.file.get() }.getOrNull()

        if (isInsert && file != null) {
            processInsert(fileChange.uri, file, taskController)
        } else if (isUpdate && file != null) {
            processUpdate(fileChange.uri, file, fileChange.user, taskController, dataModel)
        } else {
            processDelete(fileChange.uri, taskController)
        }
    }

    private fun processInsert(uri: Uri, file: HomeScreenFile, taskController: ModelTaskController) {
        val item =
            WorkspaceItemInfo().apply {
                title = file.displayName
                itemType =
                    if (file.isDirectory) ITEM_TYPE_FILE_SYSTEM_FOLDER
                    else ITEM_TYPE_FILE_SYSTEM_FILE
                intent = HomeScreenFilesUtils.buildLaunchIntent(uri, file)
                bitmap = iconCache.getDefaultIcon(user)
            }
        val coords =
            workspaceItemSpaceFinder.findSpaceForItem(ArrayList(), item.spanX, item.spanY, IntSet())
        taskController
            .getModelWriter()
            .addItemToDatabase(item, CONTAINER_DESKTOP, coords.screenId, coords.cellX, coords.cellY)
        taskController.scheduleCallbackTask { cb -> cb.bindItemsAdded(listOf(item)) }
    }

    private fun processUpdate(
        uri: Uri,
        file: HomeScreenFile,
        user: UserHandle,
        taskController: ModelTaskController,
        dataModel: BgDataModel,
    ) {
        val updatedItems =
            dataModel.updateAndCollectWorkspaceItemInfos(
                user,
                {
                    if (it.intent?.data == uri) {
                        it.title = file.displayName
                        true
                    } else {
                        false
                    }
                },
            )
        if (updatedItems.isNotEmpty()) {
            taskController.bindUpdatedWorkspaceItems(updatedItems)
        } else {
            processInsert(uri, file, taskController)
        }
    }

    private fun processDelete(uri: Uri, taskController: ModelTaskController) {
        taskController.deleteAndBindComponentsRemoved(
            { it?.intent?.data == uri },
            "The file system item no longer exists",
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(fileChange: HomeScreenFilesProvider.FileChange): HomeScreenFilesChangedTask
    }
}
