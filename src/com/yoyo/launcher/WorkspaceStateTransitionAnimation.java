/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.yoyo.launcher;

import static androidx.dynamicanimation.animation.DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE;

import static com.android.app.animation.Interpolators.ACCELERATE_2;
import static com.android.app.animation.Interpolators.EMPHASIZED_DECELERATE;
import static com.android.app.animation.Interpolators.LINEAR;
import static com.android.app.animation.Interpolators.ZOOM_OUT;
import static com.yoyo.launcher.LauncherAnimUtils.HOTSEAT_SCALE_PROPERTY_FACTORY;
import static com.yoyo.launcher.LauncherAnimUtils.SCALE_INDEX_WORKSPACE_STATE;
import static com.yoyo.launcher.LauncherAnimUtils.VIEW_ALPHA;
import static com.yoyo.launcher.LauncherAnimUtils.VIEW_TRANSLATE_X;
import static com.yoyo.launcher.LauncherAnimUtils.VIEW_TRANSLATE_Y;
import static com.yoyo.launcher.LauncherAnimUtils.WORKSPACE_SCALE_PROPERTY_FACTORY;
import static com.yoyo.launcher.LauncherState.FLAG_HAS_SYS_UI_SCRIM;
import static com.yoyo.launcher.LauncherState.FLAG_HOTSEAT_INACCESSIBLE;
import static com.yoyo.launcher.LauncherState.HINT_STATE;
import static com.yoyo.launcher.LauncherState.HOTSEAT_ICONS;
import static com.yoyo.launcher.LauncherState.NORMAL;
import static com.yoyo.launcher.LauncherState.WORKSPACE_PAGE_INDICATOR;
import static com.yoyo.launcher.anim.PropertySetter.NO_ANIM_PROPERTY_SETTER;
import static com.yoyo.launcher.config.FeatureFlags.FOLDABLE_SINGLE_PAGE;
import static com.yoyo.launcher.graphics.Scrim.SCRIM_PROGRESS;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_HOTSEAT_FADE;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_HOTSEAT_SCALE;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_HOTSEAT_TRANSLATE;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_SCRIM_FADE;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_WORKSPACE_FADE;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_WORKSPACE_PAGE_TRANSLATE_X;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_WORKSPACE_SCALE;
import static com.yoyo.launcher.states.StateAnimationConfig.ANIM_WORKSPACE_TRANSLATE;
import static com.yoyo.launcher.states.StateAnimationConfig.SKIP_SCRIM;

import android.animation.ValueAnimator;
import android.util.FloatProperty;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import com.yoyo.launcher.LauncherState.PageAlphaProvider;
import com.yoyo.launcher.LauncherState.PageTranslationProvider;
import com.yoyo.launcher.LauncherState.ScaleAndTranslation;
import com.yoyo.launcher.anim.AnimatedFloat;
import com.yoyo.launcher.anim.PendingAnimation;
import com.yoyo.launcher.anim.PropertySetter;
import com.yoyo.launcher.anim.SpringAnimationBuilder;
import com.yoyo.launcher.graphics.Scrim;
import com.yoyo.launcher.graphics.SysUiScrim;
import com.yoyo.launcher.states.EditModeState;
import com.yoyo.launcher.states.SpringLoadedState;
import com.yoyo.launcher.states.StateAnimationConfig;
import com.yoyo.launcher.util.DynamicResource;
import com.android.systemui.plugins.ResourceProvider;

/**
 * Manages the animations between each of the workspace states.
 */
public class WorkspaceStateTransitionAnimation {

    private static final float FIRST_PAGE_PINNED_WIDGET_DISABLED_ALPHA = 0.3f;

    private static final FloatProperty<Workspace<?>> WORKSPACE_SCALE_PROPERTY =
            WORKSPACE_SCALE_PROPERTY_FACTORY.get(SCALE_INDEX_WORKSPACE_STATE);

    private static final FloatProperty<Hotseat> HOTSEAT_SCALE_PROPERTY =
            HOTSEAT_SCALE_PROPERTY_FACTORY.get(SCALE_INDEX_WORKSPACE_STATE);

    private final Launcher mLauncher;
    private final Workspace<?> mWorkspace;

    private float mNewScale;

    public WorkspaceStateTransitionAnimation(Launcher launcher, Workspace<?> workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;
    }

    public void setState(LauncherState toState) {
        setWorkspaceProperty(toState, NO_ANIM_PROPERTY_SETTER, new StateAnimationConfig());
    }

    /**
     * @see com.yoyo.launcher.statemanager.StateManager.StateHandler#setStateWithAnimation
     */
    public void setStateWithAnimation(
            LauncherState toState, StateAnimationConfig config, PendingAnimation animation) {
        setWorkspaceProperty(toState, animation, config);
    }

