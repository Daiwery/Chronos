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
*/

package com.daiwerystudio.chronos.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.icu.util.TimeZone
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView
import androidx.core.graphics.drawable.toBitmap
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.ActionTypeRepository
import java.util.concurrent.Executors

/*  В данном файле описаны необходимые виджеты для визуализации времени.  */

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
 * Показывает действия на временной шкале.
 *
 * Главная особенность состоит в том, что если времена действий пересекают друг друга,
 * то это действие нужно отодвинуть в сторону, чтобы не рисовать их друг на друге.
 * Основная суть алгоритма, обрабатывающее это, состоит в превращении отрезка времени
 * в две точки с указанием, конец это или начало, и последующей сортировке этих точек по
 * координате. После чего можно найти те действия, которые пересекаются, и делать с ними,
 * что требуется.
 *
 * Используется для показа действий внутри расписания.
 */
class ScheduleView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var stripWidth: Float
    private var spaceWidth: Float
    private var corner: Float
    private var colorColumn: Int
    private var mCount: Int = 1
    private val mPaint: Paint = Paint()
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * Репозиторий для связи с базой данных типов действий. Необходим для получения цвета.
     * Получает не LiveData, а напрямую значению., поэтому наблюдателей нет.
     */
    private val mRepository = ActionTypeRepository.get()

    /**
     * Массив действий, которые не нужно никак обрабатывать и можно сразу рисовать.
     */
    private var mActionDrawables: List<ActionDrawable> = emptyList()

    /**
     * Время начала на циферблате.
     */
    private var startTime: Long = 0


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

    /*  Интерфейс, который сообщает, что завершена обработка данных.  */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
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
        var dayID: String,
        var index: Int = 0,
    )

    /**
     * Устанавливает и обрабатывает данные.
     *
     * Основная суть алгоритма состоит в превращении отрезка времени в две точки с указанием,
     * конец это или начало, и последующей сортировке этих точек по координате.
     * С помощью этого находит те действия, которые пересекаются. После находит для них первый
     * свободный столбец, в котором можно это действие нарисовать.
     * @param actionsSchedule массив с действиями в расписании.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>){
        Executors.newSingleThreadExecutor().execute {
            mCount = 1

            val rawPoints = mutableListOf<Point>()
            // У каждого действия может быть только один интервал. Связываем id и интервал.
            val intervals = mutableMapOf<String, Interval>()

            actionsSchedule.forEach{
                val color = mRepository.getColor(it.actionTypeId)
                val start = it.startTime/(24f*60*60*1000)
                val end = it.endTime/(24f*60*60*1000)

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id,false, end, color))
                intervals[it.id] = Interval(color, start, end, it.scheduleID)
            }
            val points = rawPoints.sortedBy { it.coordinate }


            // Массив id действий, которые сейчас активны в столбце с номер, равным индексу.
            // Если id='', то это свободный столбец. По умолчанию всегда есть один столбец.
            val columns = mutableListOf("")
            // Это номер последного задейственного столбца.
            var index = 0

            // Список активных действий, то есть тех, которые есть в данный момент времени.
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    // Если это начало, то добавляем id в список активных id.
                    active.add(point.id)

                    // Если сейчас только одно активное действие, то даем ему первый столбец.
                    if (active.size == 1) columns[0] = point.id

                    // Если больше одного, то действия пересекаются.
                    if (active.size > 1) {
                        // Находим свободный индекс.
                        val freeIndex = columns.indexOfFirst { it == "" }
                        if (freeIndex == -1){
                            // Если не нашли, то ставим новый столбец.
                            index += 1
                            columns.add(point.id)
                            intervals[point.id]!!.index = index
                        } else {
                            intervals[point.id]!!.index = freeIndex
                            columns[freeIndex] = point.id
                        }
                        if (index+1 > mCount) mCount = index+1
                    }
                } else {
                    active.remove(point.id)
                    columns[columns.indexOfFirst { it == point.id }] = ""
                }
            }

            // Составляем объекты для рисования.
            val actionDrawables = mutableListOf<ActionDrawable>()
            intervals.toList().forEach {
                val interval = it.second

                val left = interval.index.toFloat()
                val right = left+1

                actionDrawables.add(
                    ActionDrawable(interval.color, interval.start, interval.end, left, right)
                )
            }

            mActionDrawables = actionDrawables

            // Обновляем объекты рисования с учетом другого начала времени.
            updateActionDrawables()

            // Это нужно сделать, так как мы не в основном потоке.
            mHandler.post{ requestLayout() }
            mHandler.post{ mFinishedListener?.finish() }
        }
    }

    /**
     * Обновляет расположение действий на временной шкале от нового startTime.
     * Необходимо вызывать после назвачения mActionDrawables и до requestLayout().
     */
    private fun updateActionDrawables(){
        val actionDrawables = mutableListOf<ActionDrawable>()

        mActionDrawables.forEach {
            val actionDrawable = it

            actionDrawable.start -= startTime/(1000f*60*60*24)
            actionDrawable.end -= startTime/(1000f*60*60*24)

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
 * При расчете индекса в Interval пытается установить для одного расписания один столбец.
 * Также показывает цели и напоминания на день.
 *
 * Используется для показа расписания на один день, которое составляется из множества расписаний.
 */
class MultiScheduleView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var stripWidth: Float
    private var spaceWidth: Float
    private var corner: Float
    private var colorColumn: Int
    private var sizeDrawable: Float
    private var marginIcon: Float
    private var drawableGoal: Drawable?
    private var drawableReminder: Drawable?

    private var bitmapGoal: Bitmap?
    private var bitmapReminder: Bitmap?
    private var mCount: Int = 1
    private val mPaint: Paint = Paint()
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * Массив действий, которые не нужно никак обрабатывать и можно сразу рисовать.
     */
    private var mActionDrawables: List<ActionDrawable> = emptyList()

    /**
     * Репозиторий для связи с базой данных типов действий. Необходим для получения цвета.
     * Получает не LiveData, а напрямую значение, поэтому наблюдателей нет.
     */
    private val mRepository = ActionTypeRepository.get()

    /**
     * Массив с ActionTypeTime. Нужен, чтобы определить,
     * какое сейчас действие необходимо выполнять.
     */
    private var mActionTypesTime: List<ActionTypeTime> = emptyList()

    /**
     * Массив с временами целей. Время от начала дня.
     */
    private var mGoalsTimes: List<Float> = emptyList()

    /**
     * Массив с временами напоминаний. Время от начала дня.
     */
    private var mRemindersTimes: List<Float> = emptyList()

    /**
     * Timer. Каждую минуту проверяет, какой тип действие нужно выполнить, и
     * если оно изменилось, сообщает об этом с помощью интерфейса.
     */
    private val mTimer: Handler = Handler(Looper.getMainLooper())


    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MultiScheduleView,
            0, 0).apply {
            try {
                stripWidth = getDimensionPixelSize(R.styleable.MultiScheduleView_stripWidth, 0).toFloat()
                spaceWidth = getDimensionPixelSize(R.styleable.MultiScheduleView_spaceWidth, 0).toFloat()
                corner = getDimensionPixelSize(R.styleable.MultiScheduleView_corner, 0).toFloat()
                colorColumn = getColor(R.styleable.MultiScheduleView_colorColumn, 0)
                sizeDrawable = getDimensionPixelSize(R.styleable.MultiScheduleView_sizeDrawable, 0).toFloat()
                marginIcon = getDimensionPixelSize(R.styleable.MultiScheduleView_marginIcon, 0).toFloat()
                drawableGoal = getDrawable(R.styleable.MultiScheduleView_drawableGoal)
                drawableReminder = getDrawable(R.styleable.MultiScheduleView_drawableReminder)
            } finally {
                recycle()
            }
        }
        // Создаем bitmap.
        bitmapGoal = drawableGoal?.toBitmap(sizeDrawable.toInt(), sizeDrawable.toInt())
        bitmapReminder = drawableReminder?.toBitmap(sizeDrawable.toInt(), sizeDrawable.toInt())

        // Сглаживание.
        mPaint.isAntiAlias = true

        // Таймер.
        val run = Runnable {
            val time = (System.currentTimeMillis()+TimeZone.getDefault().getOffset(System.currentTimeMillis()))
            mMustActionTypeListener?.getMustActionType(getMustActionType(time))
        }
        mTimer.postDelayed(run, 60*1000L)
    }

    /*  Интерфейс, который сообщает, что завершена обработка данных.  */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    /*  Интерфейс, который сообщает о типе действии, которое нужно выполнить.  */
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
     * в котором находится это расписание.
     * @param actionsSchedule массив с действиями в расписании.
     */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>){
        Executors.newSingleThreadExecutor().execute {
            mCount = 1

            val rawPoints = mutableListOf<Point>()
            // У каждого действия может быть только один интервал. Связываем id и интервал.
            val intervals = mutableMapOf<String, Interval>()

            actionsSchedule.forEach{
                val color = mRepository.getColor(it.actionTypeId)
                var start = it.startTime/(24f*60*60*1000)
                var end = it.endTime/(24f*60*60*1000)

                // Возможно, что действие выйдет за пределы текущего дня.
                if (start < 0f ) start = 0f
                if (end > 1f ) end = 1f

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id,false, end, color))
                intervals[it.id] = Interval(color, it.actionTypeId, start, end, it.scheduleID)
            }
            val points = rawPoints.sortedBy { it.coordinate }

            // Типы действий с временем начала и конца.
            val actionTypesTime = mutableListOf<ActionTypeTime>()
            var actionTypeTime = ActionTypeTime()

            // Нужно, чтобы  действия из одного расписания имели один индекс.
            // Естественно, это при условии, что само расписание не имеет пересечений.
            // Словарь типа id расписания: номер столбца.
            var activeSchedules = mutableMapOf<String, Int>()
            // Это номер последного задейственного столбца.
            var index = 0

            // ID действия в первом столбце. Нужно, чтобы добавить его в секцию, если
            // оно пересекается с другим действием.
            var first: String
            // Список активных действий, то есть тех, которые есть в данный момент времени.
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    // Если это начало, то добавляем id в список активных id.
                    active.add(point.id)

                    if (active.size == 1) {
                        first = point.id

                        // Начали одно новое действие.
                        actionTypeTime.actionTypeID = intervals[first]!!.actionTypeID
                        actionTypeTime.start = point.coordinate
                    }

                    // Если все-таки находим пересечение, то нужно не забыть
                    // про первое действие в этой секции (которое могло просто существовать
                    // без персечений).
                    if (active.size == 2) {
                        // Устанавливаем конец действия в начале секции.
                        // Так как при пересечении неизвестно, что делать.
                        actionTypeTime.end = point.coordinate
                        actionTypesTime.add(actionTypeTime)
                        actionTypeTime = ActionTypeTime()

                        // Указываем, что тут пересечение, и нельзя определить тип действия, который
                        // нужно выполнить.
                        actionTypeTime.actionTypeID = null
                        actionTypeTime.start = point.coordinate
                    }

                    if (active.size > 1) {
                        val scheduleID = intervals[point.id]!!.scheduleID
                        if (scheduleID !in activeSchedules) {
                            // Если такого расписания еще нет в секции, то добавляем
                            // новый столбец.
                            index += 1
                            intervals[point.id]!!.index = index
                            activeSchedules[scheduleID] = index
                        } else intervals[point.id]!!.index = activeSchedules[scheduleID]!!

                        if (index+1 > mCount) mCount = index+1
                    }
                } else {
                    active.remove(point.id)

                    // Если сейчас только одно активное действие, то мы только что
                    // вышли из пересечения.
                    if (active.size == 1) {
                        // Готовимся к следующей части.
                        first = active[0]
                        index = 0
                        activeSchedules = mutableMapOf()

                        // Конец пересечения.
                        actionTypeTime.end = point.coordinate
                        actionTypesTime.add(actionTypeTime)
                        actionTypeTime = ActionTypeTime()

                        // И начало нового действия. Так как теперь можно определить,
                        // какое действие необходимо выполнить.
                        actionTypeTime.actionTypeID = intervals[first]!!.actionTypeID
                        actionTypeTime.start = point.coordinate
                    }

                    // Если нет активный действий, то мы закончили действие без пересечений
                    // (с учетом того, что начало действия моглы было в конце пересечения).
                    if (active.size == 0) {
                        actionTypeTime.end = point.coordinate
                        actionTypesTime.add(actionTypeTime)
                        actionTypeTime = ActionTypeTime()
                    }
                }
            }

            // Составляем объекты для рисования.
            val actionDrawables = mutableListOf<ActionDrawable>()
            intervals.toList().forEach {
                val interval = it.second

                val left = interval.index.toFloat()
                val right = left+1

                actionDrawables.add(
                    ActionDrawable(interval.color, interval.start, interval.end, left, right)
                )
            }

            mActionDrawables = actionDrawables
            mActionTypesTime = actionTypesTime

            // Это нужно сделать, так как мы не в основном потоке.
            mHandler.post{ requestLayout() }
            mHandler.post{ mFinishedListener?.finish() }
            mHandler.post{
                val time = (System.currentTimeMillis()+TimeZone.getDefault().getOffset(System.currentTimeMillis()))/1000
                mMustActionTypeListener?.getMustActionType(getMustActionType(time))
            }
        }
    }

    fun setGoalsTimes(goalsTimes: List<Long>){
        mGoalsTimes = goalsTimes.map{ it/(1000*60*60*24f) }
        requestLayout()
    }

    fun setRemindersTimes(remindersTimes: List<Long>){
        mRemindersTimes = remindersTimes.map{ it/(1000*60*60*24f) }
        requestLayout()
    }

    /**
     * Возвращает id типа действия, которое нужно сейчас выполнить.
     * "" - ничего
     * null - ошибка.
     */
    private fun getMustActionType(time: Long): String? {
        val timeDay = time%(24*60*60*1000)
        val coordinate = timeDay/(24f*60*60*1000)
        val actionType = mActionTypesTime.firstOrNull { coordinate >= it.start && coordinate <= it.end }

        return if (actionType == null) ""
        else actionType.actionTypeID
    }

    /**
     * Изменят только ширину виджета в зависимости от mCount.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        val desiredWidth = (stripWidth*mCount+spaceWidth*(mCount-1)+sizeDrawable+marginIcon).toInt()

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
            (stripWidth+spaceWidth)-spaceWidth, height, corner, corner, mPaint)

        mActionDrawables.forEach {
            mPaint.color = it.color
            canvas?.drawRoundRect((stripWidth+spaceWidth)*it.left, height*it.start,
                (stripWidth+spaceWidth)*it.right-spaceWidth, height*it.end, corner, corner, mPaint)
        }

        mGoalsTimes.forEach {
            mPaint.color = Color.BLACK
            bitmapGoal?.also { bitmap ->
                canvas?.drawBitmap(bitmap, (stripWidth+spaceWidth)*mCount-spaceWidth+marginIcon,
                    height*it-sizeDrawable/2, mPaint)
            }
            canvas?.drawLine(0f, height*it,
                (stripWidth+spaceWidth)*mCount-spaceWidth, height*it, mPaint)
        }

        mRemindersTimes.forEach {
            mPaint.color = Color.BLACK
            bitmapReminder?.also { bitmap ->
                canvas?.drawBitmap(bitmap, (stripWidth+spaceWidth)*mCount-spaceWidth+marginIcon,
                    height*it-sizeDrawable/2, mPaint)
            }
            canvas?.drawLine(0f, height*it,
                (stripWidth+spaceWidth)*mCount-spaceWidth, height*it, mPaint)
        }
    }
}

/**
 * Очень похож на класс ScheduleView, за тем исключением, что находит только индексы у Interval.
 *
 * Используется для визуализации тайм трекинга.
 */
