/*
* Дата создания: 20.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.DaySchedule
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository
import com.daiwerystudio.chronos.database.UnionRepository

class ScheduleViewModel : ViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mUnionRepository = UnionRepository.get()

    var scheduleID: MutableLiveData<String> = MutableLiveData()
        private set

    var schedule: LiveData<Schedule> =
        Transformations.switchMap(scheduleID) { mScheduleRepository.getSchedule(it) }
        private set

    fun getDaysScheduleFromScheduleID(scheduleID: String): LiveData<List<DaySchedule>> =
        mScheduleRepository.getDaysScheduleFromScheduleID(scheduleID)

    fun deleteUnionWithChild(id: String){
        mUnionRepository.deleteUnionWithChild(id)
    }

    fun addDaySchedule(daySchedule: DaySchedule){
        mScheduleRepository.addDaySchedule(daySchedule)
    }

}