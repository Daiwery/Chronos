/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.widgets

import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import java.util.concurrent.Executors

/**
 * Специальный класс ViewModel для работы SelectActionType.
 */
class SelectActionTypeViewModel: ViewModel() {
    private val mUnionRepository = UnionRepository.get()
    private val mExecutor = Executors.newSingleThreadExecutor()

    /* Показывать ли типы действий со всего дерева или только от id родителя, который получает диалог. */
    var isAll: MutableLiveData<Boolean> = MutableLiveData()
    lateinit var parentID: String


    /*  Первый этап наблюдения.   Получаем union и всех его детей.
    * Мы получаем все unions, независимо от типа.  */
    private val mLiveRawUnions: LiveData<List<Union>> =
        Transformations.switchMap(isAll) {
            if (it) mUnionRepository.getActionTypeWithChild("")
            else mUnionRepository.getActionTypeWithChild(parentID)
        }

    /*  Второй этап наблюдния. Нам нужно удалить все, кроме типов действий.
    * При этом нужно сохранить иерархию среди типов действий такой же.  */
    private val mLiveUnions: LiveData<List<Union>> =
        Transformations.switchMap(mLiveRawUnions) { rawUnions ->
            val unions = mutableListOf<Union>()
            // Union с parent=first, нужно установить родителя, равного second.
            val newParents = mutableListOf<Pair<String, String>>()
            // Если мы показываем не все, то нужно типы действий от этого расписания
            // переместить в начало, так как SelectActionType начинает показывать с parent=''
            if (isAll.value == false) newParents.add(Pair(parentID, ""))

            // Рекурсивный выбор нового родителя.
            // Выбираем до тех пор, пока не найдется замены родителю.
            fun getNewParent(parent: String): String{
                val index = newParents.indexOfFirst { it.first == parent }
                return if (index == -1) parent
                else getNewParent(newParents[index].second)
            }
            // Цикл по узлам дерева (графа).
            rawUnions.forEach {
                if (it.type == TYPE_ACTION_TYPE)
                    unions.add(it.apply { parent = getNewParent(it.parent) })
                else newParents.add(Pair(it.id, it.parent))
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