package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository


class GoalViewModel: ViewModel() {
    private val repository = GoalRepository.get()
    lateinit var goals: LiveData<List<Goal>>
    lateinit var parentGoal: LiveData<Goal>

    fun getGoalsFromParent(id: String){
        goals = repository.getGoalsFromParent(id)
    }

    fun deleteGoalWithChild(goal: Goal){
        repository.deleteGoalWithChild(goal)
    }

    fun setAchievedGoalWithChild(goal: Goal){
        repository.setAchievedGoalWithChild(goal)
    }

    fun getGoal(id: String){
        parentGoal = repository.getGoal(id)
    }

    fun getPercentAchieved(id: String): LiveData<Int> = repository.getPercentAchieved(id)

    fun updateGoal(goal: Goal){
        repository.updateGoal(goal)
    }

    fun updateListGoals(listGoal: List<Goal>){
        repository.updateListGoals(listGoal)
    }
}