class ActionsView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var stripWidth: Float
    private var spaceWidth: Float
    private var corner: Float
    private var colorColumn: Int
    private var mCount: Int = 1
    private val mPaint: Paint = Paint()
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * Репозиторий для связи с базой данных типов действий. Необходим для получения цвета.
     * Получает не LiveData, а напрямую значение, поэтому наблюдателей нет.
     */
    private val mRepository = ActionTypeRepository.get()

    /**
     * Массив действий, которые не нужно никак обрабатывать и можно сразу рисовать.
     */
    private var mActionDrawables: List<ActionDrawable> = emptyList()


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

    /*  Интерфейс, который сообщает, что завершена обработка данных.  */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
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
     * свободный столбец, в котором можно его нарисовать.
     * @param actions массив с действиями. Время НЕ локальное.
     * @param local отклонение от местного времени
     * @param day локальный день, от начало которого нужно отсчитывать.
     */
    fun setActions(actions: List<Action>, local: Int, day: Long){
        Executors.newSingleThreadExecutor().execute {
            mCount = 1

            val rawPoints = mutableListOf<Point>()
            // У каждого действия может быть только один интервал. Связываем id и интервал.
            val intervals = mutableMapOf<String, Interval>()

            val startDay = day*24*60*60*1000
            actions.forEach{
                val color = mRepository.getColor(it.actionTypeId)
                var start = (it.startTime+local-startDay)/(24f*60*60*1000)
                var end = (it.endTime+local-startDay)/(24f*60*60*1000)

                // На всякий случай.
                if (start < 0f && end < 0f) return@forEach
                if (start > 1f && end > 1f) return@forEach
                // Если мы вышли за пределы, то значит действие начинается
                // или заканчивается в другом дне.
                if (start < 0f) start = 0f
                if (end > 1f) end = 1f

                rawPoints.add(Point(it.id, true, start, color))
                rawPoints.add(Point(it.id, false, end, color))
                intervals[it.id] = Interval(color, start, end)
            }
            val points = rawPoints.sortedBy { it.coordinate }

            // Массив id действий, которые сейчас активны в столбце с номер, равным индексу.
            // Если id='', то это свободный столбец. По умолчанию всегда есть один столбец.
            val columns = mutableListOf("")
            // Это номер последного задейственного столбца.
            var index = 0

            // Список активных действий, то есть тех, которые есть в данный момент времени.
            val active = mutableListOf<String>()
            points.forEach { point ->
                if (point.isStart) {
                    // Если это начало, то добавляем id в список активных id.
                    active.add(point.id)

                    // Если сейчас только одно активное действие, то даем ему первый столбец.
                    if (active.size == 1) columns[0] = point.id
                    // Если больше одного, то действия пересекаются.
                    if (active.size > 1) {
                        // Находим свободный индекс.
                        val freeIndex = columns.indexOfFirst { it == "" }
                        if (freeIndex == -1){
                            // Если не нашли, то ставим новый столбец.
                            index += 1
                            columns.add(point.id)
                            intervals[point.id]!!.index = index
                        } else {
                            intervals[point.id]!!.index = freeIndex
                            columns[freeIndex] = point.id
                        }
                        if (index+1 > mCount) mCount = index+1
                    }
                } else {
                    active.remove(point.id)
                    columns[columns.indexOfFirst { it == point.id }] = ""
                }
            }

            // Составляем объекты для рисования.
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

            mActionDrawables = actionDrawables

            // Это нужно сделать, так как мы не в основном потоке.
            mHandler.post{ requestLayout() }
            mHandler.post{ mFinishedListener?.finish() }
        }
    }

    /**
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
 * в начале циферблата. Показываем 24 часа.
 */
