package com.yoyo.launcher.util;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class LauncherAccessibilityService extends AccessibilityService {
    private static LauncherAccessibilityService sInstance;
    private static Runnable sConnectionCallback;

    public static void setConnectionCallback(Runnable callback) {
        sConnectionCallback = callback;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        if (sConnectionCallback != null) {
            sConnectionCallback.run();
            sConnectionCallback = null;
        }
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        sInstance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    public static void lockScreen() {
        if (sInstance != null) {
            sInstance.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
    }

    public static boolean isRunning() {
        return sInstance != null;
    }
}
