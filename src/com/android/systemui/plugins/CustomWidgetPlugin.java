package com.android.systemui.plugins;

import android.content.Context;
import com.yoyo.launcher.widget.LauncherAppWidgetHostView;
import com.yoyo.launcher.widget.custom.CustomAppWidgetProviderInfo;

public interface CustomWidgetPlugin extends Plugin {
    void updateWidgetInfo(CustomAppWidgetProviderInfo info, Context context);
    void onViewCreated(LauncherAppWidgetHostView view);
}
