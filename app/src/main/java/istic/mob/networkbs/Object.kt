package istic.mob.networkbs

import android.graphics.RectF

class Object (
    var label: String,
    var x: Float,
    var y: Float
) {
    val id: Int = NEXT_ID++
    var bounds: RectF = RectF(x - R, y - R, x + R, y + R)

    fun moveTo(newX: Float, newY: Float) {
        x = newX
        y = newY
        bounds.offsetTo(newX - R, newY - R)
    }

    companion object {
        private var NEXT_ID = 1
        private const val R = 60f
    }
}