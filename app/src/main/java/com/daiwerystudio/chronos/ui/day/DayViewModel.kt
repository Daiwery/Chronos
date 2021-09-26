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
import com.daiwerystudio.chronos.ui.widgets.ActionsView
import java.util.concurrent.Executors

class DayViewModel: ClockViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mActionTypeRepository = ActionTypeRepository.get()
    private val mGoalRepository = GoalRepository.get()
    private val mReminderRepository = ReminderRepository.get()
    val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())

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
        mSections.value?.also { sections ->
            newData.addAll(sections.map { Pair(TYPE_SECTION, it) })
        }
        mReminders.value?.also { reminders ->
            newData.addAll(reminders.map { Pair(TYPE_REMINDER, it) })
        }
        mGoals.value?.also { goals ->
            newData.addAll(goals.map { Pair(TYPE_GOAL, it) })
        }
        newData.sortBy {
            when (it.first){
                TYPE_SECTION -> (it.second as Section).data.first().first.startTime
                TYPE_GOAL -> (it.second as Goal).deadline
                TYPE_REMINDER -> (it.second as Reminder).time
                else -> throw IllegalArgumentException("Invalid type")
            }
        }

        return newData
    }

    // Функция для алгоритма обработки действий.
    override fun getIndexForInterval(columns: List<String>): Int {
        // Находим свободный индекс.
        val freeIndex = columns.indexOfFirst { it == "" }
        // Если не нашли, то ставим новый столбец.
        return if (freeIndex == -1) columns.size else freeIndex
    }

    fun updateGoal(goal: Goal){
        mGoalRepository.updateGoal(goal)
    }

    companion object {
        const val TYPE_SECTION = 0
        const val TYPE_REMINDER = 1
        const val TYPE_GOAL = 2
    }
}