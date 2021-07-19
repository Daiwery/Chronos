package com.daiwerystudio.chronos

import android.graphics.*
import android.graphics.drawable.Drawable



class lineItemListActionType(private val colors: List<Int>): Drawable() {
    private val paint: Paint = Paint()
    private val size: Int = colors.size

    override fun draw(canvas: Canvas) {
        val width: Float = bounds.width().toFloat()
        val height: Float = bounds.height().toFloat()

        colors.forEachIndexed { i, color ->
            paint.setShader(LinearGradient(0f, 0f, 0f, height,
                intArrayOf(color, Color.rgb(255, 255, 255)),
                floatArrayOf(0f, 0.7f), Shader.TileMode.MIRROR))
            canvas.drawRect(width/size*i, 0f, width/size*(i+1), height, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE
}