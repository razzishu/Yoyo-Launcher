package com.android.systemui.plugins;

import com.android.systemui.plugins.shared.LauncherOverlayManager;

public interface LauncherOverlayPlugin extends Plugin {
    LauncherOverlayManager createOverlayManager(android.app.Activity activity);
}
