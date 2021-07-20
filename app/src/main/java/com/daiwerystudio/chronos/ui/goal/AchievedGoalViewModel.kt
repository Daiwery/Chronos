package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository

class AchievedGoalViewModel : ViewModel() {
    private val goalRepository = GoalRepository.get()
    var achievedGoals: LiveData<List<Goal>> = goalRepository.getGoalsWithoutParentFromSolve(true)
}