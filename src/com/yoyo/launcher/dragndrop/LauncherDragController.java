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

import static android.view.View.VISIBLE;

import static com.yoyo.launcher.AbstractFloatingView.TYPE_DISCOVERY_BOUNCE;
import static com.yoyo.launcher.Flags.removeAppsRefreshOnRightClick;
import static com.yoyo.launcher.LauncherAnimUtils.SPRING_LOADED_EXIT_DELAY;
import static com.yoyo.launcher.LauncherState.EDIT_MODE;
import static com.yoyo.launcher.LauncherState.NORMAL;
import static com.yoyo.launcher.util.Executors.MAIN_EXECUTOR;

import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.yoyo.launcher.AbstractFloatingView;
import com.yoyo.launcher.DragSource;
import com.yoyo.launcher.DropTarget;
import com.yoyo.launcher.DropTarget.DragObject;
import com.yoyo.launcher.Launcher;
import com.yoyo.launcher.R;
import com.yoyo.launcher.Utilities;
import com.yoyo.launcher.accessibility.DragViewStateAnnouncer;
import com.yoyo.launcher.dragndrop.DragOptions.PreDragCondition;
import com.yoyo.launcher.model.data.ItemInfo;
import com.yoyo.launcher.util.TouchUtil;
import com.yoyo.launcher.widget.util.WidgetDragScaleUtils;

/**
 * Drag controller for Launcher activity
 */
public class LauncherDragController extends DragController<Launcher> {

    public static final String TAG = "LauncherDragController";

    private static final boolean PROFILE_DRAWING_DURING_DRAG = false;
    private final FlingToDeleteHelper mFlingToDeleteHelper;

    /** Whether or not the drag operation is triggered by mouse right click. */
    private boolean mIsInMouseRightClick = false;

    public LauncherDragController(Launcher launcher) {
        super(launcher);
        mFlingToDeleteHelper = new FlingToDeleteHelper(launcher);
    }

    @Override
    protected DragView startDrag(
            @Nullable Drawable drawable,
            @Nullable View view,
            DraggableView originalView,
            int dragLayerX,
            int dragLayerY,
            DragSource source,
            ItemInfo dragInfo,
            Rect dragRegion,
            float initialDragViewScale,
            float dragViewScaleOnDrop,
            DragOptions options) {
        Log.d(TAG, "startDrag: dragInfo=" + dragInfo);
        if (PROFILE_DRAWING_DURING_DRAG) {
            Debug.startMethodTracing("Launcher");
        }

        if (removeAppsRefreshOnRightClick() && mIsInMouseRightClick
                && options.preDragCondition == null
                && originalView instanceof View v) {
            options.preDragCondition = new PreDragCondition() {

                @Override
                public boolean shouldStartDrag(double distanceDragged) {
                    return false;
                }

                @Override
                public void onPreDragStart(DragObject dragObject) {
                    // Set it to visible so the text of FolderIcon would not flash (avoid it from
                    // being invisible and then visible)
                    v.setVisibility(VISIBLE);
                }

                @Override
                public void onPreDragEnd(DragObject dragObject, boolean dragStarted) { }
            };
        }

        mActivity.hideKeyboard();
        AbstractFloatingView.closeOpenViews(mActivity, false, TYPE_DISCOVERY_BOUNCE);

        mOptions = options;
        if (mOptions.simulatedDndStartPoint != null) {
            mLastTouch.x = mMotionDown.x = mOptions.simulatedDndStartPoint.x;
            mLastTouch.y = mMotionDown.y = mOptions.simulatedDndStartPoint.y;
        }

        final int registrationX = mMotionDown.x - dragLayerX;
        final int registrationY = mMotionDown.y - dragLayerY;
        Log.d(TAG, "startDrag: mMotionDown=" + mMotionDown + " dragLayer=(" + dragLayerX + "," + dragLayerY + ") reg=(" + registrationX + "," + registrationY + ")");

        final int dragRegionLeft = dragRegion == null ? 0 : dragRegion.left;
        final int dragRegionTop = dragRegion == null ? 0 : dragRegion.top;

        mLastDropTarget = null;

        mDragObject = new DragObject(mActivity.getApplicationContext());
        mDragObject.originalView = originalView;

        mIsInPreDrag = mOptions.preDragCondition != null
                && !mOptions.preDragCondition.shouldStartDrag(0);

        final Resources res = mActivity.getResources();

        final float scalePx;
        if (originalView.getViewType() == DraggableView.DRAGGABLE_WIDGET) {
            scalePx = mIsInPreDrag ? 0f : getWidgetDragScalePx(drawable, view, dragInfo);
        } else {
            scalePx = mIsInPreDrag ? res.getDimensionPixelSize(R.dimen.pre_drag_view_scale) : 0f;
        }

        int width = drawable != null ? drawable.getIntrinsicWidth() : view.getMeasuredWidth();
        int height = drawable != null ? drawable.getIntrinsicHeight() : view.getMeasuredHeight();
        if (width <= 1 || height <= 1) {
            width = height = mActivity.getDeviceProfile().getWorkspaceIconProfile().getIconSizePx();
        }

        final View content;
        if (drawable != null) {
            ImageView iv = new ImageView(mActivity);
            iv.setImageDrawable(drawable);
            content = iv;
        } else {
            content = view;
        }

        final DragView dragView = mDragObject.dragView = new LauncherDragView(
                mActivity,
                content,
                width,
                height,
                registrationX,
                registrationY,
                initialDragViewScale,
                dragViewScaleOnDrop,
                scalePx);

        dragView.setItemInfo(dragInfo);
        mDragObject.dragComplete = false;

        mDragObject.xOffset = mMotionDown.x - (dragLayerX + dragRegionLeft);
        mDragObject.yOffset = mMotionDown.y - (dragLayerY + dragRegionTop);

        mDragDriver = DragDriver.create(this, mOptions, mFlingToDeleteHelper::recordMotionEvent);
        updateDescendantsAccessibility(dragView, /*accessible=*/ false);
        if (!mOptions.isAccessibleDrag) {
            mDragObject.stateAnnouncer = DragViewStateAnnouncer.createFor(dragView);
        }

        mDragObject.dragSource = source;
        mDragObject.dragInfo = dragInfo;
        mDragObject.originalDragInfo = mDragObject.dragInfo.makeShallowCopy();

        if (mOptions.preDragCondition != null) {
            dragView.setHasDragOffset(mOptions.preDragCondition.getDragOffset().x != 0 ||
                    mOptions.preDragCondition.getDragOffset().y != 0);
        }

        if (dragRegion != null) {
            dragView.setDragRegion(new Rect(dragRegion));
        }

        mActivity.getDragLayer().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        dragView.show(mLastTouch.x, mLastTouch.y);
        mDistanceSinceScroll = 0;

        if (!mIsInPreDrag) {
            callOnDragStart();
        } else if (mOptions.preDragCondition != null) {
            mOptions.preDragCondition.onPreDragStart(mDragObject);
        }

        handleMoveEvent(mLastTouch.x, mLastTouch.y);

        if (!isItemPinnable()) {
            Log.d(TAG, "startDrag: Item not pinnable, cancelling");
            MAIN_EXECUTOR.post(this::cancelDrag);
        } else if (!mIsInPreDrag && !mActivity.isTouchInProgress()
                && options.simulatedDndStartPoint == null) {
            Log.d(TAG, "startDrag: Internal drag and touch already complete, cancelling");
            // If it is an internal drag and the touch is already complete, cancel immediately
            MAIN_EXECUTOR.post(this::cancelDrag);
        }
        return dragView;
    }

