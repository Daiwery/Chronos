/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.action_type

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository

/**
 * Является ViewModel. Используется в ActionTypeFragment. С помощью этого класса фрагмент
 * может получать данные из базы данных. Данные для RecyclerView подписаны
 * на actionTypes. Этот класс необходимо использовать для того, чтобы при перевороте устройста
 * (при котором фрагмент удаляется и создается заного) данные из базы данных сохранились, а не
 * извлекались заного.
 * @see ActionTypeFragment
 */
class ActionTypeViewModel: ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val repository = ActionTypeRepository.get()
    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    var actionTypes: LiveData<List<ActionType>> = repository.getActionTypesFromParent("")
        private set

    /**
     * Возвращает количество детей у заданного типа действия. Используется только для UI.
     */
    fun getCountChild(id: String): LiveData<Int> = repository.getCountChild(id)

    /**
     * Удаляет все дерево у заданного типа действия.
     */
    fun deleteActionTypeWithChild(actionType: ActionType){
        repository.deleteActionTypeWithChild(actionType)
    }
}