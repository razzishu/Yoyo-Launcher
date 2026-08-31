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

package com.yoyo.launcher.dragndrop

import android.net.Uri
import android.view.DragAndDropPermissions
import com.yoyo.launcher.LauncherSettings.Favorites.ITEM_TYPE_SYSTEM_DRAG
import com.yoyo.launcher.model.data.WorkspaceItemInfo

/**
 * Item info used for system-level drag-and-drop. It is replaced with N-many items of more
 * appropriate types during drop handling.
 */
class SystemDragItemInfo : WorkspaceItemInfo() {

    /** The permissions for the URIs that were dropped in a system-level drag-and-drop sequence. */
    var permissions: DragAndDropPermissions? = null

    /** The list of URIs that were dropped in a system-level drag-and-drop sequence. */
    var uriList: List<Uri>? = null

    init {
        itemType = ITEM_TYPE_SYSTEM_DRAG
    }
}
