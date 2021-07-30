package com.daiwerystudio.chronos.ui.actiontype

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository


class ChildActionTypeViewModel: ViewModel() {
    private val repository = ActionTypeRepository.get()
    var actionTypes: LiveData<List<ActionType>> = MutableLiveData()
    lateinit var parentActionType: LiveData<ActionType>


    fun getActionTypesFromParent(id: String){
        actionTypes = repository.getActionTypesFromParent(id)
    }

    fun deleteActionTypeWithChild(actionType: ActionType){
        repository.deleteActionTypeWithChild(actionType)
    }

    fun getActionType(id: String){
        parentActionType = repository.getActionType(id)
    }

    fun getCountChild(id: String): LiveData<Int> = repository.getCountChild(id)
}