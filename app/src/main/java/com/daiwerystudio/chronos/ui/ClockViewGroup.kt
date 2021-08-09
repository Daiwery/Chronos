/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

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

/**
 * В данном файле описаны необходимые виджеты для визуализации времени.
 */

/**
 * Основной виджет для визуализации времени. Показывает действия на временной шкале.
 *
 * Главная особенность состоит в том, что если времена действий пересекают друг друга,
 * то это действие нужно отодвинуть в сторону, чтобы не рисовать их друг на друге.
 * Основная суть алгоритма, обрабатывающее это, состоит в превращении отрезка времени
 * в две точки с указанием, конец это или начало, и последующей сортировке этих точек по
 * координате. После чего можно найти те действия, которые пересекаются, и делать с ними,
 * что требуется.
 *
 * Возможная модификация: при смене defaultStartDayTime все расчеты происходят заного.
 * Лучше сделать что-то более оптимизированное.
 */
class ScheduleView(context: Context, attrs: AttributeSet): View(context, attrs) {
    /**
     * Ширина линии. UI. Получает из xml.
     */
    private var stripWidth: Float
    /**
     * Ширина пробела. UI. Получает из xml.
     */
    private var spaceWidth: Float
    /**
     * Размер закруглений у отрезков времени. UI. Получает из xml.
     */
    private var corner: Float
    /**
     * Задний цвет одерй колонки. UI. Получает из xml.
     */
    private var colorColumn: Int

    /**
     * Репозиторий для связи с базой данных типов действий. Необходим для получения цвета.
     */
    private val mRepository = ActionTypeRepository.get()

    /**
     * Массив секций, в которых пересекаются действия. В каждой секции содержится начало, конец
     * и какие действия.
     */
    private var mSections: List<Section> = emptyList()

    /**
     * Массив ActionDrawables. Необходим для прорисовки.
     */
    private var mActionDrawables: List<ActionDrawable> = emptyList()

    /**
     * Количество колонок. Необохдим для расчета ширины виджета.
     */
    private var mCount: Int = 1

    /**
     * Paint. Необходим для рисования.
     */
    private val mPaint: Paint = Paint()

    /**
     * Handler. Необходим для передачи запроса на обновление виджета из потока, в котором
     * обратываются действия.
     */
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * Массив с id испорченных действий. Необходим, чтобы при получении новых данных, найти
     * изменения и сообщить их в CorruptedListener.
     */
    private var mCorrupted: List<String> = emptyList()

    /**
     * Время начала на цифрблате. В секундах.
     */
    private var startTime: Long = 0

