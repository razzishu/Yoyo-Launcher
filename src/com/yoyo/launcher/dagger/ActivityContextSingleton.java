package com.yoyo.launcher.dagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Scope;

/**
 * Scope for the Activity context.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityContextSingleton {
}
