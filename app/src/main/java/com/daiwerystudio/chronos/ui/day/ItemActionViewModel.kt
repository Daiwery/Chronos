package com.daiwerystudio.chronos.ui.day

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionRepository
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import java.util.*


class ItemActionViewModel: ViewModel() {
    private val actionRepository = ActionRepository.get()
    private val actionTypeRepository = ActionTypeRepository.get()
    var actionTypes: LiveData<List<ActionType>> = actionTypeRepository.getActionTypesFromParent("")


    fun getActionTypesFromParent(id: String){
        actionTypes = actionTypeRepository.getActionTypesFromParent(id)
    }

    fun getActionType(id: UUID): LiveData<ActionType> = actionTypeRepository.getActionType(id)

    fun addAction(action: Action){
        actionRepository.addAction(action)
    }
}