class ClockFaceView(context: Context, attrs: AttributeSet): View(context, attrs){
    private var textSize: Float

    /**
     * Расстояние между двумя цифрами на циферблате.
     */
    private var spaceHeight: Int

    /**
     * Верхний отступ текста от штриха.
     */
    private var marginText: Int

    private var mPaint: Paint = Paint()
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * Время начала на цифрблате.
     */
    private var startTime: Long = 0

    /**
     * Массив с текстом часа и его координатой.
     * Нужен для единоразового расчета расположения цифр.
     */
    private var hours: List<Hour> = emptyList()

    /**
     * Иниализация виджета. Получение параметров из xml.
     */
    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ClockFaceView,
            0, 0).apply {
            try {
                textSize = getDimensionPixelSize(R.styleable.ClockFaceView_textSize, 0).toFloat()
                spaceHeight = getDimensionPixelSize(R.styleable.ClockFaceView_spaceHeight, 0)
                marginText = getDimensionPixelSize(R.styleable.ClockFaceView_marginText, 0)
            } finally {
                recycle()
            }
        }

        mPaint.color = Color.BLACK
        mPaint.textSize = textSize
        mPaint.isAntiAlias = true

        calculateHours()
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
    private fun calculateHours(){
        val hours = mutableListOf<Hour>()
        for (i in 0..23){
            val text = if (i < 10) "0$i:00"
            else "$i:00"

            val width = mPaint.measureText(text)

            var y = i/24f
            y -= startTime/(1000f*60*60*24)
            if (y < 0) y += 1

            hours.add(Hour(text, width, y))
        }
        this.hours = hours
        mHandler.post{ requestLayout() }
    }

    /**
     * Изменят только вызоту виджета в зависимости от заданных параметров.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        val desiredHeight = ((textSize+spaceHeight+textSize)*24).toInt()

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

        hours.forEach {
            canvas?.drawText(it.text, (width-it.width)/2, height*it.y+textSize+marginText, mPaint)
            canvas?.drawLine(0f, height*it.y, width , height*it.y, mPaint)

            var position = it.y+1/48f
            if (position > 1f) position -= 1f
            canvas?.drawLine(width/4f, height*position, 3*width/4f, height*position, mPaint)

            position = it.y+1/(2*48f)
            if (position > 1f) position -= 1f
            canvas?.drawLine(3*width/8f, height*position, 5*width/8f, height*position, mPaint)

            position = it.y+3/(2*48f)
            if (position > 1f) position -= 1f
            canvas?.drawLine(3*width/8f, height*position, 5*width/8f, height*position, mPaint)
        }
    }
}

/**
 * ViewGroup. Соединение виджетов выше с ScrollView. Используется в DayScheduleFragment.
 * Высота определяется ClockFaceView.
 */
