/*
* Дата создания: 20.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository
import com.daiwerystudio.chronos.database.UnionRepository

class ScheduleViewModel : ViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mUnionRepository = UnionRepository.get()

    val scheduleID: MutableLiveData<String> = MutableLiveData()

    val schedule: LiveData<Schedule> =
        Transformations.switchMap(scheduleID) { mScheduleRepository.getSchedule(it) }

    // В once_schedule длина массива равна 1.
    val daysScheduleIDs: LiveData<List<String>> =
        Transformations.switchMap(scheduleID) { mScheduleRepository.getIDsDaysScheduleFromScheduleID(it) }


    fun deleteUnionWithChild(id: String){
        mUnionRepository.deleteUnionWithChild(id)
    }

}