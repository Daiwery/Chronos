package com.daiwerystudio.chronos.ui.day

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionRepository
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import java.util.*


class DayViewModel: ViewModel() {
    private val actionRepository = ActionRepository.get()
    lateinit var actions: LiveData<List<Action>>
    private val actionTypeRepository = ActionTypeRepository.get()

    fun getActionsFromTimes(time1: Long, time2: Long){
        actions = actionRepository.getActionsFromTimes(time1, time2)
    }

    fun getActionType(id: UUID): LiveData<ActionType> = actionTypeRepository.getActionType(id)
}