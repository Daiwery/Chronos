/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 31.08.2021. Последний день лета :(
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: полное изменение логики взаимодействия с базой данных.
*
* Дата изменения: 08.09.2021. Последний день лета :(
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменен 3 этап наблюдения и добавлен 4 этап наблюдения.
*
* Дата изменения: 11.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: удаление таблицы с днями в расписании. Измения схема наблюдения: теперь мы
* сразу получаем действия из базы данных. Без промежуточных этапов. Вся логика теперь в SQLite.
*
* Дата изменения: 24.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: наследование от ClockViewModel.
*/

package com.daiwerystudio.chronos.ui.day

import android.icu.util.TimeZone
import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.ui.ClockViewModel
import com.daiwerystudio.chronos.ui.union.ID
import com.daiwerystudio.chronos.ui.widgets.ActionsView
import java.util.concurrent.Executors

class DayViewModel: ClockViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mActionTypeRepository = ActionTypeRepository.get()
    private val mGoalRepository = GoalRepository.get()
    private val mReminderRepository = ReminderRepository.get()
    private val mUnionRepository = UnionRepository.get()
    val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())
    var isAnimated: Boolean = false

    /**
     * Специальный класс, необходимый для адаптера.
     */
    data class Section(
        val data: List<Pair<ActionSchedule, ActionType?>>
    )

    // Локальный день.
    val day: MutableLiveData<Long> = MutableLiveData()

    // Массив с целями.
    private val mGoals: LiveData<List<Goal>> = Transformations.switchMap(day){
        mIsReceivedGoals = false
        // Нужно не забыть перевести время из локального в глобальное.
        mGoalRepository.getGoalsFromTimeInterval(it*1000*60*60*24-local,
            (it+1)*1000*60*60*24-local)
    }
    private var mIsReceivedGoals: Boolean = false

    // Массив с напоминаниями.
    private val mReminders: LiveData<List<Reminder>> = Transformations.switchMap(day){
        mIsReceivedReminders = false
        // Нужно не забыть перевести время из локального в глобальное.
        mReminderRepository.getRemindersFromTimeInterval(it*1000*60*60*24-local,
            (it+1)*1000*60*60*24-local)
    }
    private var mIsReceivedReminders: Boolean = false

    /*  Получение активных расписаний.  */
    val mActionsSchedule: LiveData<List<ActionSchedule>> = Transformations.switchMap(day) {
        mIsReceivedSections = false
        mScheduleRepository.getActionsScheduleFromDay(it, local)
    }

    /*  Обрабатываем действия.  */
    private val mActionSections: LiveData<List<ActionSection>> =
        Transformations.switchMap(mActionsSchedule) { actionsSchedule ->
            val liveActionSections = MutableLiveData<List<ActionSection>>()

            Executors.newSingleThreadExecutor().execute {
                val actionIntervals = mutableListOf<ActionInterval>()
                actionsSchedule.forEach {
                    actionIntervals.add(ActionInterval(it.id, it.actionTypeID, it.startTime, it.endTime))
                }
                liveActionSections.postValue(processingActionIntervals(actionIntervals))
            }

            liveActionSections
        }

    /*  Получаем типы действий, которые используются.  */
    private val mActionTypes: LiveData<List<ActionType>> =
        Transformations.switchMap(mActionSections) { actionSections ->
            val ids = mutableListOf<String>()
            actionSections.forEach { actionSection ->
                actionSection.intervals.forEach {
                    if (it.actionTypeID !in ids) ids.add(it.actionTypeID)
                }
            }

            mActionTypeRepository.getActionTypes(ids)
        }

    /*  Формируем секции с действиями и типами, с которыми они связаны.  */
    private val mSections: LiveData<List<Section>> =
        Transformations.switchMap(mActionTypes) { actionTypes ->
            val sections = mutableListOf<Section>()
            mActionSections.value!!.forEach { actionSection ->
                val data = mutableListOf<Pair<ActionSchedule, ActionType?>>()
                actionSection.intervals.forEach { actionInterval ->
                    val actionSchedule = mActionsSchedule.value!!.first { it.id == actionInterval.id }
                    val actionType = actionTypes.firstOrNull { it.id == actionSchedule.actionTypeID }
                    data.add(Pair(actionSchedule, actionType))
                }
                sections.add(Section(data))
            }

            MutableLiveData(sections)
        }
    private var mIsReceivedSections: Boolean = false

    /*  Формируем объекты для рисования.   */
    val actionDrawables: LiveData<List<ActionsView.ActionDrawable>> =
        Transformations.switchMap(mActionTypes) { actionTypes ->
            val actionDrawables = mutableListOf<ActionsView.ActionDrawable>()
            mActionSections.value!!.forEach { actionSection ->
                actionSection.intervals.forEach { actionInterval ->
                    val actionSchedule = mActionsSchedule.value!!.first { it.id == actionInterval.id }
                    val actionType = actionTypes.firstOrNull { it.id == actionSchedule.actionTypeID }

                    val color = actionType?.color ?: 0
                    val start = actionInterval.start/(24f*60*60*1000)
                    val end = actionInterval.end/(24f*60*60*1000)
                    val left = actionInterval.index.toFloat()
                    val right = left+1

                    actionDrawables.add(ActionsView.ActionDrawable(color, start, end, left, right))
                }
            }

            MutableLiveData(actionDrawables)
        }

    /*  Соединяем все в один список.  */
    val data: MediatorLiveData<List<Pair<Int, Any>>> = MediatorLiveData()
    init {
        data.addSource(mGoals){
            mIsReceivedGoals = true

            if (mIsReceivedGoals && mIsReceivedReminders && mIsReceivedSections)
                data.value = buildData()
        }
        data.addSource(mReminders){
            mIsReceivedReminders = true

            if (mIsReceivedGoals && mIsReceivedReminders && mIsReceivedSections)
                data.value = buildData()
        }
        data.addSource(mSections){
            mIsReceivedSections = true

            if (mIsReceivedGoals && mIsReceivedReminders && mIsReceivedSections)
                data.value = buildData()
        }
    }

    private fun buildData(): List<Pair<Int, Any>> {
        val newData = mutableListOf<Pair<Int, Any>>()
        // Квазисортировка по типу.
        mSections.value?.also { sections ->
            newData.addAll(sections.map { Pair(TYPE_SECTION, it) })
        }
        mGoals.value?.also { goals ->
            newData.addAll(goals.map { Pair(TYPE_GOAL, it) })
        }
        mReminders.value?.also { reminders ->
            newData.addAll(reminders.map { Pair(TYPE_REMINDER, it) })
        }
        newData.sortBy {
            when (it.first){
                TYPE_SECTION -> (it.second as Section).data.first().first.startTime+day.value!!*1000*60*60*24-local
                TYPE_GOAL -> (it.second as Goal).deadline
                TYPE_REMINDER -> (it.second as Reminder).time
                else -> throw IllegalArgumentException("Invalid type")
            }
        }

        return newData
    }

    // Функция для алгоритма обработки действий.
    private lateinit var mActiveSchedules: MutableMap<Int, String>  // Словарь типа: номер столбца - id расписания.
    override fun getIndexForInterval(point: ActionPoint, columns: List<String>): Int {
        // Не забываем про первое действие, которое могло существовать без пересечений.
        if (columns.size == 1) {
            mActiveSchedules = mutableMapOf()
            mActiveSchedules[0] = mActionsSchedule.value!!.first { it.id == columns[0] }.scheduleID
        }

        // Находим свободные столбцы.
        val freeIndexes = columns.mapIndexed { index, s -> if (s == "") index else -1 }.filter { it != -1 }
        // Находим свободный столбец, в котором сейчас расписание текущего действия.
        // Если не нашли, то reeIndex = null.
        var freeIndex: Int? = null
        freeIndexes.forEach { index ->
            if (freeIndex == null)
                if (index in mActiveSchedules)
                    if (mActiveSchedules[index] == mActionsSchedule.value!!.first { it.id == point.id }.scheduleID)
                        freeIndex = index
        }
        // Если не нашли, то ставим новый столбец.
        return if (freeIndex == null) {
            mActiveSchedules[columns.size] = mActionsSchedule.value!!.first { it.id == point.id }.scheduleID
            columns.size
        } else freeIndex!!
    }

    fun updateGoal(goal: Goal){
        mGoalRepository.updateGoal(goal)
    }

    fun deleteItem(position: Int){
        val item = data.value!![position]
        when (item.first) {
            TYPE_GOAL -> {
                mUnionRepository.deleteUnionWithChild((item.second as Goal).id)
                mGoalRepository.deleteGoal(item.second as Goal)
            }
            TYPE_REMINDER -> {
                mUnionRepository.deleteUnionWithChild((item.second as Reminder).id)
                mReminderRepository.deleteReminder(item.second as Reminder)
            }
        }
    }

    fun deleteItems(positions: List<Int>){
        val items = data.value!!.filterIndexed { index, _ -> index in positions }
        mUnionRepository.deleteUnionsWithChild(items.map { (it.second as ID).id })

        val goals = items.filter { it.first == TYPE_GOAL }
        mGoalRepository.deleteGoals(goals.map { (it.second as ID).id })

        val reminders = items.filter { it.first == TYPE_REMINDER }
        mReminderRepository.deleteReminders(reminders.map { (it.second as ID).id })
    }

    fun changeTimeItems(positions: List<Int>, delta: Long){
        val items = data.value!!.filterIndexed { index, _ -> index in positions }

        val goals = items.filter { it.first == TYPE_GOAL }
            .map { (it.second as Goal).copy().apply { deadline += delta } }
        mGoalRepository.updateGoals(goals)

        val reminders = items.filter { it.first == TYPE_REMINDER }
            .map { (it.second as Reminder).copy().apply { time += delta } }
        mReminderRepository.updateReminders(reminders)
    }

    companion object {
        const val TYPE_SECTION = 0
        const val TYPE_REMINDER = 1
        const val TYPE_GOAL = 2
    }
}