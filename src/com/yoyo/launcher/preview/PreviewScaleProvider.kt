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

package com.yoyo.launcher.preview

import android.appwidget.AppWidgetManager
import android.graphics.PointF
import android.util.SizeF
import com.yoyo.launcher.DeviceProfile.DEFAULT_SCALE
import com.yoyo.launcher.DeviceProfile.ViewScaleProvider
import com.yoyo.launcher.model.data.ItemInfo
import com.yoyo.launcher.model.data.LauncherAppWidgetInfo
import com.yoyo.launcher.views.ActivityContext
import com.yoyo.launcher.widget.util.WidgetSizeHandler.Companion.getWidgetSizeList
import com.yoyo.launcher.widget.util.WidgetSizes
import kotlin.math.pow

/** A [ViewScaleProvider] which scales widgets based on existing widget size */
class PreviewScaleProvider(val target: ActivityContext) : ViewScaleProvider {

    override fun getScaleFromItemInfo(info: ItemInfo?): PointF {
        if (info !is LauncherAppWidgetInfo) {
            return DEFAULT_SCALE
        }

        val density = target.asContext().resources.displayMetrics.density
        if (density == 0f) return DEFAULT_SCALE

        val existingSizes: List<SizeF>
        try {
            existingSizes =
                AppWidgetManager.getInstance(target.asContext())
                    .getAppWidgetOptions(info.appWidgetId)
                    .getWidgetSizeList() ?: return DEFAULT_SCALE
        } catch (e: Exception) {
            // Failed to get widget options, ignore
            return DEFAULT_SCALE
        }

        val expectedSize =
            WidgetSizes.getWidgetSizePx(target.deviceProfile, info.spanX, info.spanY).let {
                SizeF(it.width / density, it.height / density)
            }
        if (expectedSize.height <= 0 || expectedSize.width <= 0) return DEFAULT_SCALE

        // Find the size which is closest to the expected size
        val bestOriginalSize =
            existingSizes
                .asSequence()
                .filter { it.height > 0 && it.width > 0 }
                .minByOrNull {
                    (it.width - expectedSize.width).pow(2) +
                        (it.height - expectedSize.height).pow(2)
                } ?: return DEFAULT_SCALE

        return PointF(
            expectedSize.width / bestOriginalSize.width,
            expectedSize.height / bestOriginalSize.height,
        )
    }
}
