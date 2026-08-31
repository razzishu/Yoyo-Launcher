package com.yoyo.launcher.icons

import android.graphics.Path

/**
 * Manages custom vector paths for specific apps to ensure high-quality themed icons.
 */
object CustomLogoManager {
    
    fun getCustomPath(packageName: String): Path? {
        // Catch various Telegram clients
        if (packageName.contains("telegram") || packageName.contains("nekomimi") || packageName.contains("nekogram") || packageName.contains("challegram")) {
            return createTelegramPath()
        }
        
        return when (packageName) {
            "com.rapido.passenger" -> createRapidoPath() // Rapido Bike
            else -> null
        }
    }

    private fun createTelegramPath(): Path {
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        
        // Modern minimalist origami paper plane
        path.moveTo(85f, 15f) // Nose
        path.lineTo(15f, 45f) // Left tip
        path.lineTo(45f, 55f) // Inner center
        path.lineTo(55f, 85f) // Bottom tip
        path.close()
        
        // Geometric cutout to represent the inner fold / shadow
        path.moveTo(80f, 20f) 
        path.lineTo(47f, 55f) 
        path.lineTo(53f, 75f) 
        path.lineTo(62f, 40f) 
        path.close()
        
        // Scale it down to make it smaller as requested
        val matrix = android.graphics.Matrix()
        matrix.postScale(0.75f, 0.75f, 50f, 50f)
        path.transform(matrix)
        
        return path
    }

    private fun createRapidoPath(): Path {
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        
        // Modern minimalist scooter / bike silhouette
        
        // Rear wheel with hole
        path.addCircle(30f, 70f, 14f, Path.Direction.CW)
        path.addCircle(30f, 70f, 6f, Path.Direction.CW) 
        
        // Front wheel with hole
        path.addCircle(70f, 70f, 14f, Path.Direction.CW)
        path.addCircle(70f, 70f, 6f, Path.Direction.CW) 
        
        // Chassis (Base board)
        path.addRoundRect(30f, 66f, 65f, 74f, 4f, 4f, Path.Direction.CW)
        
        // Front steering column
        path.moveTo(64f, 35f)
        path.lineTo(70f, 35f)
        path.lineTo(75f, 70f)
        path.lineTo(69f, 70f)
        path.close()
        
        // Handlebars and grip
        path.addRoundRect(60f, 30f, 75f, 38f, 4f, 4f, Path.Direction.CW)
        path.addCircle(60f, 34f, 4f, Path.Direction.CW)
        
        // Seat column
        path.moveTo(40f, 46f)
        path.lineTo(46f, 46f)
        path.lineTo(40f, 66f)
        path.lineTo(34f, 66f)
        path.close()
        
        // Seat
        path.addRoundRect(28f, 42f, 50f, 50f, 4f, 4f, Path.Direction.CW)
        
        return path
    }
}
