package com.yoyo.launcher.icons;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

/**
 * Implementation of DotRenderer for drawing notification dots.
 */
public class DotRenderer {

    private final float mCircleRadius;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public static class IconShapeInfo {
        public float normalizationScale = 1f;

        public static IconShapeInfo fromPath(Path path, int size) {
            IconShapeInfo info = new IconShapeInfo();
            // Simplified for standalone port
            info.normalizationScale = 1f;
            return info;
        }

        public static final IconShapeInfo DEFAULT = new IconShapeInfo();
    }

    public static class DrawParams {
        public IconShapeInfo shapeInfo = IconShapeInfo.DEFAULT;
        public int dotColor = Color.RED;
        public Rect iconBounds = new Rect();
        public float dotScale = 0;
        public float scale = 0;
        public Rect visibleBounds = new Rect();

        public void setDotColor(int color) {
            dotColor = color;
        }
    }

    public DotRenderer(int size, PathRenderer shape, int count) {
        this(size);
    }

    public DotRenderer(int size) {
        // Dot size is typically 1/4 of icon size
        mCircleRadius = size * 0.125f; 
    }

    public float getDotRadius() {
        return mCircleRadius;
    }

    public void draw(Canvas canvas, Paint paint, Rect iconBounds, float dotScale, Rect visibleBounds) {
        if (dotScale <= 0) return;

        float radius = mCircleRadius * dotScale;
        // Draw in top right corner
        float cx = iconBounds.right - mCircleRadius;
        float cy = iconBounds.top + mCircleRadius;

        canvas.drawCircle(cx, cy, radius, paint);
    }

    public void draw(Canvas canvas, DrawParams params) {
        if (params == null || params.scale <= 0) return;

        mPaint.setColor(params.dotColor);
        mPaint.setAlpha(255);

        float radius = mCircleRadius * params.scale;
        // Position relative to icon bounds
        float cx = params.iconBounds.right - mCircleRadius;
        float cy = params.iconBounds.top + mCircleRadius;

        canvas.drawCircle(cx, cy, radius, mPaint);
    }
}
