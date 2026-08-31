package com.android.systemui.plugins;

public interface Plugin {
    default void onCreate(android.content.Context sysuiContext, android.content.Context pluginContext) {}
    default void onDestroy() {}
    default int getVersion() { return -1; }
}
