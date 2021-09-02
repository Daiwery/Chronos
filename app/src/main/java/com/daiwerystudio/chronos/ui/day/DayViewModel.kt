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
import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors

/**
 * Схема наблюдения следующая. Начальная точка - это номер дня. На него подписаны две LiveData:
 * actions - действия в этом дне, и mActiveSchedules - активные расписания. На mActiveSchedules
 * подписан mDaysSchedule, который получает список дней, активных сегодня. Предупреждение:
 * он не имеет подписку на базу данных. На mDaysSchedule подписан actionsSchedule, который
 * получает список типов действий.
 */
class DayViewModel: ViewModel() {
    private val mActionRepository = ActionRepository.get()
    private val mScheduleRepository = ScheduleRepository.get()
    private val mExecutor = Executors.newSingleThreadExecutor()
    val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())

    // Локальный день.
    val day: MutableLiveData<Long> = MutableLiveData()

    /*  Получение действий в этом дне. */
    val actions: LiveData<List<Action>> = Transformations.switchMap(day) {
        // Нужно не забыть перевести время из локального в глобальное.
        mActionRepository.getActionsFromInterval(it*1000*60*60*24-local,
            (it+1)*1000*60*60*24-local)
    }

    /*  Первый этап наблюдения. Получение активных расписаний.  */
    private val mActiveSchedules: LiveData<List<Schedule>> = Transformations.switchMap(day) {
        // Расписание показывается, только когда мы в настоящем или будущем.
        if (it-(System.currentTimeMillis()+local)/(1000*60*60*24) >= 0)
            mScheduleRepository.getActiveSchedules()
        else MutableLiveData()
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
                            daysSchedule.add(mScheduleRepository.getDaySchedule(it.id, dayIndex%it.countDays))
                            // А после берем прошлый день, так как действия из прошлого "дня" могут
                            // быть ночью этого.
                            daysSchedule.add(mScheduleRepository.getDaySchedule(it.id, (dayIndex-1)%it.countDays))
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


    fun deleteAction(action: Action){
        mActionRepository.deleteAction(action)
    }

    private val mActionTypeRepository = ActionTypeRepository.get()
    fun getActionType(id: String): LiveData<ActionType> = mActionTypeRepository.getActionType(id)
}