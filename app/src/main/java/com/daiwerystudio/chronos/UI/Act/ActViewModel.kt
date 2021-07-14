package com.daiwerystudio.chronos.UI.Act

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.DataBase.Act
import com.daiwerystudio.chronos.DataBase.ActRepository
import java.util.*


class ActViewModel: ViewModel() {
    private val actRepository = ActRepository.get()
    lateinit var acts: LiveData<List<Act>>  // У act без родителей, parent=""

    fun getActsFromParent(id: String){
        acts = actRepository.getActsFromParent(id)
    }
}