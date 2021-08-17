/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository
import com.daiwerystudio.chronos.ui.union.UnionViewModel

class UnionGoalViewModel: UnionViewModel() {
    private val mRepository = GoalRepository.get()

    // Добавляем подписку на parentID.
    var parent: LiveData<Goal> =
        Transformations.switchMap(parentID) { mRepository.getGoal(it) }
        private set
}