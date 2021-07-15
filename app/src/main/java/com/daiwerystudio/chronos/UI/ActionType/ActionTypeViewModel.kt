package com.daiwerystudio.chronos.UI.ActionType

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.DataBase.ActionType
import com.daiwerystudio.chronos.DataBase.ActionTypeRepository


class ActionTypeViewModel: ViewModel() {
    private val actionTypeRepository = ActionTypeRepository.get()
    lateinit var actionTypes: LiveData<List<ActionType>>  // У act без родителей, parent=""

    fun getActionTypesFromParent(id: String){
        actionTypes = actionTypeRepository.getActionTypesFromParent(id)
    }

    fun deleteActWithChild(actionType: ActionType){
        actionTypeRepository.deleteActionTypeWithChild(actionType)
    }

    fun countRows(): LiveData<Int> = actionTypeRepository.countRows()
}