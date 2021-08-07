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
 * на actionTypes, а родительный тип действия на parentActionType. Этот класс необходимо
 * использовать для того, чтобы при перевороте устройста (при котором фрагмент удаляется
 * и создается заного) данные из базы данных сохранились, а не извлекались заного.
 * @see ChildActionTypeFragment
 */
class ChildActionTypeViewModel: ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val repository = ActionTypeRepository.get()
    /**
     *  Здесь хранится id родительского типа действия, полученного из фрагмента.
     *  Причина, по которой фрагмент получает не сам тип действия, заключается в возможности
     *  изменить в этом фрагменте этот тип действия. Поэтому фрагмент подписывается на
     *  родительский тип действия. Это нужно, чтобы при перевороте устройства, данные из базы
     *  данных заного не извлекались.
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
    lateinit var actionTypes: LiveData<List<ActionType>>
        private set
    /**
     * Данные из базы данных в обертке LiveData. Есть подписка во фрагменте.
     */
    lateinit var parent: LiveData<ActionType>
        private set

    /**
     * Извлекает данные из базы данных.
     */
    private fun updateData(){
        actionTypes = repository.getActionTypesFromParent(idParent!!)
        parent = repository.getActionType(idParent!!)
    }

    /**
     * Удаляет дерево у заданного типа действия.
     */
    fun deleteActionTypeWithChild(actionType: ActionType){
        repository.deleteActionTypeWithChild(actionType)
    }

    /**
     * Возвращает количество детей у заданного типа действия. Используется только для UI.
     */
    fun getCountChild(id: String): LiveData<Int> = repository.getCountChild(id)
}