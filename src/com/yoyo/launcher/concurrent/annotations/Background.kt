package com.yoyo.launcher.concurrent.annotations

import javax.inject.Qualifier

/**
 * Qualifier for an Executor or derivative that runs on a background thread.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Background
