package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository

class AchievedGoalViewModel : ViewModel() {
    private val repository = GoalRepository.get()
    
    var goals: LiveData<List<Goal>> = repository.getGoalsWithoutParentFromSolve(true)

    fun deleteGoalWithChild(goal: Goal){
        repository.deleteGoalWithChild(goal)
    }
}