package com.android.systemui.plugins;

public interface PluginListener<T> {
    void onPluginConnected(T plugin, android.content.Context context);
    void onPluginDisconnected(T plugin);
}
