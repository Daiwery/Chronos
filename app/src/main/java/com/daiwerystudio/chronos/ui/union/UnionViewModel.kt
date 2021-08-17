/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import java.util.*
import java.util.concurrent.Executors

/**
 * Класс определяет основные методы для взаимодействия с базой данных.
 *
 * Схема наблюдения LiveData: parentID - MutableLiveData, меняется во фрагмете;
 * На parentID подписан mUnions, которые по значению получает все unions от родителя;
 * На mUnions подписаны 3 LiveData: mActionTypesLiveData, mGoalsLiveData и т.п. Которые
 * получают по значению соответствующие значения;
 * На эти 3 LiveData подписан MediatorLiveData data, который по ним формирует и сортирует
 * общие данные.
 */
open class UnionViewModel : ViewModel() {
    private val mRepository = UnionRepository.get()
    private val mExecutor = Executors.newSingleThreadExecutor()

    var parentID: MutableLiveData<String> = MutableLiveData()
        private set

    /*                        Первый этап наблюдения                        */
    private var mUnions: LiveData<List<Union>> =
        Transformations.switchMap(parentID) { mRepository.getUnionsFromParent(it) }


    /*                         Второй этап наблюдения                        */
    private var mActionTypesLiveData: LiveData<List<ActionType>> =
        Transformations.switchMap(mUnions) { mRepository.getActionTypes(it) }
    private var mActionTypes: List<ActionType> = emptyList()

    private var mGoalsLiveData: LiveData<List<Goal>> =
        Transformations.switchMap(mUnions) { mRepository.getGoals(it) }
    private var mGoals: List<Goal> = emptyList()

    private var mSchedulesLiveData: LiveData<List<Schedule>> =
        Transformations.switchMap(mUnions) { mRepository.getSchedules(it) }
    private var mSchedules: List<Schedule> = emptyList()


    /*                        Третий этап наблюдения                        */
    var data: MediatorLiveData<List<Pair<Int, ID>>> = MediatorLiveData()
        private set

    init {
        data.addSource(mActionTypesLiveData) {
            mActionTypes = it
            mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mGoalsLiveData){
            mGoals = it
            mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mSchedulesLiveData){
            mSchedules = it
            mExecutor.execute { data.postValue(updateData()) }
        }
    }
    private fun updateData(): List<Pair<Int, ID>>{
        val newData = mutableListOf<Pair<Int, ID>>()

        newData.addAll(mActionTypes.map { Pair(TYPE_ACTION_TYPE, it) })
        newData.addAll(mGoals.map { Pair(TYPE_GOAL, it) })
        newData.addAll(mSchedules.map { Pair(TYPE_SCHEDULE, it) })

        return newData.sortedBy { mUnions.value!!.indexOfFirst { union -> union.id == it.second.id } }
    }


    /*                        Доп. функции                        */
    fun deleteUnionWithChild(id: String){
        mRepository.deleteUnionWithChild(id)
    }

    fun swap(from: Int, to: Int){
        mUnions.value!![from].indexList = to
        mUnions.value!![to].indexList = from

        Collections.swap(mUnions.value!!, from, to)
    }

    fun updateUnions(){
        mRepository.updateUnions(mUnions.value!!)
    }

    fun setAchievedGoalWithChild(id: String, isAchieved: Boolean){
        mRepository.setAchievedGoalWithChild(id, isAchieved)
    }

    fun updateSchedule(schedule: Schedule) {
        mRepository.updateSchedule(schedule)
    }
}