package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository

class ActiveScheduleViewModel : ViewModel() {
    private val repository = ScheduleRepository.get()

    var schedules: LiveData<List<Schedule>> = repository.getSchedulesFromActive(true)

    fun deleteScheduleWithActions(schedule: Schedule){
        repository.deleteScheduleWithActions(schedule)
    }

    fun updateSchedules(schedule: Schedule){
        repository.updateSchedule(schedule)
    }
}