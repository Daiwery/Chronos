package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository

class ItemGoalViewModel: ViewModel() {
    private val goalRepository = GoalRepository.get()

    fun updateGoal(goal: Goal) {
        goalRepository.updateGoal(goal)
    }

    fun addGoal(goal: Goal) {
        goalRepository.addGoal(goal)
    }
}