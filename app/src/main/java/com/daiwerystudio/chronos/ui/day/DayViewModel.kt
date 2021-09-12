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
*/

package com.daiwerystudio.chronos.ui.day

import android.icu.util.TimeZone
import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.ui.union.ID
import java.util.concurrent.Executors

/**
 * Схема наблюдения следующая. Начальная точка - это номер дня day. На него подписан mActiveSchedules
 * - активные расписания. На mActiveSchedules подписан actionSchedule, который получает действия,
 * которые активны сегодня.
 *
 * Также на day подписаны две LiveData: mLiveGoals и mLiveReminders, которые объединяются
 * с помощью MediatorLiveData.
 */
class DayViewModel: ViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mGoalRepository = GoalRepository.get()
    private val mReminderRepository = ReminderRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private val mExecutor = Executors.newSingleThreadExecutor()
    val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())

    // Локальный день.
    val day: MutableLiveData<Long> = MutableLiveData()

    // Массив с целями.
    private val mLiveGoals: LiveData<List<Goal>> = Transformations.switchMap(day){
        // Нужно не забыть перевести время из локального в глобальное.
        mGoalRepository.getGoalsFromTimeInterval(it*1000*60*60*24-local,
            (it+1)*1000*60*60*24-local)
    }
    private var mGoals: List<Goal> = emptyList()

    // Массив с напоминаниями.
    private val mLiveReminders: LiveData<List<Reminder>> = Transformations.switchMap(day){
        // Нужно не забыть перевести время из локального в глобальное.
        mReminderRepository.getRemindersFromTimeInterval(it*1000*60*60*24-local,
            (it+1)*1000*60*60*24-local)
    }
    private var mReminders: List<Reminder> = emptyList()

    /*  Соединяем массив с целями и напоминаниями.  */
    val data: MediatorLiveData<List<Pair<Int, ID>>> = MediatorLiveData()
    init {
        data.addSource(mLiveGoals){ goals ->
            mGoals = goals

            val newData = mutableListOf<Pair<Int, ID>>()
            newData.addAll(mReminders.map { Pair(TYPE_REMINDER, it) })
            newData.addAll(mGoals.map { Pair(TYPE_GOAL, it) })
            newData.sortBy {
                when (it.first){
                    TYPE_GOAL -> (it.second as Goal).deadline
                    TYPE_REMINDER -> (it.second as Reminder).time
                    else -> throw IllegalArgumentException("Invalid type")
                }
            }
            data.value = newData
        }
        data.addSource(mLiveReminders){ reminders ->
            mReminders = reminders

            val newData = mutableListOf<Pair<Int, ID>>()
            newData.addAll(mReminders.map { Pair(TYPE_REMINDER, it) })
            newData.addAll(mGoals.map { Pair(TYPE_GOAL, it) })
            newData.sortBy {
                when (it.first){
                    TYPE_GOAL -> (it.second as Goal).deadline
                    TYPE_REMINDER -> (it.second as Reminder).time
                    else -> throw IllegalArgumentException("Invalid type")
                }
            }
            data.value = newData
        }
    }

    /*  Получение активных расписаний.  */
    val actionsSchedule: LiveData<List<ActionSchedule>> = Transformations.switchMap(day) {
        mScheduleRepository.getActionsScheduleFromDay(it, local)
    }


    /*                        Доп. функции                        */
    fun updateGoal(goal: Goal){
        mGoalRepository.updateGoal(goal)
    }

    fun deleteItem(position: Int){
        val item = data.value!![position]
        when(item.first){
            TYPE_GOAL -> {
                mUnionRepository.deleteUnionWithChild(item.second.id)
                mGoalRepository.deleteGoal(item.second as Goal)
            }
            TYPE_REMINDER -> {
                mUnionRepository.deleteUnionWithChild(item.second.id)
                mReminderRepository.deleteReminder(item.second as Reminder)
            }
            else -> throw IllegalArgumentException("Invalid type")
        }
    }
}