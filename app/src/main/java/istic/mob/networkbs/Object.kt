package istic.mob.networkbs

import android.graphics.RectF

class Object (
    var label: String,
    var x: Float,
    var y: Float
) {
    val id: Int = NEXT_ID++
    var bounds: RectF = RectF(x - R, y - R, x + R, y + R)

    fun moveTo(newX: Float, newY: Float, viewWidth: Int, viewHeight: Int, topMargin: Float = 0F) {
        val clampedX = newX.coerceIn(R, viewWidth - R)
        val clampedY = newY.coerceIn(topMargin + R, viewHeight - R)

        x = clampedX
        y = clampedY
        bounds.offsetTo(x - R, y - R)
    }

    companion object {
        private var NEXT_ID = 1
        private const val R = 60f
    }
}