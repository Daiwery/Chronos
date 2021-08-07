/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository

/**
 * Является ViewModel. Имеет такой же функционал, как и все остальные.
 * @see NotActiveScheduleFragment
 */
class NotActiveScheduleViewModel : ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val repository = ScheduleRepository.get()
    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    var schedules: LiveData<List<Schedule>> = repository.getSchedulesFromActive(false)
        private set

    /**
     * Удаляет расписание со всеми действиями.
     */
    fun deleteScheduleWithActions(schedule: Schedule){
        repository.deleteScheduleWithActions(schedule)
    }

    /**
     * Обновляет расписание. Используется во фрагменте, чтобы обновить состояние isActive.
     */
    fun updateSchedule(schedule: Schedule){
        repository.updateSchedule(schedule)
    }
}