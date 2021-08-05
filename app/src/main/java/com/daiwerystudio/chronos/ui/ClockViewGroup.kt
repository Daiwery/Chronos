package com.daiwerystudio.chronos.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.ActionTypeRepository
import java.util.concurrent.Executors


class TimeView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var stripWidth: Float
    private var spaceWidth: Float
    private var corner: Float
    private var colorColumn: Int
    private val mRepository = ActionTypeRepository.get()
    private var mSections: List<Section> = emptyList()
    private var mActionDrawables: List<ActionDrawable> = emptyList()
    private var mCount: Int = 1
    private val mPaint: Paint = Paint()
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private var mCorrupted: List<String> = emptyList()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.TimeView,
            0, 0).apply {
            try {
                stripWidth = getDimensionPixelSize(R.styleable.TimeView_stripWidth, 0).toFloat()
                spaceWidth = getDimensionPixelSize(R.styleable.TimeView_spaceWidth , 0).toFloat()
                corner = getDimensionPixelSize(R.styleable.TimeView_corner , 0).toFloat()
                colorColumn = getColor(R.styleable.TimeView_colorColumn , 0)
            } finally {
                recycle()
            }
        }

        mPaint.isAntiAlias = true
    }

    /*  Вызывется, когда заканчивается обработка данных  */
    private var mFinishedListener: FinishedListener? = null
    interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    /*  Вызывается, когда добавляется или удаляется corrupt action schedule  */
    private var mCorruptedListener: CorruptedListener? = null
    interface CorruptedListener{
        fun addCorrupt(id: String)
        fun deleteCorrupt(id: String)
    }
    fun setCorruptedListener(corruptedListener: CorruptedListener){
        mCorruptedListener = corruptedListener
    }


    /*  Вспомогательные классы  */
    private data class Point(
        val id: String,
        val isStart: Boolean,
        val coordinate: Float,
        val color: Int
    )
    private data class Interval(
        val color: Int,
        val start: Float,
        val end: Float,
        var scheduleID: String,
        var index: Int = 0,
    )
    private data class Section(
        var start: Float = 0f,
        var end: Float = 0f,
        var intervals: List<Interval> = emptyList()
    )
    private data class ActionDrawable(
        val color: Int,
        val start: Float,
        val end: Float,
        val left: Float,
        val right: Float,
    )


    /* Это даже и здесь...
    * Нужно для оптимизированного нахождения изменений (а нужно ли?) */
    private class Diff(private val oldList: List<String>,
               private val newList: List<String>): DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }


    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>, useSchedule: Boolean){
        Executors.newSingleThreadExecutor().execute {
            this.mCount = 1

            /* Corrupt actions schedule */
            val corrupted = mutableListOf<String>()

            /*         Initialization          */
            val rawPoints = mutableListOf<Point>()
            val intervals = mutableMapOf<String, Interval>()

            actionsSchedule.forEach{
                val color = mRepository.getColor(it.actionTypeId)
                val start = it.startTime/(24f*60*60)
                var end = it.endTime/(24f*60*60)

                if (start > 1f) {
                    corrupted.add(it.id)
                    return@forEach
                }
                else if (end > 1f) {
                    corrupted.add(it.id)
                    end = 1f
                }

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id,false, end, color))
                intervals[it.id] = Interval(color, start, end, it.scheduleId)
            }
            val points = rawPoints.sortedBy { it.coordinate }

            /*         Create sections and set index action drawables          */
            /* Суть в том, что мы сортируем массив точек и по ним находим области пересечения и
            * определяем идексы  */
            /* Это нужно для определения областей пересечения */
            val sections = mutableListOf<Section>()
            var section = Section()
            var intervalsForSection = mutableListOf<Interval>()

            /* Нужно для определение индексов. Это можно делать и без этого, но это нужно, чтобы
            * действия из одного расписания имели одной индекс. Естественно, это при условии, что
            * само расписание не имеет пересечений */
            var index = 0
            var activeSchedules = mutableMapOf<String, Int>()
            /* А это нужно, если расписания не используются. Тогда действия будут иметь индекс
            * первого свободного слобца */
            val columns = mutableListOf("")

            /* Нужно для определения пересечений (до слез реально) */
            var first = ""
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    active.add(point.id)

                    if (active.size == 1) {
                        first = point.id
                        columns[0] = point.id
                    }
                    if (active.size == 2) {
                        section.start = point.coordinate

                        intervalsForSection.add(intervals[first]!!)
                        activeSchedules[intervals[first]!!.scheduleID] = 1
                    }
                    if (active.size > 1) {
                        corrupted.add(point.id)

                        if (useSchedule) {
                            val scheduleID = intervals[point.id]!!.scheduleID
                            if (scheduleID !in activeSchedules) {
                                index += 1
                                intervals[point.id]!!.index = index
                                activeSchedules[scheduleID] = index
                            } else intervals[point.id]!!.index = activeSchedules[scheduleID]!!
                        } else {
                            val freeIndex = columns.indexOfFirst { it == "" }
                            if (freeIndex == -1){
                                index += 1
                                columns.add(point.id)
                                intervals[point.id]!!.index = index
                            } else {
                                intervals[point.id]!!.index = freeIndex
                                columns[freeIndex] = point.id
                            }
                        }

                        if (index+1 > this.mCount) this.mCount = index+1
                        intervalsForSection.add(intervals[point.id]!!)
                    }
                } else {
                    active.remove(point.id)

                    columns[columns.indexOfFirst { it == point.id }] = ""

                    if (active.size == 1) {
                        section.end = point.coordinate
                        section.intervals = intervalsForSection
                        sections.add(section)

                        section = Section()
                        first = active[0]
                        intervalsForSection = mutableListOf()
                        activeSchedules = mutableMapOf()
                    }
                }
            }

            /* Create action drawables */
            val actionDrawables = mutableListOf<ActionDrawable>()
            intervals.toList().forEach {
                val interval = it.second

                val left = interval.index.toFloat()
                val right = left+1

                actionDrawables.add(
                    ActionDrawable(interval.color, interval.start, interval.end, left, right))
            }

            /*     Calculate diff     */
            val diff = Diff(mCorrupted, corrupted)
            val diffResult = DiffUtil.calculateDiff(diff , false)
            mCorrupted.forEachIndexed { i, id ->
                if (diffResult.convertOldPositionToNew(i) == DiffUtil.DiffResult.NO_POSITION)
                    mHandler.post { mCorruptedListener?.deleteCorrupt(id) }
            }
            corrupted.forEachIndexed { i, id ->
                if (diffResult.convertNewPositionToOld(i) == DiffUtil.DiffResult.NO_POSITION)
                    mHandler.post { mCorruptedListener?.addCorrupt(id) }
            }

            /*  Update data  */
            this.mActionDrawables = actionDrawables
            this.mSections = sections
            this.mCorrupted = corrupted

            /*  Notify  */
            mHandler.post{ requestLayout() }
            mHandler.post{ mFinishedListener?.finish() }
        }
    }

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
        // val width = width.toFloat()

        mPaint.color = colorColumn
        for (i in 0 until mCount){
            canvas?.drawRoundRect((stripWidth+spaceWidth)*i, 0f,
                (stripWidth+spaceWidth)*(i+1)-spaceWidth, height, corner, corner, mPaint)
        }

        mActionDrawables.forEach {
            mPaint.color = it.color
            canvas?.drawRoundRect((stripWidth+spaceWidth)*it.left, height*it.start,
                (stripWidth+spaceWidth)*it.right-spaceWidth, height*it.end, corner, corner, mPaint)
        }
    }
}


