package com.daiwerystudio.chronos.ui.actiontype

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository


class ActionTypeViewModel: ViewModel() {
    private val repository = ActionTypeRepository.get()
    var actionTypes: LiveData<List<ActionType>> = repository.getActionTypesFromParent("")


    fun getCountChild(id: String): LiveData<Int> = repository.getCountChild(id)

    fun deleteActionTypeWithChild(actionType: ActionType){
        repository.deleteActionTypeWithChild(actionType)
    }
}