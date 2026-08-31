package com.yoyo.launcher.icons;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;

/**
 * Ported/Stubbed BitmapRenderer for standalone build.
 * Forcing software bitmaps to ensure visibility in standalone environments.
 */
public interface BitmapRenderer {

    boolean USE_HARDWARE_BITMAP = false; // Force software for stability

    void draw(Canvas out);

    static Bitmap createSoftwareBitmap(int width, int height, BitmapRenderer renderer) {
        if (width <= 0 || height <= 0) {
            android.util.Log.e("BitmapRenderer", "Invalid size for software bitmap: " + width + "x" + height);
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        renderer.draw(new Canvas(bitmap));
        return bitmap;
    }

    static Bitmap createHardwareBitmap(int width, int height, BitmapRenderer renderer) {
        return createSoftwareBitmap(width, height, renderer);
    }

    static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        return Bitmap.createBitmap(source, x, y, width, height);
    }
}