class ClockFaceView(context: Context, attrs: AttributeSet): View(context, attrs){
    private var textSize: Int
    private var spaceHeight: Int
    private var paint: Paint = Paint()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ClockFaceView,
            0, 0).apply {
            try {
                textSize = getDimensionPixelSize(R.styleable.ClockFaceView_textSize, 0)
                spaceHeight = getDimensionPixelSize(R.styleable.ClockFaceView_spaceHeight , 0)
            } finally {
                recycle()
            }
        }

        paint.textSize = textSize.toFloat()
        paint.isAntiAlias = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        val desiredHeight = (textSize+spaceHeight)*24

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> requestedWidth
            MeasureSpec.AT_MOST -> requestedWidth
            else -> requestedWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> requestedHeight
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(requestedWidth)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val height = height.toFloat()
        val width = width.toFloat()

        for (i in 0..23){
            val text = "$i:00"
            paint.color = Color.WHITE
            canvas?.drawText(text, (width-paint.measureText(text))/2,
                i/24f*height+textSize, paint)
        }
    }
}


class ScheduleClockView(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {
    private val timeView: TimeView


    init {
        inflate(context, R.layout.layout_schedule_clock_view, this)

        timeView = findViewById(R.id.timeView)
        timeView.setFinishedListener(object : TimeView.FinishedListener{
            override fun finish() {
                mFinishedListener?.finish()
            }
        })
        timeView.setCorruptedListener(object : TimeView.CorruptedListener{
            override fun addCorrupt(id: String) {
                mCorruptedListener?.addCorrupt(id)
            }

            override fun deleteCorrupt(id: String) {
                mCorruptedListener?.deleteCorrupt(id)
            }
        })
    }


    /*   Вызывается, когда заканчивается обработка данных   */
    private var mFinishedListener: FinishedListener? = null
    interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }


    /*  Вызывается, когда добавляется или удаляется corrupt action schedule  */
    private var mCorruptedListener: CorruptedListener? = null
    interface CorruptedListener{
        fun addCorrupt(id: String)
        fun deleteCorrupt(id: String)
    }
    fun setCorruptedListener(corruptedListener: CorruptedListener){
        mCorruptedListener = corruptedListener
    }


    /*   Set data   */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>, useSchedule: Boolean){
        timeView.setActionsSchedule(actionsSchedule, useSchedule)
    }

}
