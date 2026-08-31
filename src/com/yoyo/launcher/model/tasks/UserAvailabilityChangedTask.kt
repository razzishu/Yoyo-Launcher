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

package com.yoyo.launcher.model.tasks

import android.os.UserHandle
import com.yoyo.launcher.Flags
import com.yoyo.launcher.LauncherModel.ModelUpdateTask
import com.yoyo.launcher.model.AllAppsList
import com.yoyo.launcher.model.BgDataModel
import com.yoyo.launcher.model.ModelTaskController
import com.yoyo.launcher.model.data.AppsListData.Companion.FLAG_PRIVATE_PROFILE_QUIET_MODE_ENABLED
import com.yoyo.launcher.model.data.AppsListData.Companion.FLAG_QUIET_MODE_ENABLED
import com.yoyo.launcher.model.data.AppsListData.Companion.FLAG_WORK_PROFILE_QUIET_MODE_ENABLED
import com.yoyo.launcher.model.data.ItemInfoWithIcon.FLAG_DISABLED_QUIET_USER
import com.yoyo.launcher.pm.UserCache
import com.yoyo.launcher.util.FlagOp
import com.yoyo.launcher.util.ItemInfoMatcher

/**
 * Model task to handle a profile user availability changes (eg, enabling/disabling work-profile or
 * private profile).
 */
class UserAvailabilityChangedTask(private val user: UserHandle) : ModelUpdateTask {

    override fun execute(
        taskController: ModelTaskController,
        dataModel: BgDataModel,
        apps: AllAppsList,
    ) {
        val ums = UserCache.INSTANCE[taskController.context].userManagerState
        val userInfo = ums.getCachedInfo(user)

        val isUserQuiet = userInfo.isQuietModeEnabled
        val flagOp = FlagOp.NO_OP.setFlag(FLAG_DISABLED_QUIET_USER, isUserQuiet)

        apps.updateDisabledFlags(ItemInfoMatcher.ofUser(user), flagOp)

        if (Flags.enablePrivateSpace()) {
            if (userInfo.iconInfo.isWork) {
                apps.setFlags(FLAG_WORK_PROFILE_QUIET_MODE_ENABLED, isUserQuiet)
            } else if (userInfo.iconInfo.isPrivate) {
                apps.setFlags(FLAG_PRIVATE_PROFILE_QUIET_MODE_ENABLED, isUserQuiet)
            }
        } else {
            // We are not synchronizing here, as int operations are atomic
            apps.setFlags(FLAG_QUIET_MODE_ENABLED, ums.isAnyProfileQuietModeEnabled)
        }
        taskController.bindApplicationsIfNeeded()

        val updates =
            dataModel.updateAndCollectWorkspaceItemInfos(
                user,
                {
                    val oldFlag = it.runtimeStatusFlags
                    it.runtimeStatusFlags = flagOp.apply(oldFlag)
                    it.runtimeStatusFlags != oldFlag
                },
            )
        taskController.bindUpdatedWorkspaceItems(updates)
    }
}
