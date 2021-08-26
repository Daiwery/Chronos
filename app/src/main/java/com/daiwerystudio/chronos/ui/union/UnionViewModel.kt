/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 24.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: вместо parentID теперь Pair<parentID, typeShowing>, где typeShowing -
* какой тип показывать.
*
* Дата изменения: 26.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: вместо Pair<parentID, typeShowing> добавлен специальный класс, наследуемый от LiveData.
*/

package com.daiwerystudio.chronos.ui.union

import android.util.Log
import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import java.util.*
import java.util.concurrent.Executors

/**
 * Класс определяет основные методы для взаимодействия с базой данных.
 *
 * Схема наблюдения LiveData: showing: - ShowingLiveData, специальный класс, определенный
 * здесь. Содержит два свойства - parentID и typeShowing, соединенные в Pair-объект,
 * где typeShowing - какой тип показывать. Меняется во фрагмете;
 * На showing подписан mUnions, которые по значению получает все unions от родителя;
 * На mUnions подписаны n LiveData: mActionTypesLiveData, mGoalsLiveData и т.п. Которые
 * получают по значению соответствующие значения;
 * На эти 3 LiveData подписан MediatorLiveData data, который по ним формирует и сортирует
 * общие данные.
 */
open class UnionViewModel : ViewModel() {
    private val mRepository = UnionRepository.get()
    private val mExecutor = Executors.newSingleThreadExecutor()

    /**
     * Специальный LiveData для наблюдения за id родителя и типом показа.
     */
    class ShowingLiveData: LiveData<Pair<String, Int>>(){
        var parentID: String = ""
            private set
        var typeShowing: Int = -1
            private set

        fun setData(parentID: String, typeShowing: Int){
            this.parentID = parentID
            this.typeShowing = typeShowing
            value = Pair(parentID, typeShowing)
        }

        fun setTypeShowing(typeShowing: Int){
            this.typeShowing = typeShowing
            value = Pair(parentID, typeShowing)
        }
    }
    val showing: ShowingLiveData = ShowingLiveData()


    /*                        Первый этап наблюдения                        */
    private var mUnions: LiveData<List<Union>> =
        Transformations.switchMap(showing) {
            Log.d("TEST", "${it.second}")
            if (it.second == -1)  mRepository.getUnionsFromParent(it.first)
            else mRepository.getUnionsFromParentAndType(it.first, it.second)
        }


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

    private var mNotesLiveData: LiveData<List<Note>> =
        Transformations.switchMap(mUnions) { mRepository.getNotes(it) }
    private var mNotes: List<Note> = emptyList()

    private var mRemindersLiveData: LiveData<List<Reminder>> =
        Transformations.switchMap(mUnions) { mRepository.getReminders(it) }
    private var mReminders: List<Reminder> = emptyList()


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
        data.addSource(mNotesLiveData){
            mNotes = it
            mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mRemindersLiveData){
            mReminders = it
            mExecutor.execute { data.postValue(updateData()) }
        }
    }
    private fun updateData(): List<Pair<Int, ID>>{
        val newData = mutableListOf<Pair<Int, ID>>()

        newData.addAll(mActionTypes.map { Pair(TYPE_ACTION_TYPE, it) })
        newData.addAll(mGoals.map { Pair(TYPE_GOAL, it) })
        newData.addAll(mSchedules.map { Pair(TYPE_SCHEDULE, it) })
        newData.addAll(mNotes.map { Pair(TYPE_NOTE, it) })
        newData.addAll(mReminders.map { Pair(TYPE_REMINDER, it) })

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

    fun getPercentAchieved(id: String): LiveData<Int> = mRepository.getPercentAchieved(id)
}