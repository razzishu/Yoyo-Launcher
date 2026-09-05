package com.yoyo.launcher.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

        TextView versionPill = view.findViewById(R.id.version_pill);
        if (versionPill != null) {
            try {
                String versionName = requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                if (versionName != null) {
                    versionPill.setText("v" + versionName);
                }
            } catch (Exception e) {
                versionPill.setText("v1.0.10beta");
            }
        }

        View closeBtn = view.findViewById(R.id.btn_close_settings);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }

        View iconsThemeCard = view.findViewById(R.id.menu_icons_theme);
        if (iconsThemeCard != null) {
            iconsThemeCard.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openSubSettings(
                            null,
                            R.xml.launcher_theme_preferences,
                            "Look & Feel"
                    );
                }
            });
        }

        View homescreenCard = view.findViewById(R.id.menu_homescreen);
        if (homescreenCard != null) {
            homescreenCard.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openSubSettings(
                            null,
                            R.xml.launcher_homescreen_preferences,
                            "Home Screen & Layout"
                    );
                }
            });
        }

        View drawerCard = view.findViewById(R.id.menu_app_drawer);
        if (drawerCard != null) {
            drawerCard.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openSubSettings(
                            null,
                            R.xml.launcher_app_drawer_preferences,
                            "App Drawer & Search"
                    );
                }
            });
        }

        View privacyCard = view.findViewById(R.id.menu_privacy_system);
        if (privacyCard != null) {
            privacyCard.setOnClickListener(v -> {
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).openSubSettings(
                            null,
                            R.xml.launcher_general_preferences,
                            "Privacy & System"
                    );
                }
            });
        }

        if (getActivity() instanceof SettingsActivity) {
            ((SettingsActivity) getActivity()).updateToolbarState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof SettingsActivity) {
            ((SettingsActivity) getActivity()).updateToolbarState();
        }
    }
}
