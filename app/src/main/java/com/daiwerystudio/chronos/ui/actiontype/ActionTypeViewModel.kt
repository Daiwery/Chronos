package com.daiwerystudio.chronos.ui.actiontype

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository


class ActionTypeViewModel: ViewModel() {
    private val actionTypeRepository = ActionTypeRepository.get()
    // У actionType без родителей, parent=""
    var actionTypes: LiveData<List<ActionType>> = actionTypeRepository.getActionTypesFromParent("")
    fun getColorsActionTypesFromParent(id: String): LiveData<List<Int>> = actionTypeRepository.getColorsActionTypesFromParent(id)
}