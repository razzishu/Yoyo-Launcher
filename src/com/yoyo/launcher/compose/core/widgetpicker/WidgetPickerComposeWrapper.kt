package com.yoyo.launcher.compose.core.widgetpicker

import com.yoyo.launcher.widgetpicker.WidgetPickerActivity
import com.yoyo.launcher.widgetpicker.WidgetPickerConfig
import javax.inject.Inject

interface WidgetPickerComposeWrapper {
    fun showAllWidgets(activity: WidgetPickerActivity, widgetPickerConfig: WidgetPickerConfig)
}

class NoOpWidgetPickerComposeWrapper @Inject constructor() : WidgetPickerComposeWrapper {
    override fun showAllWidgets(activity: WidgetPickerActivity, widgetPickerConfig: WidgetPickerConfig) {
        error("Widget picker with compose is not supported")
    }
}
