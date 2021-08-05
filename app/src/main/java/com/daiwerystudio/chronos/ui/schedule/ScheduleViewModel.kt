package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository

class ScheduleViewModel : ViewModel() {
    private val repository = ScheduleRepository.get()
    lateinit var schedule: LiveData<Schedule>

    fun getSchedule(id: String){
        schedule = repository.getSchedule(id)
    }

    fun updateSchedule(schedule: Schedule){
        repository.updateSchedule(schedule)
    }

    fun deleteScheduleWithActions(schedule: Schedule){
        repository.deleteScheduleWithActions(schedule)
    }
}