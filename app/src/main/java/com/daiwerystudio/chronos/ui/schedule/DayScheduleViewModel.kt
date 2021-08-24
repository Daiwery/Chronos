/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 23.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавление логики взаимодействия с DaySchedule.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import java.util.concurrent.Executors

class DayScheduleViewModel : ViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mExecutor = Executors.newSingleThreadExecutor()

    val dayScheduleID: MutableLiveData<String> = MutableLiveData()

    val daySchedule: LiveData<DaySchedule> =
        Transformations.switchMap(dayScheduleID) { mScheduleRepository.getDaySchedule(it) }

    // В случае абсолютного расписания значения сразу можно использоваться, но в случае
    // относительного нужно посчитать start и end.
    private val rawActionsSchedule: LiveData<List<ActionSchedule>> =
        Transformations.switchMap(daySchedule) { getRawActionsSchedule(it) }
    private fun getRawActionsSchedule(daySchedule: DaySchedule): LiveData<List<ActionSchedule>>{
        return when (daySchedule.type){
            TYPE_DAY_SCHEDULE_RELATIVE -> mScheduleRepository.getActionsRelativeScheduleFromDayID(daySchedule.id)
            TYPE_DAY_SCHEDULE_ABSOLUTE -> mScheduleRepository.getActionsAbsoluteScheduleFromDayID(daySchedule.id)
            else -> throw IllegalStateException("Invalid type")
        }
    }

    // Если расписание относительное, то необходимо расчитать start и end.
    val actionsSchedule: MediatorLiveData<List<ActionSchedule>> = MediatorLiveData()
    init {
        actionsSchedule.addSource(rawActionsSchedule) {
            mExecutor.execute {
                val newData = it.map { it.copy() }

                if (daySchedule.value!!.type == TYPE_DAY_SCHEDULE_RELATIVE){
                    it.forEachIndexed { i, actionSchedule ->
                        var start = actionSchedule.startAfter
                        start += if (i != 0) it[i-1].endTime else daySchedule.value!!.startDayTime

                        newData[i].startTime = start
                        newData[i].endTime = start+actionSchedule.duration
                    }
                }
                actionsSchedule.postValue(newData)
            }
        }
    }


    fun updateDaySchedule(){
        mScheduleRepository.updateDaySchedule(daySchedule.value!!)
    }


    private val mActionTypeRepository = ActionTypeRepository.get()
    fun getActionType(id: String): LiveData<ActionType> = mActionTypeRepository.getActionType(id)
}