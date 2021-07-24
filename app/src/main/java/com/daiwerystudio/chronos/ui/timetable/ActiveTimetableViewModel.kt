package com.daiwerystudio.chronos.ui.timetable

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Timetable
import com.daiwerystudio.chronos.database.TimetableRepository

class ActiveTimetableViewModel : ViewModel() {
    private val timetableRepository = TimetableRepository.get()
    var activeTimetables: LiveData<List<Timetable>> = timetableRepository.getTimetableFromActive(true)
}