package com.yoyo.launcher.concurrent.annotations

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class LightweightBackgroundContext(
    val priority: LightweightBackgroundPriority = LightweightBackgroundPriority.DATA
)
