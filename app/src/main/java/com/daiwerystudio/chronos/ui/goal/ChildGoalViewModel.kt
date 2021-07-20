package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository


class ChildGoalViewModel: ViewModel() {
    private val goalRepository = GoalRepository.get()
    lateinit var goals: LiveData<List<Goal>>

    fun getGoalsFromParent(id: String){
        goals = goalRepository.getGoalsFromParent(id)
    }

    fun deleteGoalWithChild(goal: Goal){
        goalRepository.deleteGoalWithChild(goal)
    }

    fun setAchievedGoalWithChild(goal: Goal){
        goalRepository.setAchievedGoalWithChild(goal)
    }

    fun getPercentAchieved(id: String): LiveData<Int> = goalRepository.getPercentAchieved(id)

}