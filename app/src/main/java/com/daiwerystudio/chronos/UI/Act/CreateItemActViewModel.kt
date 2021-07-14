package com.daiwerystudio.chronos.UI.Act

import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.DataBase.Act
import com.daiwerystudio.chronos.DataBase.ActRepository


class CreateItemActViewModel: ViewModel() {
    private val actRepository = ActRepository.get()

    fun addAct(act: Act){
        actRepository.addAct(act)
    }

    fun updateAct(act: Act){
        actRepository.updateAct(act)
    }

    fun deleteAct(act: Act){
        actRepository.deleteAct(act)
    }
    
}