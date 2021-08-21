/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 20.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: модификация без особых изменений логики.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.*

class DayScheduleViewModel : ViewModel() {
    private val scheduleRepository = ScheduleRepository.get()

    lateinit var actionsSchedule: LiveData<List<ActionSchedule>>
        private set

    fun getActionsSchedule(daySchedule: DaySchedule){
//        actionsSchedule = when (schedule.type){
//            TYPE_SCHEDULE_RELATIVE -> scheduleRepository.getActionsRelativeScheduleFromDayIndex(daySchedule)
//            TYPE_SCHEDULE_ABSOLUTE -> scheduleRepository.getActionsAbsoluteScheduleFromDayIndex(daySchedule)
//            else -> throw IllegalStateException("Invalid type")
//        }
    }

    fun deleteActionSchedule(actionSchedule: ActionSchedule){
        scheduleRepository.deleteActionSchedule(actionSchedule)
    }

    fun updateListActionSchedule(listActionSchedule: List<ActionSchedule>){
        scheduleRepository.updateListActionsSchedule(listActionSchedule)
    }

    fun updateActionSchedule(actionSchedule: ActionSchedule){
        scheduleRepository.updateActionSchedule(actionSchedule)
    }

    fun updateSchedule(schedule: Schedule){
        scheduleRepository.updateSchedule(schedule)
    }

    private val actionTypeRepository = ActionTypeRepository.get()

    fun getActionType(id: String): LiveData<ActionType> = actionTypeRepository.getActionType(id)
}