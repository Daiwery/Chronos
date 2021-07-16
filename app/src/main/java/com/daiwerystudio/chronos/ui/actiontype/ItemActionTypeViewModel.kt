package com.daiwerystudio.chronos.ui.actiontype

import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository


class ItemActionTypeViewModel: ViewModel() {
    private val actionTypeRepository = ActionTypeRepository.get()

    fun addActionType(actionType: ActionType){
        actionTypeRepository.addActionType(actionType)
    }

    fun updateActionType(actionType: ActionType){
        actionTypeRepository.updateActionType(actionType)
    }
}