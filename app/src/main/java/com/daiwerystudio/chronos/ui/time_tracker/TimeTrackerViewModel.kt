/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.time_tracker

import android.icu.util.TimeZone
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionRepository
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository

class TimeTrackerViewModel : ViewModel() {
    private val mActionRepository = ActionRepository.get()
    private val mActionTypeRepository = ActionTypeRepository.get()
    val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())

    // Локальный день.
    val day: MutableLiveData<Long> = MutableLiveData()

    // Массив с действиями.
    val actions: LiveData<List<Action>> = Transformations.switchMap(day){
        // Нужно не забыть перевести время из локального в глобальное.
        mActionRepository.getActionsFromTimeInterval(it*1000*60*60*24-local,
            (it+1)*1000*60*60*24-local)
    }


    /*                        Доп. функции                        */
    fun deleteAction(action: Action){
        mActionRepository.deleteAction(action)
    }

    fun getActionType(id: String): LiveData<ActionType> = mActionTypeRepository.getActionType(id)
}