/*
* Дата создания: 29.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.widgets

import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.TYPE_ACTION_TYPE
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository

/**
 * Специальный класс ViewModel для работы SelectActionType.
 */
class SelectActionTypeViewModel: ViewModel() {
    private val mUnionRepository = UnionRepository.get()

    /* Показывать ли типы действий со всего дерева или только от id родителя, который получает диалог. */
    var isAll: MutableLiveData<Boolean> = MutableLiveData()
    lateinit var parentID: String


    /*  Первый этап наблюдения.  Получаем union и всех его детей.
    * Мы получаем все unions, независимо от типа.  */
    private val mLiveRawUnions: LiveData<List<Union>> =
        Transformations.switchMap(isAll) {
            if (it) mUnionRepository.getUnionWithChild("")
            else mUnionRepository.getUnionWithChild(parentID)
        }

    /*  Второй этап наблюдния. Нам нужно удалить все, кроме типов действий.
    * При этом нужно сохранить иерархию среди типов действий такой же.  */
    private val mLiveUnions: LiveData<List<Union>> =
        Transformations.switchMap(mLiveRawUnions) { rawUnions ->
            // Обработанные unions.
            val unions = mutableListOf<Union>()
            // Для union с parent=pair.first нужно установить родителя, равного pair.second.
            val newParents = mutableListOf<Pair<String, String>>()
            // Если мы показываем не все, то нужно типы действий от этого расписания
            // переместить в начало, так как SelectActionType начинает показывать с parent=''.
            if (isAll.value == false && parentID != "") newParents.add(Pair(parentID, ""))

            // Рекурсивный выбор нового родителя.
            // Выбираем до тех пор, пока не найдем замену родителю.
            fun getNewParent(parent: String): String{
                val index = newParents.indexOfFirst { it.first == parent }
                return if (index == -1) parent
                else getNewParent(newParents[index].second)
            }

            // Сперва нужно пройтись по узлам, которые не являются типами действий.
            rawUnions.filter { it.type != TYPE_ACTION_TYPE }.forEach { newParents.add(Pair(it.id, it.parent)) }
            // А после меняем родителей у типов действий, чтобы созранить иерархию.
            rawUnions.filter { it.type == TYPE_ACTION_TYPE }.forEach {
                unions.add(it.apply { parent = getNewParent(it.parent) })
            }

            MutableLiveData<List<Union>>().apply { value = unions }
        }
    private var mUnions: List<Union> = emptyList()

    /*  Третий этап наблюдения. Получаем список action types.  */
    private val mRawActionTypes: LiveData<List<ActionType>> =
        Transformations.switchMap(mLiveUnions) {
            mUnionRepository.getActionTypes(it)
        }

    /*  Четвертый этап наблюдения. Объединяем action types с их родителями.  */
    val actionTypes: MediatorLiveData<List<Pair<String, ActionType>>> = MediatorLiveData()
    init {
        actionTypes.addSource(mLiveUnions){ mUnions = it }
        actionTypes.addSource(mRawActionTypes){ rawActionTypes ->
            val newData = mutableListOf<Pair<String, ActionType>>()
            rawActionTypes.forEach { actionType ->
                newData.add(Pair(mUnions.first { it.id == actionType.id}.parent, actionType))
            }
            actionTypes.value = newData
        }
    }
}