/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository


/**
 * Такая же логика как и в остальных ViewModel.
 * @see NotAchievedGoalFragment
 */
class NotAchievedGoalViewModel : ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val repository = GoalRepository.get()

    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    var goals: LiveData<List<Goal>> = repository.getGoalsWithoutParentFromSolve(false)

    fun getPercentAchieved(id: String): LiveData<Int> = repository.getPercentAchieved(id)

    /**
     * Удаляет все дерево у заданной цели.
     */
    fun deleteGoalWithChild(goal: Goal){
        repository.deleteGoalWithChild(goal)
    }

    /**
     * Устанавливаем всему дереву целей isAchieved равный true. Выполняется, когда
     * пользователь решил отметить главную цель завершенной. Поэтому все подцели должны тоже
     * стать завершенными.
     */
    fun setAchievedGoalWithChild(goal: Goal){
        repository.setAchievedGoalWithChild(goal)
    }
}