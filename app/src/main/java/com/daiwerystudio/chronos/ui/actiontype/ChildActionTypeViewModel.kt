package com.daiwerystudio.chronos.ui.actiontype

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository


class ChildActionTypeViewModel: ViewModel() {
    private val actionTypeRepository = ActionTypeRepository.get()
    lateinit var actionTypes: LiveData<List<ActionType>>

    fun getActionTypesFromParent(id: String){
        actionTypes = actionTypeRepository.getActionTypesFromParent(id)
    }

    fun deleteActWithChild(actionType: ActionType){
        actionTypeRepository.deleteActionTypeWithChild(actionType)
    }
}