class ScheduleClockView(context: Context, attrs: AttributeSet): ScrollView(context, attrs) {
    private val scheduleView: ScheduleView
    private val clockFaceView: ClockFaceView


    init {
        inflate(context, R.layout.layout_clock_schedule, this)
        isVerticalScrollBarEnabled = false

        scheduleView = findViewById(R.id.scheduleView)
        scheduleView.setFinishedListener{ mFinishedListener?.finish() }
        clockFaceView = findViewById(R.id.clockFaceView)
    }

    /*  Интерфейс, который сообщает, что заверешна обработка данных.  */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    /*  Установка данных для ScheduleView.  */
    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>){
        scheduleView.setActionsSchedule(actionsSchedule)
    }
}

/**
 * ViewGroup. Соединение виджетов выше с ScrollView. Используется в DayFragment.
 * Высота определяется ClockFaceView.
 */
class DayClockView(context: Context, attrs: AttributeSet): ScrollView(context, attrs) {
    private val multiScheduleView: MultiScheduleView
    private val clockFaceView: ClockFaceView


    init {
        inflate(context, R.layout.layout_clock_day, this)
        isVerticalScrollBarEnabled = false

        clockFaceView = findViewById(R.id.clockFaceView)

        multiScheduleView = findViewById(R.id.multiScheduleView)
        multiScheduleView.setFinishedListener{ mFinishedListener?.finish() }
        multiScheduleView.setMustActionTypeListener{ mMustActionTypeListener?.getMustActionType(it) }
    }

