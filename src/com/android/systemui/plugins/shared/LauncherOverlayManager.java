package com.android.systemui.plugins.shared;

import android.view.MotionEvent;
import java.io.PrintWriter;

public interface LauncherOverlayManager {
    interface LauncherOverlayTouchProxy {
        default void setOverlayCallbacks(LauncherOverlayCallbacks callbacks) {}
        default void onOverlayMotionEvent(MotionEvent ev, float distance) {}
        default void onOverlayMotionEvent(MotionEvent ev, int distance) {}
        default void onFlingVelocity(int velocity) {}
    }
    interface LauncherOverlayCallbacks {
        default void onOverlayScrollChanged(float progress) {}
    }

    default void onActivityDestroyed() {}
    default void onAttachedToWindow() {}
    default void onDetachedFromWindow() {}
    default void onDeviceProvideChanged() {}
    default void onActivityStopped() {}
    default void onActivityStarted() {}
    default void onActivityResumed() {}
    default void onActivityPaused() {}
    default void hideOverlay(boolean animate) {}
    default void dump(String prefix, PrintWriter writer) {}
    default void onDisallowSwipeToMinusOnePage() {}
}
