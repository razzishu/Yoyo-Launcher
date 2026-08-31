package com.yoyo.launcher.concurrent.annotations

import javax.inject.Qualifier

enum class LightweightBackgroundPriority {
    DATA,
    UI
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class LightweightBackground(
    val priority: LightweightBackgroundPriority = LightweightBackgroundPriority.DATA
)
