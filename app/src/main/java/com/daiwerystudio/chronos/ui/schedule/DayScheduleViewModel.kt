/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.*
import java.lang.IllegalStateException

/**
 * Является ViewModel. Логика идентична остальным ViewModel.
 * За тем исключением, что на этот класс возложена обязанность считать startTime и endTime
 * у ActionsSchedule, если расписание относительное.
 */
class DayScheduleViewModel : ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val scheduleRepository = ScheduleRepository.get()
    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    lateinit var actionsSchedule: LiveData<List<ActionSchedule>>
        private set

    /**
     * Извлекает данные из базы данных.
     */
    fun getActionsSchedule(schedule: Schedule, dayIndex: Int){
        actionsSchedule = when (schedule.type){
            TYPE_SCHEDULE_RELATIVE -> scheduleRepository.getActionsRelativeScheduleFromDayIndex(schedule.id, dayIndex)
            TYPE_SCHEDULE_ABSOLUTE -> scheduleRepository.getActionsAbsoluteScheduleFromDayIndex(schedule.id, dayIndex)
            else -> throw IllegalStateException("Invalid type")
        }
    }


    /**
     * Удаляет действие в базе данных.
     */
    fun deleteActionSchedule(actionSchedule: ActionSchedule){
        scheduleRepository.deleteActionSchedule(actionSchedule)
    }

    /**
     * Обновляет все действия в списке. Используется, чтобы сохранить indexList, требуемый
     * пользователем. И заодно значения startTime и endTime.
     */
    fun updateListActionTimetable(listActionSchedule: List<ActionSchedule>){
        scheduleRepository.updateListActionsSchedule(listActionSchedule)
    }

    /**
     * Обновляет расписание. Нужно для изменения defaultDayStartTime.
     */
    fun updateSchedule(schedule: Schedule){
        scheduleRepository.updateSchedule(schedule)
    }

    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val actionTypeRepository = ActionTypeRepository.get()

    /**
     * Ивлекает из базы данных тип действия в обертке LiveData. Необходим для UI.
     */
    fun getActionType(id: String): LiveData<ActionType> = actionTypeRepository.getActionType(id)
}