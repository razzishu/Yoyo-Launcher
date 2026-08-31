package com.yoyo.launcher.util;

import android.content.ComponentName;
import android.os.UserHandle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Arrays;

public class ComponentKey {

    public final ComponentName componentName;
    public final UserHandle user;
    private final int mHashCode;

    public ComponentKey(ComponentName componentName, UserHandle user) {
        if (componentName == null || user == null) {
            throw new NullPointerException();
        }
        this.componentName = componentName;
        this.user = user;
        mHashCode = Arrays.hashCode(new Object[] {componentName, user});
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ComponentKey other) 
                && other.componentName.equals(componentName) 
                && other.user.equals(user);
    }

    @Override
    public String toString() {
        return componentName.flattenToString() + "#" + user.hashCode();
    }
}
