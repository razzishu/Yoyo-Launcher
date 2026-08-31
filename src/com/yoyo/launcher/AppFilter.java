package com.yoyo.launcher;

import android.content.ComponentName;
import android.content.Context;

import com.yoyo.launcher.dagger.ApplicationContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Utility class to filter out components from various lists
 */
public class AppFilter {

    private final Set<ComponentName> mFilteredComponents;

    @Inject
    public AppFilter(@ApplicationContext Context context) {
        mFilteredComponents = Arrays.stream(
                context.getResources().getStringArray(R.array.filtered_components))
                .map(ComponentName::unflattenFromString)
                .collect(Collectors.toSet());
    }

    public boolean shouldShowApp(ComponentName app) {
        return !mFilteredComponents.contains(app);
    }
}