    /**
     * During a drag, we don't want to expose the descendants of drag view to a11y users,
     * since those descendants are not a valid position in the workspace.
     * We need to go through the children because the view itself is important for
     * accessibility, basically we are implementing:
     * IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS when {@code accessible} is true and
     * reversing it when false.
     */
    void updateDescendantsAccessibility(DragView dragView, boolean accessible) {
        for (int i = 0; i < dragView.getChildCount(); i++) {
            dragView.getChildAt(i).setImportantForAccessibility(
                    accessible ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                            : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            );
        }
    }

    /**
     * Returns the scale in terms of pixels (to be applied on width) to scale the preview
     * during drag and drop.
     */
    public float getWidgetDragScalePx(@Nullable Drawable drawable, @Nullable View view,
            ItemInfo dragInfo) {
        float draggedViewWidthPx = 0;
        float draggedViewHeightPx = 0;

        if (view != null) {
            draggedViewWidthPx = view.getMeasuredWidth();
            draggedViewHeightPx = view.getMeasuredHeight();
        } else if (drawable != null) {
            draggedViewWidthPx = drawable.getIntrinsicWidth();
            draggedViewHeightPx = drawable.getIntrinsicHeight();
        }

        return WidgetDragScaleUtils.getWidgetDragScalePx(mActivity, mActivity.getDeviceProfile(),
                draggedViewWidthPx, draggedViewHeightPx, dragInfo);
    }

    @Override
    public String dump() {
        return TAG;
    }

    @Override
    protected void exitDrag() {
        if (!mIsInPreDrag && !mActivity.isInState(EDIT_MODE)) {
            mActivity.getStateManager().goToState(NORMAL, SPRING_LOADED_EXIT_DELAY);
        }
    }

    @Override
    protected boolean endWithFlingAnimation() {
        if (mDragObject != null && mDragObject.dragView != null) {
            updateDescendantsAccessibility(mDragObject.dragView, /*accessible=*/ true);
        }
        Runnable flingAnimation = mFlingToDeleteHelper.getFlingAnimation(mDragObject, mOptions);
        if (flingAnimation != null) {
            drop(mFlingToDeleteHelper.getDropTarget(), flingAnimation);
            return true;
        }
        return super.endWithFlingAnimation();
    }

    @Override
    protected void endDrag() {
        if (mDragObject != null && mDragObject.dragView != null) {
            updateDescendantsAccessibility(mDragObject.dragView, /*accessible=*/ true);
        }
        super.endDrag();
        mFlingToDeleteHelper.releaseVelocityTracker();
    }

    @Override
    protected DropTarget getDefaultDropTarget(int[] dropCoordinates) {
        mActivity.getDragLayer().mapCoordInSelfToDescendant(mActivity.getWorkspace(),
                dropCoordinates);
        return mActivity.getWorkspace();
    }

    /**
     * Intercepts touch events from a drag source view.
     */
    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        mIsInMouseRightClick = TouchUtil.isMouseRightClickDownOrMove(ev);
        if (!Utilities.isWorkspaceEditAllowed(mActivity.getDragLayer().getContext())) {
            cancelDrag();
            return false;
        }
        return super.onControllerInterceptTouchEvent(ev);
    }
}
