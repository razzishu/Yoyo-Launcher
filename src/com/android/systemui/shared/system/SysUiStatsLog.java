package com.android.systemui.shared.system;

public class SysUiStatsLog {
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_UNKNOWN = 0;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_MAIN = 1;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_PRIVATE = 2;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_WORK = 3;
    public static final int LAUNCHER_UICHANGED__USER_TYPE__TYPE_CLONED = 4;

    public static void write(int code, int arg1, int arg2) {}
}
