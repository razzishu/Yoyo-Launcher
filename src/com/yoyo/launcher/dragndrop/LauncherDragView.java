/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.yoyo.launcher.dragndrop;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.yoyo.launcher.Launcher;
import com.yoyo.launcher.LauncherState;
import com.yoyo.launcher.statemanager.StateManager;

/**
 * A DragView drawn/used by the Launcher activity.
 */
public class LauncherDragView extends DragView<Launcher> {


    public LauncherDragView(Launcher launcher, Drawable drawable, int registrationX,
            int registrationY, float initialScale, float scaleOnDrop, float finalScaleDps) {
        super(launcher, drawable, registrationX, registrationY, initialScale, scaleOnDrop,
                finalScaleDps);
    }

    public LauncherDragView(Launcher launcher, View content, int width, int height,
            int registrationX, int registrationY, float initialScale, float scaleOnDrop,
            float finalScaleDps) {
        super(launcher, content, width, height, registrationX, registrationY, initialScale,
                scaleOnDrop, finalScaleDps);
    }

    @Override
    public void animateTo(int toTouchX, int toTouchY, Runnable onCompleteRunnable, int duration) {
        mTempLoc[0] = toTouchX - mRegistrationX;
        mTempLoc[1] = toTouchY - mRegistrationY;
        mActivity.getDragLayer().animateViewIntoPosition(this, mTempLoc, 1f, mScaleOnDrop,
                mScaleOnDrop, DragLayer.ANIMATION_END_DISAPPEAR, onCompleteRunnable, duration);
    }
}
