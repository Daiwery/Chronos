/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.icu.util.TimeZone
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.ActionTypeRepository
import java.util.concurrent.Executors

/**
 * В данном файле описаны необходимые виджеты для визуализации времени.
 */

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
     * Задний цвет первой колонки. UI. Получает из xml.
     */
    private var colorColumn: Int

    /**
     * Репозиторий для связи с базой данных типов действий. Необходим для получения цвета.
     */
    private val mRepository = ActionTypeRepository.get()

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
     * Интерфейс, который сообщает, что завершена обработка данных.
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
     * С помощью этого находит те действия, которые пересекаются. После находит для первый
     * свободны столбец, в котором можно его нарисовать.
     * Выбор регулирует параметр useSchedule.
     * @param actionsSchedule массив с действиями в расписании.
     * @param startDayTime начало дня. Нужен для нахождения выхода за пределы в относительном
     * расписании. В абсолютном равен null.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>, startDayTime: Long?){
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
                if (startDayTime != null)
                    if (start-1f >= startDayTime/(24f*60*60) || end-1f >= startDayTime/(24f*60*60)) {
                        corrupted.add(it.id)
                        return@forEach
                    }

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id,false, end, color))
                intervals[it.id] = Interval(color, start, end, it.dayID)
            }
            val points = rawPoints.sortedBy { it.coordinate }


            // А это нужно, если расписания не используются. Тогда действия будут иметь индекс
            // первого свободного слобца.
            val columns = mutableListOf("")
            var index = 0

            // Нужно для определения пересечений.
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    active.add(point.id)

                    if (active.size == 1) columns[0] = point.id

                    if (active.size > 1) {
                        corrupted.add(point.id)

                        val freeIndex = columns.indexOfFirst { it == "" }
                        if (freeIndex == -1){
                            index += 1
                            columns.add(point.id)
                            intervals[point.id]!!.index = index
                        } else {
                            intervals[point.id]!!.index = freeIndex
                            columns[freeIndex] = point.id
                        }

                        if (index+1 > this.mCount) this.mCount = index+1
                    }
                } else {
                    active.remove(point.id)

                    columns[columns.indexOfFirst { it == point.id }] = ""
                }
            }


            val actionDrawables = mutableListOf<ActionDrawable>()
            intervals.toList().forEach {
                val interval = it.second

                val left = interval.index.toFloat()
                val right = left+1

                actionDrawables.add(
                    ActionDrawable(interval.color, interval.start, interval.end, left, right)
                )
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
            this.mCorrupted = corrupted

            updateActionDrawables()

            // Это нужно сделать, так как мы не в основном потоке.
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
        canvas?.drawRoundRect(0f, 0f,
            (stripWidth+spaceWidth)-spaceWidth, height, corner, corner, mPaint)


        mActionDrawables.forEach {
            mPaint.color = it.color
            canvas?.drawRoundRect((stripWidth+spaceWidth)*it.left, height*it.start,
                (stripWidth+spaceWidth)*it.right-spaceWidth, height*it.end, corner, corner, mPaint)
        }
    }
}

/**
 * Тоже самое, что и ScheduleView, но используется для нескольких расписаний одновременно.
 * Использует для расчета индекса в Interval от факт, что в одном расписании нет
 * пересечений.
 */
class MultiScheduleView(context: Context, attrs: AttributeSet): View(context, attrs) {
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
     * Задний цвет первой колонки. UI. Получает из xml.
     */
    private var colorColumn: Int
    /**
     * Цвет ошибки, в которую красится секция.
     */
    private var colorError: Int

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
     * Количество колонок. Необходим для расчета ширины виджета.
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
     * Время начала на цифрблате. В секундах.
     */
    private var startTime: Long = 0

    /**
     * Массив с ActionTypeTime. Нужен, чтобы определить,
     * какое сейчас действие необходимо выполнять.
     */
    private var mActionTypes: List<ActionTypeTime> = emptyList()

    /**
     * Timer. Каждую минуту проверяет, какой тип действие нужно выполнить, и
     * если оно изменилось, меняет необходимый UI.
     */
    private val mTimer: Handler = Handler(Looper.getMainLooper())

