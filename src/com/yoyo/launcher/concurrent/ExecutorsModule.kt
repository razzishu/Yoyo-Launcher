package com.yoyo.launcher.concurrent

import com.yoyo.launcher.concurrent.annotations.*
import com.google.common.util.concurrent.ListeningExecutorService
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.asCoroutineDispatcher

@Module
interface ExecutorsModule {

    @Binds
    @ThreadPool
    fun provideThreadPoolExecutor(
        @ThreadPool listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @ThreadPool
    fun provideThreadPoolExecutorService(
        @ThreadPool listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    @Binds
    @Background
    fun provideBackgroundExecutor(
        @Background listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @Background
    fun provideBackgroundExecutorService(
        @Background listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    @Binds
    @LightweightBackground(LightweightBackgroundPriority.UI)
    fun provideUiLightweightBackgroundExecutor(
        @LightweightBackground(LightweightBackgroundPriority.UI) listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @LightweightBackground(LightweightBackgroundPriority.UI)
    fun provideUiLightweightBackgroundExecutorService(
        @LightweightBackground(LightweightBackgroundPriority.UI) listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    @Binds
    @LightweightBackground(LightweightBackgroundPriority.DATA)
    fun provideDataLightweightBackgroundExecutor(
        @LightweightBackground(LightweightBackgroundPriority.DATA) listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @LightweightBackground(LightweightBackgroundPriority.DATA)
    fun provideDataLightweightBackgroundExecutorService(
        @LightweightBackground(LightweightBackgroundPriority.DATA) listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    @Binds
    @Ui
    fun provideUiExecutor(
        @Ui listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @Ui
    fun provideUiExecutorService(
        @Ui listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    companion object {
        @Provides
        @UiContext
        fun provideUiContext(@Ui executor: Executor): CoroutineContext = executor.asCoroutineDispatcher()

        @Provides
        @LightweightBackgroundContext(LightweightBackgroundPriority.DATA)
        fun provideDataLightweightContext(@LightweightBackground(LightweightBackgroundPriority.DATA) executor: Executor): CoroutineContext = executor.asCoroutineDispatcher()

        @Provides
        @LightweightBackgroundContext(LightweightBackgroundPriority.UI)
        fun provideUiLightweightContext(@LightweightBackground(LightweightBackgroundPriority.UI) executor: Executor): CoroutineContext = executor.asCoroutineDispatcher()

        @Provides
        @BackgroundContext
        fun provideBackgroundContext(@Background executor: Executor): CoroutineContext = executor.asCoroutineDispatcher()

        @Provides
        @ThreadPoolContext
        fun provideThreadPoolContext(@ThreadPool executor: Executor): CoroutineContext = executor.asCoroutineDispatcher()
    }
}
