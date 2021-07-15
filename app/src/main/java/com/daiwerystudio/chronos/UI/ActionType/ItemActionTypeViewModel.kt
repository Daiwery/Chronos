package com.daiwerystudio.chronos.UI.ActionType

import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.DataBase.ActionType
import com.daiwerystudio.chronos.DataBase.ActionTypeRepository


class ItemActionTypeViewModel: ViewModel() {
    private val actionTypeRepository = ActionTypeRepository.get()

    fun addActionType(actionType: ActionType){
        actionTypeRepository.addActionType(actionType)
    }

    fun updateActionType(actionType: ActionType){
        actionTypeRepository.updateActionType(actionType)
    }
}