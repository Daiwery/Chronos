package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.*
import java.lang.IllegalStateException


class DayScheduleViewModel : ViewModel() {
    private val scheduleRepository = ScheduleRepository.get()
    lateinit var actionsSchedule: LiveData<List<ActionSchedule>>

    fun getActionsSchedule(schedule: Schedule, dayIndex: Int){
        actionsSchedule = when (schedule.type){
            TYPE_SCHEDULE_RELATIVE -> scheduleRepository.getActionsRelativeScheduleFromDayIndex(schedule.id, dayIndex)
            TYPE_SCHEDULE_ABSOLUTE -> scheduleRepository.getActionsAbsoluteScheduleFromDayIndex(schedule.id, dayIndex)
            else -> throw IllegalStateException("Invalid type")
        }
    }

    fun updateStartEndTimes(schedule: Schedule, actionsSchedule: List<ActionSchedule>){
        if (schedule.type == TYPE_SCHEDULE_RELATIVE){
            actionsSchedule.forEachIndexed { i, actionSchedule ->
                var start = actionSchedule.startAfter
                start += if (i != 0) actionsSchedule[i-1].endTime
                else schedule.defaultStartDayTime

                actionsSchedule[i].startTime = start
                actionsSchedule[i].endTime = start+actionSchedule.duration
            }
        }
    }

    fun deleteActionSchedule(actionSchedule: ActionSchedule){
        scheduleRepository.deleteActionSchedule(actionSchedule)
    }

    fun updateListActionTimetable(listActionSchedule: List<ActionSchedule>){
        scheduleRepository.updateListActionsSchedule(listActionSchedule)
    }


    private val actionTypeRepository = ActionTypeRepository.get()
    fun getActionType(id: String): LiveData<ActionType> = actionTypeRepository.getActionType(id)
}