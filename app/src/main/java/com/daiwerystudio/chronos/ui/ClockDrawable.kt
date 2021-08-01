package com.daiwerystudio.chronos

import android.graphics.*
import android.graphics.drawable.Drawable
import java.util.*


data class ActionDrawable(
    val color: Int,
    var start: Float,
    var end: Float
)


class ClockDrawable(var actions: List<ActionDrawable>) : Drawable() {
    private val paint: Paint = Paint()

    override fun draw(canvas: Canvas) {
        val width: Float = bounds.width().toFloat()
        val height: Float = bounds.height().toFloat()

        actions.forEachIndexed { i, action ->
            paint.color = action.color
            canvas.drawRect(0f, height*action.start,
                width, height*action.end, paint)
        }

    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE
}