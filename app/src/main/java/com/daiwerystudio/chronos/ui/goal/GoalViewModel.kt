/*
* Дата создания: 19.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository
import com.daiwerystudio.chronos.database.UnionRepository

class GoalViewModel : ViewModel() {
    private val mGoalRepository = GoalRepository.get()
    private val mUnionRepository = UnionRepository.get()

    var goalID: MutableLiveData<String> = MutableLiveData()
        private set

    var goal: LiveData<Goal> =
        Transformations.switchMap(goalID) { mGoalRepository.getGoal(it) }
        private set


    fun updateGoal(){
        mUnionRepository.setAchievedGoalWithChild(goalID.value!!, goal.value!!.isAchieved)
        mGoalRepository.updateGoal(goal.value!!)
    }

    fun deleteUnionWithChild(id: String){
        mUnionRepository.deleteUnionWithChild(id)
    }
}