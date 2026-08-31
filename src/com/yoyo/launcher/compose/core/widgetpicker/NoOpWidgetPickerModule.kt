package com.yoyo.launcher.compose.core.widgetpicker

import dagger.Binds
import dagger.Module

@Module
interface NoOpWidgetPickerModule {
    @Binds
    fun bindWidgetPickerWrapper(noOp: NoOpWidgetPickerComposeWrapper): WidgetPickerComposeWrapper
}
