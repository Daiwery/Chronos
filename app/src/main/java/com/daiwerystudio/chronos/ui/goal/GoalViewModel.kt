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
 * @see GoalFragment
 */
class GoalViewModel: ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val repository = GoalRepository.get()
    /**
     * Такая же причина, как и в ChildActionTypeViewModel.
     */
    var idParent: String? = null
        set(value) {
            if (field == null || value != field){
                field = value
                updateData()
            }
        }
    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    lateinit var goals: LiveData<List<Goal>>
        private set
    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    lateinit var parent: LiveData<Goal>
        private set

    /**
     * Извлекает данные из базы данных.
     */
    private fun updateData(){
        goals = repository.getGoalsFromParent(idParent!!)
        parent = repository.getGoal(idParent!!)
    }

    /**
     * Удаляет дерево у заданной цели.
     */
    fun deleteGoalWithChild(goal: Goal){
        repository.deleteGoalWithChild(goal)
    }

    /**
     * Устанавливает всему дереву целей isAchieved равный true. Выполняется, когда
     * пользователь решил отметить главную цель завершенной. Поэтому все подцели должны тоже
     * стать завершенными.
     */
    fun setAchievedGoalWithChild(goal: Goal){
        repository.setAchievedGoalWithChild(goal)
    }

    /**
     * Получает процент выполненный целей среди подцелей заданной цели.
     */
    fun getPercentAchieved(id: String): LiveData<Int> = repository.getPercentAchieved(id)

    /**
     * Обновляет цель в базе данных. Используется для изменения isAchieved и note при в/д
     * пользователя с соответствующими виджетами.
     */
    fun updateGoal(goal: Goal){
        repository.updateGoal(goal)
    }

    /**
     * Обновляет все цели в списке. Используется при уничтожении фрагмента, чтобы сохранить
     * indexList, требуемый пользователем.
     */
    fun updateListGoals(listGoal: List<Goal>){
        repository.updateListGoals(listGoal)
    }
}