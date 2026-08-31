package com.yoyo.launcher.dagger

import android.net.Uri
import android.provider.Settings
import com.yoyo.launcher.util.SettingsCache.NOTIFICATION_BADGING_URI
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Named

@Module
class SettingsModule {
    @Provides
    @IntoSet
    @Named("SETTINGS_ENABLED_BY_DEFAULT")
    fun provideEnableTaskbarDefaults(): Uri = Uri.parse("content://" + Settings.AUTHORITY + "/enable_taskbar")

    @Provides
    @IntoSet
    @Named("SETTINGS_ENABLED_BY_DEFAULT")
    fun provideNavigationBarHintDefaults(): Uri = Uri.parse("content://" + Settings.AUTHORITY + "/nav_bar_hint")

    @Provides
    @IntoSet
    @Named("SETTINGS_ENABLED_BY_DEFAULT")
    fun provideNotificationBadgingDefaults(): Uri = NOTIFICATION_BADGING_URI
}
