package com.yoyo.launcher.taskbar;

import android.net.Uri;
import android.provider.Settings;

public class TaskbarManagerImpl {
    public static final Uri ENABLE_TASKBAR_URI = Uri.parse("content://" + Settings.AUTHORITY + "/enable_taskbar");
    public static final Uri NAVIGATION_BAR_HINT_URI = Uri.parse("content://" + Settings.AUTHORITY + "/nav_bar_hint");
}
