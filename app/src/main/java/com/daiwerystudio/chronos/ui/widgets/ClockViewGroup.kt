/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 29.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: рефакторинг.
*
* Дата изменения: 04.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена визуализация времени для целей и напоминаний.
*
* Дата изменения: 24.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: удалена вся логика обработки данных из View и перенесена в ViewModel. Поэтому
* теперь всего одно View для визуализации действий.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView
import androidx.core.content.res.ResourcesCompat
import com.daiwerystudio.chronos.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*  В данном файле описаны виджеты для визуализации действий.  */

/**
 * Показывает действия на временной шкале.
 */
class ActionsView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var stripWidth: Float
    private var spaceWidth: Float
    private var corner: Float
    private var colorColumn: Int
    private var mCount: Int = 1
    private val mPaint: Paint = Paint()
    private var mActionDrawables: List<ActionDrawable> = emptyList()

    /**
     * Хранит информацию о том, где и как нужно рисовать.
     */
    data class ActionDrawable(
        val color: Int,
        var start: Float,
        var end: Float,
        val left: Float,
        val right: Float,
    )


    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ActionsView,
            0, 0).apply {
            try {
                stripWidth = getDimensionPixelSize(R.styleable.ActionsView_stripWidth, 0).toFloat()
                spaceWidth = getDimensionPixelSize(R.styleable.ActionsView_spaceWidth , 0).toFloat()
                corner = getDimensionPixelSize(R.styleable.ActionsView_corner , 0).toFloat()
                colorColumn = getColor(R.styleable.ActionsView_colorColumn , 0)
            } finally {
                recycle()
            }
        }

        // Сглаживание.
        mPaint.isAntiAlias = true
    }


    fun setActionDrawables(actionDrawables: List<ActionDrawable>){
        mActionDrawables = actionDrawables
        mCount = if (actionDrawables.isNotEmpty())
            actionDrawables.map { it.right }.maxOf { it }.toInt()
        else 1
        requestLayout()
    }

    /**
     * Изменяет только ширину виджета в зависимости от mCount.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        val desiredWidth = (stripWidth*mCount+spaceWidth*(mCount-1)).toInt()

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> requestedWidth
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(requestedWidth)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> requestedHeight
            MeasureSpec.AT_MOST -> requestedHeight
            else -> requestedHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val height = height.toFloat()

        mPaint.color = colorColumn
        canvas?.drawRoundRect(0f, 0f,
            (stripWidth+spaceWidth)*mCount-spaceWidth, height, corner, corner, mPaint)

        mActionDrawables.forEach {
            mPaint.color = it.color
            canvas?.drawRoundRect((stripWidth+spaceWidth)*it.left, height*it.start,
                (stripWidth+spaceWidth)*it.right-spaceWidth, height*it.end, corner, corner, mPaint)
        }
    }
}

/**
 * Вспомогательный виджет. Показывает циферблат. Имеет один изменяемый парамент: время
 * в начале циферблата. Показываем 24 часа.
 */
class ClockFaceView(context: Context, attrs: AttributeSet): View(context, attrs){
    private var textSize: Float
    private var textColor: Int
    private var spaceHeight: Int
    private var marginText: Int
    private var fontFamilyID: Int
    private var mPaint: Paint = Paint()
    private var mTextHours: MutableList<TextHour> = mutableListOf()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ClockFaceView,
            0, 0).apply {
            try {
                textSize = getDimensionPixelSize(R.styleable.ClockFaceView_textSize, 0).toFloat()
                textColor = getColor(R.styleable.ClockFaceView_android_textColor, Color.BLACK)
                spaceHeight = getDimensionPixelSize(R.styleable.ClockFaceView_spaceHeight, 0)
                marginText = getDimensionPixelSize(R.styleable.ClockFaceView_marginText, 0)
                fontFamilyID = getResourceId(R.styleable.ClockFaceView_android_fontFamily, 0)
            } finally {
                recycle()
            }
        }

        mPaint.color = textColor
        mPaint.textSize = textSize
        mPaint.isAntiAlias = true
        mPaint.alpha = 255/2
        mPaint.typeface = ResourcesCompat.getFont(getContext(), fontFamilyID)

        for (i in 0..23){
            val textHour = TextHour(LocalTime.ofSecondOfDay(i*3600L)
                    .format(DateTimeFormatter.ofPattern("HH:mm")), i/24f)
            mPaint.getTextBounds(textHour.text, 0, textHour.text.length, textHour.bounds)
            mTextHours.add(textHour)
        }
    }

    private data class TextHour(
        var text: String,
        var y: Float,
        var bounds: Rect = Rect()
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        val desiredWidth = (mPaint.measureText(mTextHours.last().text)+marginText*2).toInt()
        val desiredHeight = ((textSize+spaceHeight+textSize)*24).toInt()

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> requestedWidth
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(requestedWidth)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> requestedHeight
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(requestedHeight)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val height = height.toFloat()
        val width = width.toFloat()

        mTextHours.forEachIndexed { index, textHour ->
            if (index != 0) {
                canvas?.drawText(textHour.text, (width-textHour.bounds.width())/2,
                    height*textHour.y+textHour.bounds.height()/2, mPaint)
                canvas?.drawLine(0f, height*textHour.y,
                    (width-textHour.bounds.width()-marginText)/2 , height*textHour.y, mPaint)
                canvas?.drawLine((width+textHour.bounds.width()+marginText)/2, height*textHour.y,
                    width , height*textHour.y, mPaint)
            } else canvas?.drawText(textHour.text, (width-textHour.bounds.width())/2,
                height*textHour.y+textHour.bounds.height(), mPaint)

            val position = textHour.y+1/48f
            canvas?.drawLine(3*width/8f, height*position, 5*width/8f, height*position, mPaint)
        }
    }
}

/**
 * ViewGroup. Соединение виджетов выше с ScrollView.
 * Высота определяется ClockFaceView.
 */
class ActionsClockView(context: Context, attrs: AttributeSet): ScrollView(context, attrs) {
    private val actionsView: ActionsView
    private val clockFaceView: ClockFaceView


    init {
        inflate(context, R.layout.layout_actions_clock, this)

        actionsView = findViewById(R.id.actionsView)
        clockFaceView = findViewById(R.id.clockFaceView)
    }

    /*  Установка данных для ActionsView.  */
    fun setActionDrawables(actionDrawables: List<ActionsView.ActionDrawable>){
        actionsView.setActionDrawables(actionDrawables)
    }
}

