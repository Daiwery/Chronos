package com.daiwerystudio.chronos.ui.timetable

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Timetable
import com.daiwerystudio.chronos.database.TimetableRepository

class ChildTimetableViewModel : ViewModel() {
    private val timetableRepository = TimetableRepository.get()

    fun getTimetable(id: String): LiveData<Timetable> = timetableRepository.getTimetable(id)

    fun updateTimetable(timetable: Timetable){
        timetableRepository.updateTimetable(timetable)
    }

    fun deleteTimetableWithActions(timetable: Timetable){
        timetableRepository.deleteTimetableWithActions(timetable)
    }
}