    /**
     * Инициализация виджета. Получение из xml необходимых атрибутов.
     */
    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MultiScheduleView,
            0, 0).apply {
            try {
                stripWidth = getDimensionPixelSize(R.styleable.MultiScheduleView_stripWidth, 0).toFloat()
                spaceWidth = getDimensionPixelSize(R.styleable.MultiScheduleView_spaceWidth, 0).toFloat()
                corner = getDimensionPixelSize(R.styleable.MultiScheduleView_corner, 0).toFloat()
                colorColumn = getColor(R.styleable.MultiScheduleView_colorColumn, 0)
                colorError = getColor(R.styleable.MultiScheduleView_colorError, 0)
            } finally {
                recycle()
            }
        }

        // Сглаживание.
        mPaint.isAntiAlias = true

        // Таймер
        val run = Runnable {
            val time = (System.currentTimeMillis()+TimeZone.getDefault().getOffset(System.currentTimeMillis()))/1000
            mMustActionTypeListener?.getMustActionType(getMustActionType(time))
        }
        mTimer.postDelayed(run, 60*1000L)
    }

    /**
     * Интерфейс, который сообщает, что завершена обработка данных.
     */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    /**
     * Интерфейс, который сообщает, на какую секцию было нажатие.
     */
    private var mClickListener: ClickListener? = null
    fun interface ClickListener{
        fun onClick(section: Section)
    }
    fun setClickListener(clickListener: ClickListener){
        mClickListener = clickListener
    }

    /**
     * Интерфейс, который сообщает количество испорченных секций.
     */
    private var mCorruptedListener: CorruptedListener? = null
    fun interface CorruptedListener{
        fun getCount(countCorrupted: Int)
    }
    fun setCorruptedListener(corruptedListener: CorruptedListener){
        mCorruptedListener = corruptedListener
    }

    /**
     * Интерфейс, который сообщает о типе действии, которое нужно выполнить.
     */
    private var mMustActionTypeListener: MustActionTypeListener? = null
    fun interface MustActionTypeListener{
        fun getMustActionType(id: String?)
    }
    fun setMustActionTypeListener(mustActionTypeListener: MustActionTypeListener){
        mMustActionTypeListener = mustActionTypeListener
    }


    /**
     * Вспомогательный класс. Хранит информацию об временном интервале.
     * Нужен в алгоритме для обработки данных.
     * Ключевое свойство - index. Этот индекс указывает, в каком столбце
     * стоит нарисовать действие.
     */
    data class Interval(
        val color: Int,
        val actionTypeID: String,
        val start: Float,
        val end: Float,
        var scheduleID: String,
        var index: Int = 0,
    )

    /**
     * Хранит информацию о секции, в которой пересекаются действия. Нужен для UI.
     */
    data class Section(
        var start: Float = 0f,
        var end: Float = 0f,
        var intervals: List<Interval> = emptyList()
    )

    /**
     * Вспомогательный класс. Связывает тип действия и время. Нужен, чтобы определить,
     * какое сейчас действие необходимо выполнять.
     */
    private data class ActionTypeTime(
        var actionTypeID: String? = "",
        var start: Float = 0f,
        var end: Float = 0f
    )

    /**
     * Устанавливает и обрабатывает данные.
     *
     * Основная суть алгоритма состоит в превращении отрезка времени в две точки с указанием,
     * конец это или начало, и последующей сортировке этих точек по координате.
     * С помощью этого находит те действия, которые пересекаются. После находит для них столбец,
     * в котором находится расписание, в котором находится это действие.
     * Выбор регулирует параметр useSchedule.
     * @param actionsSchedule массив с действиями в расписании.
     * @param startDayTime начало дня.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>, startDayTime: Long){
        Executors.newSingleThreadExecutor().execute {
            this.mCount = 1

            val rawPoints = mutableListOf<Point>()
            val intervals = mutableMapOf<String, Interval>()

            val startDay = startDayTime/(24f*60*60)
            actionsSchedule.forEach{
                val color = mRepository.getColor(it.actionTypeId)
                var start = it.startTime/(24f*60*60)
                var end = it.endTime/(24f*60*60)

                // startDay  - это новое начало дня.
                if (start < startDay  && end < startDay ) return@forEach
                if (start > 1f+startDay && end > 1f+startDay ) return@forEach
                if (start < startDay ) start = startDay
                if (end > 1f+startDay ) end = 1f+startDay

                // После чего переводим в обычные координаты.
                start -= startDay
                end -= startDay

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id,false, end, color))
                intervals[it.id] = Interval(color, it.actionTypeId, start, end, it.dayID)
            }
            val points = rawPoints.sortedBy { it.coordinate }

            // Типы действий с временем начала и конца.
            val actionTypes = mutableListOf<ActionTypeTime>()
            var actionType = ActionTypeTime()

            // Это нужно для определения областей пересечения.
            val sections = mutableListOf<Section>()
            var section = Section()
            var intervalsForSection = mutableListOf<Interval>()

            // Нужно для определение индексов. Это нужно, чтобы
            // действия из одного расписания имели одной индекс. Естественно, это при условии, что
            // само расписание не имеет пересечений.
            var index = 0
            var activeSchedules = mutableMapOf<String, Int>()

            // Нужно для определения пересечений.
            var first = ""
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    active.add(point.id)

                    if (active.size == 1) {
                        first = point.id

                        // Начали одно новое действие.
                        actionType.actionTypeID = intervals[first]!!.actionTypeID
                        actionType.start = point.coordinate
                    }

                    // Если все-таки находим пересечение, то нужно не забыть
                    // про первое действие в этой секции (которое могло просто существовать
                    // без персечений.
                    if (active.size == 2) {
                        // При пересечении, неизвестно, что делать.
                        actionType.end = point.coordinate
                        actionTypes.add(actionType)
                        actionType = ActionTypeTime()

                        // Указываем, что тут ошибка.
                        actionType.actionTypeID = null
                        actionType.start = point.coordinate

                        section.start = point.coordinate

                        intervalsForSection.add(intervals[first]!!)
                        activeSchedules[intervals[first]!!.scheduleID] = 1
                    }

                    if (active.size > 1) {
                        val scheduleID = intervals[point.id]!!.scheduleID
                        if (scheduleID !in activeSchedules) {
                            index += 1
                            intervals[point.id]!!.index = index
                            activeSchedules[scheduleID] = index
                        } else intervals[point.id]!!.index = activeSchedules[scheduleID]!!


                        if (index+1 > this.mCount) this.mCount = index+1
                        intervalsForSection.add(intervals[point.id]!!)
                    }
                } else {
                    active.remove(point.id)

                    if (active.size == 1) {
                        section.end = point.coordinate
                        section.intervals = intervalsForSection
                        sections.add(section)

                        section = Section()
                        first = active[0]
                        index = 0
                        intervalsForSection = mutableListOf()
                        activeSchedules = mutableMapOf()

                        // Конец испорченной секции.
                        actionType.end = point.coordinate
                        actionTypes.add(actionType)
                        actionType = ActionTypeTime()
                        // И начало нового действия.
                        actionType.actionTypeID = intervals[first]!!.actionTypeID
                        actionType.start = point.coordinate
                    }
                    // Конец единственного действия.
                    if (active.size == 0) {
                        actionType.end = point.coordinate
                        actionTypes.add(actionType)
                        actionType = ActionTypeTime()
                    }
                }
            }


            val actionDrawables = mutableListOf<ActionDrawable>()
            intervals.toList().forEach {
                val interval = it.second

                val left = interval.index.toFloat()
                val right = left+1

                actionDrawables.add(
                    ActionDrawable(interval.color, interval.start, interval.end, left, right)
                )
            }


            this.mActionDrawables = actionDrawables
            this.mSections = sections
            this.mActionTypes = actionTypes


            // Это нужно сделать, так как мы не в основном потоке.
            mHandler.post{ requestLayout() }
            mHandler.post{ mFinishedListener?.finish() }
            mHandler.post{ mCorruptedListener?.getCount(mSections.size) }
            mHandler.post{
                val time = (System.currentTimeMillis()+TimeZone.getDefault().getOffset(System.currentTimeMillis()))/1000
                mMustActionTypeListener?.getMustActionType(getMustActionType(time))
            }
        }
    }

    /**
     * Возвращает id тип действия, которое нужно сейчас выполнить.
     * "" - ничего
     * null - ошибка.
     */
    private fun getMustActionType(time: Long): String? {
        var timeDay = time%(24*60*60)-startTime
        if (timeDay < 0) timeDay += 24*60*60

        val coordinate = timeDay/(24f*60*60)
        val actionType = mActionTypes.firstOrNull { coordinate >= it.start && coordinate <= it.end }

        return if (actionType == null) ""
        else actionType.actionTypeID
    }


    /**
     * Устанавливает новое время начала. Расчитывать новое расположение необходимо заного.
     */
    fun setStartTime(startTime: Long){
        this.startTime = startTime
    }

    /**
     * Выполняется при нажатии на экран.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event?.actionMasked == MotionEvent.ACTION_DOWN){
            val y = event.y/height.toFloat()
            val section = mSections.firstOrNull{ y >= it.start && y <= it.end }

            if (section != null) {
                mClickListener?.onClick(section)
            }
        }

        return true
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
        val width = width.toFloat()

        mPaint.color = colorColumn
        canvas?.drawRoundRect(0f, 0f,
            (stripWidth+spaceWidth)-spaceWidth, height, corner, corner, mPaint)

        mActionDrawables.forEach {
            mPaint.color = it.color
            canvas?.drawRoundRect((stripWidth+spaceWidth)*it.left, height*it.start,
                (stripWidth+spaceWidth)*it.right-spaceWidth, height*it.end, corner, corner, mPaint)
        }

        mPaint.color = colorError
        mSections.forEach {
            canvas?.drawRect(0f, it.start*height, width, it.end*height, mPaint)
        }
    }
}

/**
 * Очень похож на класс ScheduleView, за тем исключением, что находит только индексы у Interval.
 */
