/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.yoyo.launcher.views.FloatingIconView.getLocationBoundsForView;
import static com.yoyo.launcher.views.FloatingIconViewCompanion.setPropertiesVisible;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import androidx.annotation.NonNull;

import com.yoyo.launcher.AbstractFloatingView;
import com.yoyo.launcher.GestureNavContract;
import com.yoyo.launcher.Insettable;
import com.yoyo.launcher.Launcher;
import com.yoyo.launcher.R;
import com.yoyo.launcher.util.Executors;
import com.yoyo.launcher.util.window.RefreshRateTracker;

/**
 * Similar to {@link FloatingIconView} but displays a surface with the targetIcon. It then passes
 * the surfaceHandle to the {@link GestureNavContract}.
 */
public class FloatingSurfaceView extends AbstractFloatingView implements
        OnGlobalLayoutListener, Insettable, SurfaceHolder.Callback2 {

    private final RectF mTmpPosition = new RectF();

    private final Launcher mLauncher;
    private final RectF mIconPosition = new RectF();

    private final Rect mIconBounds = new Rect();
    private final Picture mPicture = new Picture();
    private final Runnable mRemoveViewRunnable = this::removeViewFromParent;

    private final SurfaceView mSurfaceView;

    private View mIcon;
    private GestureNavContract mContract;

    public FloatingSurfaceView(Context context) {
        this(context, null);
    }

    public FloatingSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);

        mSurfaceView = new SurfaceView(context);
        mSurfaceView.setZOrderOnTop(true);

        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceView.getHolder().addCallback(this);
        mIsOpen = true;
        addView(mSurfaceView);
    }

    @Override
    protected void handleClose(boolean animate) {
        setCurrentIconVisible(true);
        mLauncher.getViewCache().recycleView(R.layout.floating_surface_view, this);
        mContract = null;
        mIcon = null;
        mIsOpen = false;

        // Remove after some time, to avoid flickering
        Executors.MAIN_EXECUTOR.getHandler().postDelayed(mRemoveViewRunnable,
                RefreshRateTracker.getSingleFrameMs(mLauncher));
    }

    private void removeViewFromParent() {
        mPicture.beginRecording(1, 1);
        mPicture.endRecording();
        mLauncher.getDragLayer().removeViewInLayout(this);
    }

    private void removeViewImmediate() {
        // Cancel any pending remove
        Executors.MAIN_EXECUTOR.getHandler().removeCallbacks(mRemoveViewRunnable);
        removeViewFromParent();
    }

    /**
     * Shows the surfaceView for the provided contract
     */
    public static void show(Launcher launcher, GestureNavContract contract) {
        FloatingSurfaceView view = launcher.getViewCache().getView(R.layout.floating_surface_view,
                launcher, launcher.getDragLayer());
        view.mContract = contract;
        view.mIsOpen = true;

        view.removeViewImmediate();
        launcher.getDragLayer().addView(view);
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_ICON_SURFACE) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        close(false);
        removeViewImmediate();
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
        updateIconLocation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        setCurrentIconVisible(true);
    }

    @Override
    public void onGlobalLayout() {
        updateIconLocation();
    }

    @Override
    public void setInsets(Rect insets) { }

    private void updateIconLocation() {
        if (mContract == null) {
            return;
        }
        View icon = mLauncher.getFirstHomeElementForAppClose(null /* StableViewInfo */,
                mContract.componentName.getPackageName(), mContract.user);

        boolean iconChanged = mIcon != icon;
        if (iconChanged) {
            setCurrentIconVisible(true);
            mIcon = icon;
            setCurrentIconVisible(false);
        }

        if (icon != null && icon.isAttachedToWindow()) {
            getLocationBoundsForView(mLauncher, icon, false, mTmpPosition, mIconBounds);

            if (!mTmpPosition.equals(mIconPosition)) {
                mIconPosition.set(mTmpPosition);

                LayoutParams lp = (LayoutParams) mSurfaceView.getLayoutParams();
                lp.width = Math.round(mIconPosition.width());
                lp.height = Math.round(mIconPosition.height());
                lp.leftMargin = Math.round(mIconPosition.left);
                lp.topMargin = Math.round(mIconPosition.top);
            }
        } else if (mIconPosition.isEmpty()) {
            // Sane fallback for apps not on current workspace page (e.g. opened from app drawer):
            // Target the bottom center / hotseat where the gesture originates to avoid
            // sending an empty (0,0,0,0) rect that causes SystemUI to abort into Recents overview.
            int width = mLauncher.getDeviceProfile().getDeviceProperties().getWidthPx();
            int height = mLauncher.getDeviceProfile().getDeviceProperties().getHeightPx();
            int iconSize = mLauncher.getDeviceProfile().getWorkspaceIconProfile().getIconSizePx();
            float left = (width - iconSize) / 2f;
            float bottom = height - mLauncher.getDeviceProfile().getInsets().bottom - (iconSize / 2f);
            mTmpPosition.set(left, bottom - iconSize, left + iconSize, bottom);
            mIconPosition.set(mTmpPosition);

            LayoutParams lp = (LayoutParams) mSurfaceView.getLayoutParams();
            lp.width = Math.round(mIconPosition.width());
            lp.height = Math.round(mIconPosition.height());
            lp.leftMargin = Math.round(mIconPosition.left);
            lp.topMargin = Math.round(mIconPosition.top);
        }

        sendIconInfo();

        if (mIcon != null && iconChanged && !mIconBounds.isEmpty()) {
            // Record the icon display
            setCurrentIconVisible(true);
            Canvas c = mPicture.beginRecording(mIconBounds.width(), mIconBounds.height());
            c.translate(-mIconBounds.left, -mIconBounds.top);
            mIcon.draw(c);
            mPicture.endRecording();
            setCurrentIconVisible(false);
            drawOnSurface();
        }
    }

    private void sendIconInfo() {
        if (mContract != null) {
            mContract.sendEndPosition(mIconPosition, mLauncher, mSurfaceView.getSurfaceControl());
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        drawOnSurface();
        sendIconInfo();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder,
            int format, int width, int height) {
        drawOnSurface();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {}

    @Override
    public void surfaceRedrawNeeded(@NonNull SurfaceHolder surfaceHolder) {
        drawOnSurface();
    }

    private void drawOnSurface() {
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();

        Canvas c = surfaceHolder.lockHardwareCanvas();
        if (c != null) {
            mPicture.draw(c);
            surfaceHolder.unlockCanvasAndPost(c);
        }
    }

    private void setCurrentIconVisible(boolean isVisible) {
        if (mIcon != null) {
            setPropertiesVisible(mIcon, isVisible);
        }
    }
}
