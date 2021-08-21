/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* * Изменения: 16.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменено: добавлен UnionRepository.
*/

package com.daiwerystudio.chronos.database

import android.app.Application


class DataBaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        ActionTypeRepository.initialize(this)
        GoalRepository.initialize(this)
        ScheduleRepository.initialize(this)
        NoteRepository.initialize(this)
        ReminderRepository.initialize(this)
        UnionRepository.initialize(this)

        ActionRepository.initialize(this)
    }
}