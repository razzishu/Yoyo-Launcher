package com.android.systemui.plugins;

import android.view.View;
import android.view.ViewGroup;

public interface AllAppsRow extends Plugin {
    interface OnHeightUpdatedListener {
        void onHeightUpdated();
    }
    
    void setOnHeightUpdatedListener(OnHeightUpdatedListener listener);
    View setup(ViewGroup parent);
    int getExpectedHeight();
}