    /**
     * Инициализация виджета. Получение из xml необходимых атрибутов.
     */
    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ScheduleView,
            0, 0).apply {
            try {
                stripWidth = getDimensionPixelSize(R.styleable.ScheduleView_stripWidth, 0).toFloat()
                spaceWidth = getDimensionPixelSize(R.styleable.ScheduleView_spaceWidth , 0).toFloat()
                corner = getDimensionPixelSize(R.styleable.ScheduleView_corner , 0).toFloat()
                colorColumn = getColor(R.styleable.ScheduleView_colorColumn , 0)
            } finally {
                recycle()
            }
        }

        // Сглаживание.
        mPaint.isAntiAlias = true
    }

    /**
     * Интерфейс, который сообщает, что заверешна обработка данных.
     */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    /**
     * Интерфейс, который сообщает, какой action schedule испорчен посредстом его id.
     * countCorrupted нужен, чтобы при равенстве его нулю, отметить расписание, как не испорченное.
     */
    private var mCorruptedListener: CorruptedListener? = null
    interface CorruptedListener{
        fun addCorrupt(id: String)
        fun deleteCorrupt(id: String, countCorrupted: Int)
    }
    fun setCorruptedListener(corruptedListener: CorruptedListener){
        mCorruptedListener = corruptedListener
    }

    /**
     * Вспомогательный класс. Хранит информацию об одной точке. Нужен в алгоритме для обработки
     * данных.
     */
    private data class Point(
        val id: String,
        val isStart: Boolean,
        val coordinate: Float,
        val color: Int
    )

    /**
     * Вспомогательный класс. Хранит информацию об временном интервале.
     * Нужен в алгоритме для обработки данных.
     * Ключевое свойство - index. Этот индекс указывает, в каком столбце
     * стоит нарисовать действие.
     */
    private data class Interval(
        val color: Int,
        val start: Float,
        val end: Float,
        var scheduleID: String,
        var index: Int = 0,
    )

    /**
     * Хранит информацию о секции, в которой пересекаются действия. Нужен для UI.
     */
    private data class Section(
        var start: Float = 0f,
        var end: Float = 0f,
        var intervals: List<Interval> = emptyList()
    )

    /**
     * Хранит информацию о том, где и как нужно рисовать.
     */
    private data class ActionDrawable(
        val color: Int,
        var start: Float,
        var end: Float,
        val left: Float,
        val right: Float,
    )

    /**
     * Класс для объявления функций класса DiffUtil.Callback. См. оф. документацию.
     *
     * Нужен для оптимизированного нахождения изменений у mCorrupted.
     */
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

    /**
     * Устанавливает и обрабатывает данные.
     *
     * Основная суть алгоритма состоит в превращении отрезка времени в две точки с указанием,
     * конец это или начало, и последующей сортировке этих точек по координате.
     * С помощью этого находит те действия, которые пересекаются. После находит для них столбец,
     * в котором нужно его нарисовать с помощью двух вариантов: ставит тот столбец,
     * который свободен (стремится дать самый первый) или ставит столбец с расписанием
     * таким же, как и у действия (это нужно для в/д нескольких расписаний).
     * Выбор регулирует параметр useSchedule.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>,
                           useSchedule: Boolean,
                           defaultStartDayTime: Long?){
        Executors.newSingleThreadExecutor().execute {
            this.mCount = 1

            val corrupted = mutableListOf<String>()
            val rawPoints = mutableListOf<Point>()
            val intervals = mutableMapOf<String, Interval>()

            actionsSchedule.forEach{
                val color = mRepository.getColor(it.actionTypeId)
                val start = it.startTime/(24f*60*60)
                val end = it.endTime/(24f*60*60)

                // В абсолютном расписании невозможно указать время больше, чем 24 часа.
                // Но в относительном можно. Но тут все по-другому. Все, что больше 24 часов,
                // должно находится в промежутке между 00:00 и defaultDayStartTime, но в следующем
                // дне. Если не влезает, то ошибка.
                // Если defaultStartDayTime == null, то это не относительное расписание.
                if (defaultStartDayTime != null)
                    if (start-1f >= defaultStartDayTime/(24f*60*60) || end-1f >= defaultStartDayTime/(24f*60*60)) {
                        corrupted.add(it.id)
                        return@forEach
                    }

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id,false, end, color))
                intervals[it.id] = Interval(color, start, end, it.scheduleID)
            }
            val points = rawPoints.sortedBy { it.coordinate }


            // Это нужно для определения областей пересечения
            val sections = mutableListOf<Section>()
            var section = Section()
            var intervalsForSection = mutableListOf<Interval>()

            // Нужно для определение индексов. Это можно делать и без этого, но это нужно, чтобы
            // действия из одного расписания имели одной индекс. Естественно, это при условии, что
            // само расписание не имеет пересечений
            var index = 0
            var activeSchedules = mutableMapOf<String, Int>()
            // А это нужно, если расписания не используются. Тогда действия будут иметь индекс
            // первого свободного слобца
            val columns = mutableListOf("")

            // Нужно для определения пересечений (до слез реально)
            var first = ""
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    active.add(point.id)

                    if (active.size == 1) {
                        first = point.id
                        columns[0] = point.id
                    }

                    // Если все-таки находим пересечение, то нужно не забыть
                    // про первое действие в этой секции (которое могло просто существовать
                    // без персечений.
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


            val actionDrawables = mutableListOf<ActionDrawable>()
            intervals.toList().forEach {
                val interval = it.second

                val left = interval.index.toFloat()
                val right = left+1

                actionDrawables.add(
                    ActionDrawable(interval.color, interval.start, interval.end, left, right))
            }


            val diff = Diff(mCorrupted, corrupted)
            val diffResult = DiffUtil.calculateDiff(diff , false)
            mCorrupted.forEachIndexed { i, id ->
                if (diffResult.convertOldPositionToNew(i) == DiffUtil.DiffResult.NO_POSITION)
                    mHandler.post { mCorruptedListener?.deleteCorrupt(id, corrupted.size) }
            }
            corrupted.forEachIndexed { i, id ->
                if (diffResult.convertNewPositionToOld(i) == DiffUtil.DiffResult.NO_POSITION)
                    mHandler.post { mCorruptedListener?.addCorrupt(id) }
            }


            this.mActionDrawables = actionDrawables
            this.mSections = sections
            this.mCorrupted = corrupted

            updateActionDrawables()

            // Это нужно сделать, так как ммы не в основном потоке.
            mHandler.post{ requestLayout() }
            mHandler.post{ mFinishedListener?.finish() }
        }
    }


    /**
     * Устанавливает новое время начала. Расчитывать новое расположение необходимо заного.
     */
    fun setStartTime(startTime: Long){
        this.startTime = startTime
    }

    /**
     * Обновляет расположение действий на временной шкале от нового startTime.
     * Необходимо вызывать после назвачения mActionDrawables и до requestLayout().
     */
    private fun updateActionDrawables(){
        val actionDrawables = mutableListOf<ActionDrawable>()

        mActionDrawables.forEach {
            val actionDrawable = it

            actionDrawable.start -= startTime/(60f*60*24)
            actionDrawable.end -= startTime/(60f*60*24)

            if (actionDrawable.start < 0f && actionDrawable.end < 0f){
                actionDrawable.start += 1f
                actionDrawable.end += 1f
                actionDrawables.add(actionDrawable)
            } else if (actionDrawable.start < 0f){
                val new = actionDrawable.copy()
                new.start = 0f
                actionDrawables.add(new)

                actionDrawable.start += 1f
                actionDrawable.end = 1f
                actionDrawables.add(actionDrawable)
            } else actionDrawables.add(actionDrawable)
        }

        mActionDrawables = actionDrawables
    }

    /**
     * Вызывается, когда родитель хочет установить размер для этого виджета.
     * Изменят только ширину виджета в зависимости от mCount.
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

    /**
     * Событие рисования. Рисует все действия на временной шкале.
     */
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

