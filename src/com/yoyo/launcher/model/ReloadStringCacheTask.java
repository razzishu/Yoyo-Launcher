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
package com.yoyo.launcher.model;

import android.app.admin.DevicePolicyManager;

import androidx.annotation.NonNull;

import com.yoyo.launcher.LauncherModel.ModelUpdateTask;

/**
 * Handles updates due to changes in Device Policy Management resources triggered by
 * {@link DevicePolicyManager#ACTION_DEVICE_POLICY_RESOURCE_UPDATED}.
 */
public class ReloadStringCacheTask implements ModelUpdateTask {

    @Override
    public void execute(@NonNull ModelTaskController taskController, @NonNull BgDataModel dataModel,
            @NonNull AllAppsList apps) {
        dataModel.updateStringCache(taskController.getContext());
        StringCache cloneSC = dataModel.getStringCache();
        taskController.scheduleCallbackTask(c -> c.bindStringCache(cloneSC));
    }
}
