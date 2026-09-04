package com.yoyo.launcher.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.yoyo.launcher.LauncherPrefs;
import com.yoyo.launcher.R;

public class AppDrawerOpacityPreference extends Preference {

    public AppDrawerOpacityPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AppDrawerOpacityPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AppDrawerOpacityPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AppDrawerOpacityPreference(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_app_drawer_opacity);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        SeekBar slider = (SeekBar) holder.findViewById(R.id.opacity_slider);
        TextView label = (TextView) holder.findViewById(R.id.opacity_value_label);

        if (slider == null || label == null) {
            return;
        }

        LauncherPrefs prefs = LauncherPrefs.get(getContext());
        int currentOpacity = prefs.get(LauncherPrefs.DRAWER_OPACITY);

        slider.setMax(100);
        slider.setProgress(currentOpacity);
        label.setText(currentOpacity + "%");

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                label.setText(progress + "%");
                if (fromUser) {
                    prefs.put(LauncherPrefs.DRAWER_OPACITY, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
