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
 * Является ViewModel. Используется в AchievedGoalViewModel. С помощью этого класса фрагмент
 * может получать данные из базы данных. Данные для RecyclerView подписаны
 * на goals. Этот класс необходимо использовать для того, чтобы при перевороте устройста
 * (при котором фрагмент удаляется и создается заного) данные из базы данных сохранились, а не
 * извлекались заного.
 * @see AchievedGoalViewModel
 */
class AchievedGoalViewModel : ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val repository = GoalRepository.get()

    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    var goals: LiveData<List<Goal>> = repository.getGoalsWithoutParentFromSolve(true)
        private set

    /**
     * Удаляет все дерево у заданной цели.
     */
    fun deleteGoalWithChild(goal: Goal){
        repository.deleteGoalWithChild(goal)
    }
}