/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 23.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавление логики взаимодействия с DaySchedule.
*
* Дата изменения: 11.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: удаление таблицы дня в расписании. Теперь только один тип.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*

class DayScheduleViewModel : ViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()

    val daySchedule: MutableLiveData<Pair<String, Int>> = MutableLiveData()
    val actionsSchedule: LiveData<List<ActionSchedule>> =
        Transformations.switchMap(daySchedule) {
            mScheduleRepository.getActionsScheduleFromSchedule(it.first, it.second)
        }

    fun deleteActionSchedule(actionSchedule: ActionSchedule){
        mScheduleRepository.deleteActionSchedule(actionSchedule)
    }

    private val mActionTypeRepository = ActionTypeRepository.get()
    fun getActionType(id: String): LiveData<ActionType> = mActionTypeRepository.getActionType(id)
}