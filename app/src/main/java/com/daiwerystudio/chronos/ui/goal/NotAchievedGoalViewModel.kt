package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository

class NotAchievedGoalViewModel : ViewModel() {
    private val repository = GoalRepository.get()

    var goals: LiveData<List<Goal>> = repository.getGoalsWithoutParentFromSolve(false)

    fun getPercentAchieved(id: String): LiveData<Int> = repository.getPercentAchieved(id)

    fun deleteGoalWithChild(goal: Goal){
        repository.deleteGoalWithChild(goal)
    }

    fun setAchievedGoalWithChild(goal: Goal){
        repository.setAchievedGoalWithChild(goal)
    }
}