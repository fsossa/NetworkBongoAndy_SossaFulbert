// fr.istic.mob.networkbs
package istic.mob.networkbs

import android.graphics.RectF

data class Node(
    var label: String,
    var x: Float,
    var y: Float,
    var id: Int = 0
) {
    var bounds: RectF = RectF(x - R, y - R, x + R, y + R)

    fun moveTo(newX: Float, newY: Float, viewWidth: Int, viewHeight: Int, topMargin: Float = 0f, offsetX: Float = 0f, offsetY: Float = 0f, scale: Float = 1f) {
        // Convertir coordonnées écran -> coordonnées locales (inverse transform)
        val localX = (newX - offsetX) / scale
        val localY = (newY - offsetY) / scale

        val clampedX = localX.coerceIn(R, viewWidth / scale - R)
        val clampedY = localY.coerceIn((topMargin / scale) + R, viewHeight / scale - R)

        x = clampedX
        y = clampedY
        bounds.offsetTo(x - R, y - R)
    }

    companion object {
        private var NEXT_ID = 1
        const val R = 60f

        fun nextId() = NEXT_ID++
    }
}