/**
 * Вспомогательный виджет. Показывает циферблат. Имеет один изменяемый парамент: время
 * в самом начале. То есть может показывать не только от 00:00 до 00:00
 */
class ClockFaceView(context: Context, attrs: AttributeSet): View(context, attrs){
    /**
     * Размер текста. UI. Получает из xml.
     */
    private var textSize: Int

    /**
     * Размер пространства между часами. Грудо говоря, размер одного часа.
     * UI. Получает из xml.
     */
    private var spaceHeight: Int

    /**
     * Paint. Рисует.
     */
    private var paint: Paint = Paint()

    /**
     * Время начала на цифрблате. В секундах.
     */
    private var startTime: Long = 0

    /**
     * Массив с текстом часа и его координатой.
     * Нужен для единоразового расчета расположения цифр.
     */
    private var hours: List<Hour> = emptyList()

    /**
     * Handler. Необходим для передачи запроса на обновление виджета из потока, в котором
     * обратываются действия.
     */
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * Иниализация виджета. Получение параметров из xml.
     */
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

        paint.color = Color.WHITE
        paint.textSize = textSize.toFloat()
        paint.isAntiAlias = true

        // setStartTime(0)
    }

    /**
     * Вспомогательный класс. Нужен для единоразового расчета расположения цифр.
     */
    data class Hour(
        var text: String,
        var width: Float,
        var y: Float
    )

    /**
     * Устанавливает новое время начала и расчитывает расположение цифр.
     */
    fun setStartTime(startTime: Long){
        this.startTime = startTime

        val hours = mutableListOf<Hour>()
        for (i in 0..23){
            val text = if (i < 10) "0$i:00"
            else "$i:00"

            val width = paint.measureText(text)

            var y = i/24f
            y -= startTime/(60f*60*24)
            if (y < 0) y += 1

            hours.add(Hour(text, width, y))
        }

        this.hours = hours

        mHandler.post{ requestLayout() }
    }

    /**
     * Вызывается, когда родитель хочет установить размер для этого виджета.
     * Изменят только вызоту виджета в зависимости от заданных параметров.
     */
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

    /**
     * Событие рисования. Рисует циферблат.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val height = height.toFloat()
        val width = width.toFloat()

        hours.forEach {
            canvas?.drawText(it.text, (width-it.width)/2, height*it.y+textSize, paint)
        }
    }
}

/**
 * ViewGroup. Соединение тех виджетов с ScrollView. Используется в DayScheduleFragment.
 */
class ScheduleClockView(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {
    /**
     * ActionsScheduleView. Необходим для настройки интерфейсов.
     */
    private val scheduleView: ScheduleView
    /**
     * ClockFaceView. Необходим для задания времени начала.
     */
    private val clockFaceView: ClockFaceView

    /**
     * Инициализация виджета. Заполнение макета и настройка интерфейсов.
     */
    init {
        inflate(context, R.layout.layout_schedule_clock_view, this)

        scheduleView = findViewById(R.id.timeView)
        scheduleView.setFinishedListener{ mFinishedListener?.finish() }
        scheduleView.setCorruptedListener(object : ScheduleView.CorruptedListener{
            override fun addCorrupt(id: String) {
                mCorruptedListener?.addCorrupt(id)
            }

            override fun deleteCorrupt(id: String, countCorrupted: Int) {
                mCorruptedListener?.deleteCorrupt(id, countCorrupted)
            }
        })

        clockFaceView = findViewById(R.id.clockFaceView)
    }

    /**
     * Интерфейс, который сообщает, что заверешна обработка данных.
     */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    /**
     * Интерфейс, который сообщает, какой action schedule испорчен посредстом его id.
     */
    private var mCorruptedListener: CorruptedListener? = null
    interface CorruptedListener{
        fun addCorrupt(id: String)
        fun deleteCorrupt(id: String, countCorrupted: Int)
    }
    fun setCorruptedListener(corruptedListener: CorruptedListener){
        mCorruptedListener = corruptedListener
    }


    /**
     * Установка данных для TimeView.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>,
                           useSchedule: Boolean,
                           defaultStartDayTime: Long?){
        scheduleView.setActionsSchedule(actionsSchedule, useSchedule, defaultStartDayTime)
    }

    /**
     * Устанавливает новое время начала и расчитывает расположение цифр.
     */
    fun setStartTime(startTime: Long){
        clockFaceView.setStartTime(startTime)
        scheduleView.setStartTime(startTime)
    }
}
