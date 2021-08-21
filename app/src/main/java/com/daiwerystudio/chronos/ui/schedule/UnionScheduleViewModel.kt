/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/


package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository
import com.daiwerystudio.chronos.ui.union.UnionViewModel


class UnionScheduleViewModel : UnionViewModel() {
    private val mRepository = ScheduleRepository.get()

    // Добавляем подписку на parentID.
    var parent: LiveData<Schedule> =
        Transformations.switchMap(parentID) { mRepository.getSchedule(it) }
        private set
}