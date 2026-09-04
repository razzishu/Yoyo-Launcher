/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yoyo.launcher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.EdgeEffect;

/**
 * An EdgeEffect implementation that performs no drawing or stretching.
 * Used to suppress overscroll stretch/glow effects that would otherwise distort
 * full-screen containers and expose underlying windows/wallpapers.
 */
public class NoOpEdgeEffect extends EdgeEffect {

    public NoOpEdgeEffect(Context context) {
        super(context);
    }

    @Override
    public boolean draw(Canvas canvas) {
        return false;
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public void onAbsorb(int velocity) {
        // No-op
    }

    @Override
    public void onPull(float deltaDistance) {
        // No-op
    }

    @Override
    public void onPull(float deltaDistance, float displacement) {
        // No-op
    }

    @Override
    public float onPullDistance(float deltaDistance, float displacement) {
        return 0f;
    }

    @Override
    public float getDistance() {
        return 0f;
    }

    @Override
    public void onRelease() {
        // No-op
    }
}
