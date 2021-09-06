/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 31.08.2021. Последний день лета :(
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: полное изменение логики взаимодействия с базой данных.
*/

package com.daiwerystudio.chronos.ui.day

import android.icu.util.TimeZone
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.ui.union.ID
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Схема наблюдения следующая. Начальная точка - это номер дня day. На него подписан mActiveSchedules
 * - активные расписания. На mActiveSchedules подписан mDaysSchedule, который получает список дней,
 * активных сегодня. Предупреждение: он не имеет подписку на базу данных.
 * На mDaysSchedule подписан actionsSchedule, который получает список типов действий.
 *
 * Также на day подписаны две LiveData: mLiveGoals и mLiveReminders, которые объединяются
 * с помощью MediatorLiveData.
 */
class DayViewModel: ViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mGoalRepository = GoalRepository.get()
    private val mReminderRepository = ReminderRepository.get()
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


    /*  Первый этап наблюдения. Получение активных расписаний.  */
    private val mActiveSchedules: LiveData<List<Schedule>> = Transformations.switchMap(day) {
        mScheduleRepository.getActiveSchedules()
    }

    /*  Второй этап наблюдения. Получение дней в расписании, которые активны сегодня.  */
    // Дни из базы данных получаются на прямую, а не через LiveData. Поэтому подписки на изменения
    // базы данных нет. Но она и не нужна, так как база данных дней сама по себе меняться не может.
    private val mDaysSchedule: LiveData<List<DaySchedule>> =
        Transformations.switchMap(mActiveSchedules) { schedules ->
            val liveDaysSchedule =  MutableLiveData<List<DaySchedule>>()

            mExecutor.execute {
                val daysSchedule = mutableListOf<DaySchedule>()
                schedules.forEach {
                    when (it.type) {
                        TYPE_SCHEDULE_ONCE -> {
                            if ((it.start+local)/(1000*60*60*24) == day.value)
                                daysSchedule.add(mScheduleRepository.getDaySchedule(it.id, 0))
                        }
                        TYPE_SCHEDULE_PERIODIC -> {
                            val dayIndex = (day.value!!-(it.start+local)/(1000*60*60*24)).toInt()
                            // Сначала берем нынешний день.
                            // abs нужен для ситуации, когда dayIndex<0.
                            var daySchedule = mScheduleRepository.getDaySchedule(it.id, abs(dayIndex)%it.countDays)
                            if (!daySchedule.isCorrupted)
                                daysSchedule.add(daySchedule)
                            // А после берем прошлый день, так как действия из прошлого "дня" могут
                            // быть ночью этого.
                            daySchedule = mScheduleRepository.getDaySchedule(it.id, abs((dayIndex-1))%it.countDays)
                            if (!daySchedule.isCorrupted)
                                daysSchedule.add(mScheduleRepository.getDaySchedule(it.id, abs((dayIndex-1))%it.countDays))
                        }
                        else -> throw IllegalArgumentException("Invalid type")
                    }
                }
                liveDaysSchedule.postValue(daysSchedule)
            }

            liveDaysSchedule
        }

    /*  Третий этап наблюдения. Получение действий в расписании.  */
    val actionsSchedule: LiveData<List<ActionSchedule>> =
        Transformations.switchMap(mDaysSchedule){ daysSchedule ->
            mScheduleRepository.getActionsScheduleFromDaysIDs(daysSchedule.map{ it.id })
        }


    /*                        Доп. функции                        */
    fun updateGoal(goal: Goal){
        mGoalRepository.updateGoal(goal)
    }

    fun deleteItem(position: Int){
        val item = data.value!![position]
        when(item.first){
            TYPE_GOAL -> mGoalRepository.deleteGoal(item.second as Goal)
            TYPE_REMINDER -> mReminderRepository.deleteReminder(item.second as Reminder)
            else -> throw IllegalArgumentException("Invalid type")
        }
    }
}