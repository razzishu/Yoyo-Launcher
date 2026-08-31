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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.yoyo.launcher.R;

public class AnimatedBackgroundView extends View {
    private final Paint mPaint;
    private float mOffset = 0;

    public AnimatedBackgroundView(Context context) {
        this(context, null);
    }

    public AnimatedBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(context.getColor(R.color.materialColorPrimaryContainer));
        mPaint.setAlpha(40);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();

        mOffset += 0.5f;
        if (mOffset > width * 10) mOffset = 0;

        // Draw some simple animated waves/circles
        canvas.drawCircle(width / 2f + (float) Math.sin(mOffset * 0.02) * 100,
                height / 2f + (float) Math.cos(mOffset * 0.02) * 100,
                300f + (float) Math.sin(mOffset * 0.01) * 50, mPaint);
        
        canvas.drawCircle(width / 4f + (float) Math.cos(mOffset * 0.03) * 50,
                height / 4f + (float) Math.sin(mOffset * 0.03) * 50,
                200f, mPaint);

        invalidate();
    }
}
