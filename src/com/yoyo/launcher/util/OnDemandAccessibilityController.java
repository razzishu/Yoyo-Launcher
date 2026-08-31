package com.yoyo.launcher.util;

import android.content.Context;
import android.provider.Settings;

/**
 * Utility to temporarily enable and disable the LauncherAccessibilityService
 * via WRITE_SECURE_SETTINGS, ensuring banking apps don't block the user.
 */
public class OnDemandAccessibilityController {

    public static void toggleAccessibilityService(Context context, boolean enable) {
        String service = context.getPackageName() + "/" + LauncherAccessibilityService.class.getName();
        String current = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (current == null) {
            current = "";
        }
        
        if (enable) {
            if (!current.contains(service)) {
                String newServices = current.isEmpty() ? service : current + ":" + service;
                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newServices);
                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            }
        } else {
            if (current.contains(service)) {
                String newServices = current.replace(":" + service, "")
                                            .replace(service + ":", "")
                                            .replace(service, "");
                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newServices);
            }
        }
    }
}
