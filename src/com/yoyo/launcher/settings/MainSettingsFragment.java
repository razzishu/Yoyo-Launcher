package com.yoyo.launcher.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.yoyo.launcher.R;

public class MainSettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.menu_icons_theme).setOnClickListener(v -> {
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).openSubSettings(
                        "com.yoyo.launcher.settings.IconPackSettingsFragment",
                        0,
                        "Icons & Theme"
                );
            }
        });

        view.findViewById(R.id.menu_general).setOnClickListener(v -> {
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).openSubSettings(
                        null,
                        R.xml.launcher_general_preferences,
                        "General"
                );
            }
        });

        view.findViewById(R.id.menu_homescreen).setOnClickListener(v -> {
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).openSubSettings(
                        null,
                        R.xml.launcher_homescreen_preferences,
                        "Homescreen"
                );
            }
        });

        view.findViewById(R.id.menu_app_drawer).setOnClickListener(v -> {
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).openSubSettings(
                        null,
                        R.xml.launcher_app_drawer_preferences,
                        "App Drawer"
                );
            }
        });

        view.findViewById(R.id.menu_dock_search).setOnClickListener(v -> {
            if (getActivity() instanceof SettingsActivity) {
                ((SettingsActivity) getActivity()).openSubSettings(
                        null,
                        R.xml.launcher_dock_search_preferences,
                        "Dock & Search Bar"
                );
            }
        });
    }
}