    /*  Интерфейс, который сообщает, что заверешна обработка данных.  */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    /*  Интерфейс, который сообщает о типе действии, которое нужно выполнить.  */
    private var mMustActionTypeListener: MustActionTypeListener? = null
    fun interface MustActionTypeListener{
        fun getMustActionType(id: String?)
    }
    fun setMustActionTypeListener(mustActionTypeListener: MustActionTypeListener){
        mMustActionTypeListener = mustActionTypeListener
    }


    fun setActionsSchedule(actionsSchedule: List<ActionSchedule>){
        multiScheduleView.setActionsSchedule(actionsSchedule)
    }

    fun setGoalsTimes(goalsTimes: List<Long>){
        multiScheduleView.setGoalsTimes(goalsTimes)
    }

    fun setRemindersTimes(remindersTimes: List<Long>){
        multiScheduleView.setRemindersTimes(remindersTimes)
    }
}


/**
 * ViewGroup. Соединение виджетов выше с ScrollView. Используется в TimeTrackerFragment.
 * Высота определяется ClockFaceView.
 */
class TimeTrackerClockView(context: Context, attrs: AttributeSet): ScrollView(context, attrs) {
    private val actionsView: ActionsView
    private val clockFaceView: ClockFaceView

    init {
        inflate(context, R.layout.layout_clock_time_tracker, this)
        isVerticalScrollBarEnabled = false

        clockFaceView = findViewById(R.id.clockFaceView)
        actionsView = findViewById(R.id.actionsView)
        actionsView.setFinishedListener{ mFinishedListener?.finish() }
    }

    /*  Интерфейс, который сообщает, что заверешна обработка данных.  */
    private var mFinishedListener: FinishedListener? = null
    fun interface FinishedListener{
        fun finish()
    }
    fun setFinishedListener(finishedListener: FinishedListener){
        mFinishedListener = finishedListener
    }

    fun setActions(actions: List<Action>, local: Int, day: Long){
        actionsView.setActions(actions, local, day)
    }
}