class ActionsView(context: Context, attrs: AttributeSet): View(context, attrs) {
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
     * Задний цвет первой колонки. UI. Получает из xml.
     */
    private var colorColumn: Int

    /**
     * Репозиторий для связи с базой данных типов действий. Необходим для получения цвета.
     */
    private val mRepository = ActionTypeRepository.get()

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
     * Время начала на цифрблате. В секундах.
     */
    private var startTime: Long = 0

    /**
     * Инициализация виджета. Получение из xml необходимых атрибутов.
     */
    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ActionsView,
            0, 0).apply {
            try {
                stripWidth = getDimensionPixelSize(R.styleable.ActionsView_stripWidth, 0).toFloat()
                spaceWidth = getDimensionPixelSize(R.styleable.ActionsView_spaceWidth, 0).toFloat()
                corner = getDimensionPixelSize(R.styleable.ActionsView_corner, 0).toFloat()
                colorColumn = getColor(R.styleable.ActionsView_colorColumn, 0)
            } finally {
                recycle()
            }
        }

        // Сглаживание.
        mPaint.isAntiAlias = true
    }

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
        var index: Int = 0,
    )

    /**
     * Устанавливает и обрабатывает данные.
     *
     * Основная суть алгоритма состоит в превращении отрезка времени в две точки с указанием,
     * конец это или начало, и последующей сортировке этих точек по координате.
     * С помощью этого находит те действия, которые пересекаются. После находит для первый
     * свободны столбец, в котором можно его нарисовать.
     * @param actions массив с действиями. Время НЕ локальное.
     * @param local отклонение от местного времени
     * @param day локальный день, от начало которого нужно отсчитывать.
     */
    fun setActions(actions: List<Action>, local: Int, day: Int){
        Executors.newSingleThreadExecutor().execute {
            this.mCount = 1

            val rawPoints = mutableListOf<Point>()
            val intervals = mutableMapOf<String, Interval>()

            val startDay = day*24*60*60+startTime

            actions.forEach{
                val color = mRepository.getColor(it.actionTypeId)
                var start = (it.startTime+local-startDay)/(24f*60*60)
                var end = (it.endTime+local-startDay)/(24f*60*60)

                // На всякий случай
                if (start < 0f && end < 0f) return@forEach
                if (start > 1f && end > 0f) return@forEach
                // Если мы вышли за пределы, то значит действие начинается
                // или заканчивается в другом дне.
                if (start < 0f) start = 0f
                if (end > 1f) end = 1f

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id, false, end, color))
                intervals[it.id] = Interval(color, start, end)
            }
            val points = rawPoints.sortedBy { it.coordinate }

            // Это нужно для определения индекса. Действия будут иметь индекс
            // первого свободного слобца.
            var index = 0
            val columns = mutableListOf("")

            // Нужно для определения пересечений.
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    active.add(point.id)

                    if (active.size == 1) columns[0] = point.id
                    if (active.size > 1) {
                        val freeIndex = columns.indexOfFirst { it == "" }
                        if (freeIndex == -1){
                            index += 1
                            columns.add(point.id)
                            intervals[point.id]!!.index = index
                        } else {
                            intervals[point.id]!!.index = freeIndex
                            columns[freeIndex] = point.id
                        }

                        if (index+1 > this.mCount) this.mCount = index+1
                    }
                } else {
                    active.remove(point.id)
                    columns[columns.indexOfFirst { it == point.id }] = ""
                }
            }


            val actionDrawables = mutableListOf<ActionDrawable>()
            intervals.toList().forEach {
                val interval = it.second

                val left = interval.index.toFloat()
                val right = left+1

                actionDrawables.add(
                    ActionDrawable(interval.color, interval.start,
                    interval.end, left, right)
                )
            }

            this.mActionDrawables = actionDrawables

            // Это нужно сделать, так как мы не в основном потоке.
            mHandler.post{ requestLayout() }
        }
    }

    /**
     * Устанавливает новое время начала. Расчитывать новое расположение необходимо заного.
     */
    fun setStartTime(startTime: Long){
        this.startTime = startTime
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
        canvas?.drawRoundRect(0f, 0f,
            (stripWidth+spaceWidth)-spaceWidth, height, corner, corner, mPaint)

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

        paint.color = Color.BLACK
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
 * ViewGroup. Соединение виджетов выше с ScrollView. Используется в DayScheduleFragment.
 * Высота определяется ClockFaceView.
 */
class ScheduleClockView(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {
    /**
     * ScheduleView. Необходим для настройки интерфейсов.
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

        scheduleView = findViewById(R.id.scheduleView)
        scheduleView.setFinishedListener{ mFinishedListener?.finish() }
        scheduleView.setCorruptedListener(object : ScheduleView.CorruptedListener {
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
     * Установка данных для ScheduleView.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>,
                           defaultStartDayTime: Long?){
        scheduleView.setActionsSchedule(actionsSchedule, defaultStartDayTime)
    }

    /**
     * Устанавливает новое время начала.
     */
    fun setStartTime(startTime: Long){
        clockFaceView.setStartTime(startTime)
        scheduleView.setStartTime(startTime)
    }
}

/**
 * ViewGroup. Соединение виджетов выше с ScrollView. Используется в DayFragment.
 * Высота определяется ClockFaceView.
 */
class ActionsClockView(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {
    /**
     * ScheduleView. Необходим для настройки интерфейсов.
     */
    private val multiScheduleView: MultiScheduleView
    /**
     * ActionsView. Необходим для настройки интерфейсов.
     */
    private val actionsView: ActionsView
    /**
     * ClockFaceView. Необходим для задания времени начала.
     */
    private val clockFaceView: ClockFaceView

    /**
     * Инициализация виджета. Заполнение макета и настройка интерфейсов.
     */
    init {
        inflate(context, R.layout.layout_actions_clock_view, this)

        clockFaceView = findViewById(R.id.clockFaceView)
        multiScheduleView = findViewById(R.id.multiScheduleView)
        multiScheduleView.setFinishedListener{ mFinishedListener?.finish() }
        multiScheduleView.setClickListener{ mClickListener?.onClick(it) }
        multiScheduleView.setCorruptedListener{ mCorruptedListener?.getCount(it) }
        multiScheduleView.setMustActionTypeListener{ mMustActionTypeListener?.getMustActionType(it) }
        actionsView = findViewById(R.id.actionsView)
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
     * Интерфейс, который сообщает, на какую секцию было нажатие.
     */
    private var mClickListener: ClickListener? = null
    fun interface ClickListener{
        fun onClick(section: MultiScheduleView.Section)
    }
    fun setClickListener(clickListener: ClickListener){
        mClickListener = clickListener
    }

    /**
     * Интерфейс, который сообщает количество испорченных секций.
     */
    private var mCorruptedListener: CorruptedListener? = null
    fun interface CorruptedListener{
        fun getCount(countCorrupted: Int)
    }
    fun setCorruptedListener(corruptedListener: CorruptedListener){
        mCorruptedListener = corruptedListener
    }

    /**
     * Интерфейс, который сообщает о типе действии, которое нужно выполнить.
     */
    private var mMustActionTypeListener: MustActionTypeListener? = null
    fun interface MustActionTypeListener{
        fun getMustActionType(id: String?)
    }
    fun setMustActionTypeListener(mustActionTypeListener: MustActionTypeListener){
        mMustActionTypeListener = mustActionTypeListener
    }

    /**
     * Установка данных для ScheduleView.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>, defaultStartDayTime: Long){
        multiScheduleView.setActionsSchedule(actionsSchedule, defaultStartDayTime)
    }

    /**
     * Установка данных для ActionsView.
     */
    fun setActions(actions: List<Action>, local: Int, day: Int){
        actionsView.setActions(actions, local, day)
    }

    /**
     * Устанавливает новое время начала.
     */
    fun setStartTime(startTime: Long){
        clockFaceView.setStartTime(startTime)
        multiScheduleView.setStartTime(startTime)
        actionsView.setStartTime(startTime)
    }

    /**
     * Устанавливает видимость компонентов UI, если мы в будущем.
     */
    fun setFuture(){
        actionsView.visibility = View.GONE
        multiScheduleView.visibility = View.VISIBLE
    }

    /**
     * Устанавливает видимость компонентов UI, если мы в прошлом.
     */
    fun setPast(){
        actionsView.visibility = View.VISIBLE
        multiScheduleView.visibility = View.GONE
    }

    /**
     * Устанавливает видимость компонентов UI, если мы в настощем.
     */
    fun setPresent(){
        actionsView.visibility = View.VISIBLE
        multiScheduleView.visibility = View.VISIBLE
    }
}
