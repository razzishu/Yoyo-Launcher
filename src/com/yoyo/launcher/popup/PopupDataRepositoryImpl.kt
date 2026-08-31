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

package com.yoyo.launcher.popup

import android.content.Context
import com.yoyo.launcher.LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
import com.yoyo.launcher.LauncherSettings.Favorites.ITEM_TYPE_APP_PAIR
import com.yoyo.launcher.LauncherSettings.Favorites.ITEM_TYPE_FOLDER
import com.yoyo.launcher.dagger.ApplicationContext
import com.yoyo.launcher.dagger.LauncherAppSingleton
import com.yoyo.launcher.model.data.ItemInfo
import com.yoyo.launcher.model.data.LauncherAppWidgetInfo
import com.yoyo.launcher.model.repository.HomeScreenRepository
import com.yoyo.launcher.util.DaggerSingletonTracker
import com.yoyo.launcher.util.Executors.DATA_HELPER_EXECUTOR
import com.yoyo.launcher.widget.WidgetManagerHelper
import javax.inject.Inject

@LauncherAppSingleton
class PopupDataRepositoryImpl
@Inject
constructor(
    popupDataSource: PopupDataSource,
    @ApplicationContext private val context: Context,
    @LauncherAppSingleton private val homeScreenRepository: HomeScreenRepository,
    lifeCycle: DaggerSingletonTracker,
) : PopupDataRepository {
    private val widgetManagerHelper = WidgetManagerHelper(context)
    private val folderSystemShortcuts = listOf(popupDataSource.removePopupData)
    private val appPairSystemShortcuts = listOf(popupDataSource.removePopupData)
    private val widgetSystemShortcuts = listOf(popupDataSource.removePopupData)
    private val widgetWithSettingsSystemShortcuts =
        listOf(popupDataSource.removePopupData, popupDataSource.widgetSettingsPopupData)
    private var popupData: Map<Int, List<PopupData>> = mapOf()

    init {
        lifeCycle.addCloseable(
            homeScreenRepository.workspaceState.forEach(DATA_HELPER_EXECUTOR) {
                aggregate(homeScreenRepository.workspaceState.value)
            }
        )
    }

    override fun getAllPopupData(): Map<Int, List<PopupData>> {
        return popupData
    }

    override fun getPopupDataByItemInfo(itemInfo: ItemInfo): List<PopupData>? {
        if (!popupData.containsKey(itemInfo.id)) {
            addItem(itemInfo)
        }
        return popupData[itemInfo.id]
    }

    /**
     * Clear the existing popupData map and re-aggregate it with all the current items.
     *
     * @param itemInfos is a list of ItemInfo for which each is linked to a popupData list specific
     *   to it.
     */
    private fun aggregate(itemInfos: Iterable<ItemInfo>) {
        popupData = buildMap {
            itemInfos.forEach {
                val itemPopupData: List<PopupData>? = getPopupDataForItemInfo(it)
                if (itemPopupData != null) {
                    put(it.id, itemPopupData)
                }
            }
        }
    }

    /**
     * Adds new item to map to a list of popupData specific to it.
     *
     * @param itemInfo is the item to which we will link a popupData list specific to it.
     */
    private fun addItem(itemInfo: ItemInfo) {
        val itemPopupData: List<PopupData>? = getPopupDataForItemInfo(itemInfo)
        if (itemPopupData != null) {
            popupData = popupData + (itemInfo.id to itemPopupData)
        }
    }

    /**
     * Determines the list of PopupData for a ItemInfo
     *
     * @param itemInfo is the item to which we will link a popupData list specific to it.
     * @return the list of PopupData that belongs to a specific type of ItemInfo.
     */
    private fun getPopupDataForItemInfo(itemInfo: ItemInfo): List<PopupData>? {
        return when (itemInfo.itemType) {
            ITEM_TYPE_FOLDER -> folderSystemShortcuts
            ITEM_TYPE_APP_PAIR -> appPairSystemShortcuts
            ITEM_TYPE_APPWIDGET -> {
                if (itemInfo is LauncherAppWidgetInfo) {
                    val launcherAppWidgetProviderInfo =
                        widgetManagerHelper.getLauncherAppWidgetInfo(
                            itemInfo.appWidgetId,
                            itemInfo.targetComponent,
                        )
                    if (launcherAppWidgetProviderInfo?.isReconfigurable == true) {
                        return widgetWithSettingsSystemShortcuts
                    }
                    return widgetSystemShortcuts
                }
                return widgetSystemShortcuts
            }
            else -> null
        }
    }
}