    public float getFinalScale() {
        return mNewScale;
    }

    /**
     * Starts a transition animation for the workspace.
     */
    private void setWorkspaceProperty(LauncherState state, PropertySetter propertySetter,
            StateAnimationConfig config) {
        ScaleAndTranslation scaleAndTranslation = state.getWorkspaceScaleAndTranslation(mLauncher);
        ScaleAndTranslation hotseatScaleAndTranslation = state.getHotseatScaleAndTranslation(
                mLauncher);
        mNewScale = scaleAndTranslation.scale;
        PageAlphaProvider pageAlphaProvider = state.getWorkspacePageAlphaProvider(mLauncher);
        final int childCount = mWorkspace.getChildCount();
        for (int i = 0; i < childCount; i++) {
            applyChildState(state, (CellLayout) mWorkspace.getChildAt(i), i, pageAlphaProvider,
                    propertySetter, config);
        }

        int elements = state.getVisibleElements(mLauncher.getLauncherUiState());
        Hotseat hotseat = mWorkspace.getHotseat();
        // Use EMPHASIZED_DECELERATE (Material You) instead of ZOOM_OUT — front-loads movement
        // so icons reach final position quickly, eliminating the "settling too late" effect.
        Interpolator scaleInterpolator = config.getInterpolator(
                ANIM_WORKSPACE_SCALE, EMPHASIZED_DECELERATE);
        LauncherState fromState = mLauncher.getStateManager().getState();

        boolean shouldSpring = propertySetter instanceof PendingAnimation
                && fromState == HINT_STATE && state == NORMAL;
        if (shouldSpring) {
            ((PendingAnimation) propertySetter).add(getSpringScaleAnimator(mLauncher,
                    mWorkspace, mNewScale, WORKSPACE_SCALE_PROPERTY));
        } else {
            propertySetter.setFloat(mWorkspace, WORKSPACE_SCALE_PROPERTY, mNewScale,
                    scaleInterpolator);
        }

        mWorkspace.setPivotToScaleWithSelf(hotseat);
        float hotseatScale = hotseatScaleAndTranslation.scale;
        if (shouldSpring) {
            PendingAnimation pa = (PendingAnimation) propertySetter;
            pa.add(getSpringScaleAnimator(mLauncher, hotseat, hotseatScale,
                    HOTSEAT_SCALE_PROPERTY));
        } else {
            Interpolator hotseatScaleInterpolator = config.getInterpolator(ANIM_HOTSEAT_SCALE,
                    scaleInterpolator);
            propertySetter.setFloat(hotseat, HOTSEAT_SCALE_PROPERTY, hotseatScale,
                    hotseatScaleInterpolator);
        }

        Interpolator workspaceFadeInterpolator = config.getInterpolator(ANIM_WORKSPACE_FADE,
                pageAlphaProvider.interpolator);
        float workspacePageIndicatorAlpha = (elements & WORKSPACE_PAGE_INDICATOR) != 0 ? 1 : 0;
        propertySetter.setViewAlpha(mLauncher.getWorkspace().getPageIndicator(),
                workspacePageIndicatorAlpha, workspaceFadeInterpolator);
        Interpolator hotseatFadeInterpolator = config.getInterpolator(ANIM_HOTSEAT_FADE,
                workspaceFadeInterpolator);
        float hotseatIconsAlpha = (elements & HOTSEAT_ICONS) != 0 ? 1 : 0;
        propertySetter.setViewAlpha(hotseat, hotseatIconsAlpha, hotseatFadeInterpolator);

        // Update the accessibility flags for hotseat based on launcher state.
        hotseat.setImportantForAccessibility(
                state.hasFlag(FLAG_HOTSEAT_INACCESSIBLE)
                        ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        hotseat.setDescendantFocusability(state.hasFlag(FLAG_HOTSEAT_INACCESSIBLE)
                ? ViewGroup.FOCUS_BLOCK_DESCENDANTS : ViewGroup.FOCUS_BEFORE_DESCENDANTS);

        Interpolator translationInterpolator =
                config.getInterpolator(ANIM_WORKSPACE_TRANSLATE, EMPHASIZED_DECELERATE);
        propertySetter.setFloat(mWorkspace, VIEW_TRANSLATE_X,
                scaleAndTranslation.translationX, translationInterpolator);
        propertySetter.setFloat(mWorkspace, VIEW_TRANSLATE_Y,
                scaleAndTranslation.translationY, translationInterpolator);
        PageTranslationProvider pageTranslationProvider = state.getWorkspacePageTranslationProvider(
                mLauncher);

        if (!FOLDABLE_SINGLE_PAGE.get()) {
            for (int i = 0; i < childCount; i++) {
                applyPageTranslation((CellLayout) mWorkspace.getChildAt(i), i,
                        pageTranslationProvider,
                        propertySetter, config);
            }
        }

        Interpolator hotseatTranslationInterpolator = config.getInterpolator(
                ANIM_HOTSEAT_TRANSLATE, translationInterpolator);
        propertySetter.setFloat(hotseat, VIEW_TRANSLATE_Y,
                hotseatScaleAndTranslation.translationY, hotseatTranslationInterpolator);
        propertySetter.setFloat(mWorkspace.getPageIndicator(), VIEW_TRANSLATE_Y,
                hotseatScaleAndTranslation.translationY, hotseatTranslationInterpolator);

        if (!config.hasAnimationFlag(SKIP_SCRIM)) {
            setScrim(propertySetter, state, config);
        }
    }

    public void setScrim(PropertySetter propertySetter, LauncherState state,
            StateAnimationConfig config) {
        Scrim workspaceDragScrim = mLauncher.getDragLayer().getWorkspaceDragScrim();
        propertySetter.setFloat(workspaceDragScrim, SCRIM_PROGRESS,
                state.getWorkspaceBackgroundAlpha(mLauncher), LINEAR);

        SysUiScrim sysUiScrim = mLauncher.getRootView().getSysUiScrim();
        propertySetter.setFloat(sysUiScrim.getSysUIProgress(), AnimatedFloat.VALUE,
                state.hasFlag(FLAG_HAS_SYS_UI_SCRIM) ? 1 : 0, LINEAR);

        propertySetter.setScrimColors(mLauncher.getScrimView(),
                state.getWorkspaceScrimColor(mLauncher),
                config.getInterpolator(ANIM_SCRIM_FADE, ACCELERATE_2));
    }

    public void applyChildState(LauncherState state, CellLayout cl, int childIndex) {
        applyChildState(state, cl, childIndex, state.getWorkspacePageAlphaProvider(mLauncher),
                NO_ANIM_PROPERTY_SETTER, new StateAnimationConfig());
    }

    private void applyChildState(LauncherState state, CellLayout cl, int childIndex,
            PageAlphaProvider pageAlphaProvider, PropertySetter propertySetter,
            StateAnimationConfig config) {
        float pageAlpha = pageAlphaProvider.getPageAlpha(childIndex);
        float springLoadedProgress =
                (state instanceof  SpringLoadedState || state instanceof EditModeState) ? 1f : 0f;
        propertySetter.setFloat(cl,
                CellLayout.SPRING_LOADED_PROGRESS, springLoadedProgress, ZOOM_OUT);
        Interpolator fadeInterpolator = config.getInterpolator(ANIM_WORKSPACE_FADE,
                pageAlphaProvider.interpolator);
        propertySetter.setFloat(cl.getShortcutsAndWidgets(), VIEW_ALPHA,
                pageAlpha, fadeInterpolator);
    }

    private void applyPageTranslation(CellLayout cellLayout, int childIndex,
            PageTranslationProvider pageTranslationProvider, PropertySetter propertySetter,
            StateAnimationConfig config) {
        float pageTranslation = pageTranslationProvider.getPageTranslation(childIndex);
        Interpolator translationInterpolator = config.getInterpolator(
                ANIM_WORKSPACE_PAGE_TRANSLATE_X, pageTranslationProvider.interpolator);
        propertySetter.setFloat(cellLayout, VIEW_TRANSLATE_X, pageTranslation,
                translationInterpolator);
    }

    /**
     * Returns a spring based animator for the scale property of {@param workspace}.
     */
    public static ValueAnimator getWorkspaceSpringScaleAnimator(Launcher launcher,
            Workspace<?> workspace, float scale) {
        return getSpringScaleAnimator(launcher, workspace, scale, WORKSPACE_SCALE_PROPERTY);
    }

    /**
     * Returns a spring based animator for the scale property of {@param v}.
     */
    public static <T extends View> ValueAnimator getSpringScaleAnimator(Launcher launcher, T v,
            float scale, FloatProperty<T> property) {
        ResourceProvider rp = DynamicResource.provider(launcher);
        float damping = rp.getFloat(R.dimen.hint_scale_damping_ratio);
        float stiffness = rp.getFloat(R.dimen.hint_scale_stiffness);
        float velocityPxPerS = rp.getDimension(R.dimen.hint_scale_velocity_dp_per_s);

        return new SpringAnimationBuilder(v.getContext())
                .setStiffness(stiffness)
                .setDampingRatio(damping)
                .setMinimumVisibleChange(MIN_VISIBLE_CHANGE_SCALE)
                .setEndValue(scale)
                .setStartValue(property.get(v))
                .setStartVelocity(velocityPxPerS)
                .build(v, property);

    }
}