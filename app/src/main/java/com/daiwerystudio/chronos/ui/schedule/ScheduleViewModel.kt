/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.DaySchedule
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository

/**
 * Является ViewModel. Логика идентична остальным ViewModel.
 */
class ScheduleViewModel : ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val repository = ScheduleRepository.get()
    /**
     *  Здесь хранится id расписания, полученного из фрагмента.
     *  Причина, по которой фрагмент получает не сам фрагмент, заключается в возможности
     *  изменить в этом фрагменте это расписание. Поэтому фрагмент подписывается на эти данные.
     *  Это нужно, чтобы при перевороте устройства, данные из базы данных заного не извлекались.
     */
    var id: String? = null
        set(value) {
            if (field == null || value != field){
                field = value
                updateData()
            }
        }

    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    lateinit var schedule: LiveData<Schedule>
        private set

    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     * Представляет из себя словарь вида словарь вида: индекс дня - количество испорченных действий.
     */
    var corruptedDays: LiveData<List<DaySchedule>> = MutableLiveData()
        private set

    /**
     * Извлекает данные из базы данных.
     */
    private fun updateData(){
        schedule = repository.getSchedule(id!!)
        corruptedDays = repository.getCorruptedDays(id!!)
    }


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