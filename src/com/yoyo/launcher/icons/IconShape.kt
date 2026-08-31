package com.yoyo.launcher.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * Ported/Stubbed IconShape for standalone build.
 * Supports a global dynamic mask path.
 */
data class IconShape(
    @JvmField val pathSize: Int,
    @JvmField var path: Path,
    @JvmField var shadowLayer: Bitmap
) {
    val shapeRenderer = object : PathRenderer {
        override fun render(canvas: Canvas, paint: Paint) {
            canvas.drawPath(path, paint)
        }
    }

    companion object {
        const val DEFAULT_PATH_SIZE = 100
        
        @JvmField
        val EMPTY: IconShape = createCircle(DEFAULT_PATH_SIZE)

        fun createCircle(size: Int): IconShape {
            val path = Path()
            val center = size / 2f
            path.addCircle(center, center, center, Path.Direction.CW)

            return IconShape(
                size,
                path,
                Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
            )
        }

        /** Updates the global EMPTY shape with a new path and shadow. */
        fun updateGlobalShape(newPath: Path) {
            EMPTY.path = newPath
            // Clear shadow for new shape
            EMPTY.shadowLayer = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        }
    }
}
