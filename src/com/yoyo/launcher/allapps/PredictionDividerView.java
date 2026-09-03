/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.yoyo.launcher.allapps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yoyo.launcher.R;
import com.yoyo.launcher.util.Themes;

/**
 * A centered divider line for the all-apps floating header.
 */
public class PredictionDividerView extends View implements FloatingHeaderRow {

    private final Paint mPaint = new Paint();
    private FloatingHeaderView mParent;

    public PredictionDividerView(@NonNull Context context) {
        this(context, null);
    }

    public PredictionDividerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(Themes.getAttrColor(context, R.attr.privateSpaceDividerColor));
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(getResources().getDimension(R.dimen.all_apps_divider_height));
    }

    @Override
    public void setup(FloatingHeaderView parent, FloatingHeaderRow[] allRows, boolean tabsHidden) {
        mParent = parent;
        updateVisibility();
    }

    @Override
    public int getExpectedHeight() {
        return 0;
    }

    @Override
    public boolean shouldDraw() {
        return false;
    }

    @Override
    public boolean hasVisibleContent() {
        return shouldDraw();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (shouldDraw()) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getExpectedHeight());
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    @Override
    public void setVerticalScroll(int scroll, boolean isScrolledOut) {
        setTranslationY(scroll);
        setAlpha(isScrolledOut ? 0 : 1);
    }

    @Override
    public Class<? extends FloatingHeaderRow> getTypeClass() {
        return PredictionDividerView.class;
    }

    @Override
    public View getFocusedChild() {
        return null;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (shouldDraw()) {
            int width = getWidth();
            int height = getHeight();
            int lineY = height / 2;

            float lineWidth = getResources().getDimension(R.dimen.all_apps_divider_width);
            float startX = (width - lineWidth) / 2f;
            float endX = startX + lineWidth;

            canvas.drawLine(startX, lineY, endX, lineY, mPaint);
        }
    }

    @Override
    public boolean isVisible() {
        return shouldDraw();
    }

    private void updateVisibility() {
        setVisibility(shouldDraw() ? VISIBLE : GONE);
    }
}
