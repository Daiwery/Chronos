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
* какой тип показывать.
*
* Дата изменения: 10.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлен поиск (фильтр).
*/

package com.daiwerystudio.chronos.ui.union

import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import java.util.*
import java.util.concurrent.Executors

/**
 * Класс определяет основные методы для взаимодействия с базой данных.
 *
 * Схема наблюдения LiveData: information: - informationLiveData, специальный класс, определенный
 * здесь. Содержит 3 свойства - value (parentID), filterType и filterString. Меняется во фрагмете;
 * На information подписан mUnions, которые по значению получает все unions от родителя;
 * На mUnions подписаны n LiveData: mActionTypesLiveData, mGoalsLiveData и т.п. Которые
 * получают по значению соответствующие значения;
 * На эти 3 LiveData подписан MediatorLiveData data, который по ним формирует и сортирует
 * общие данные.
 */
open class UnionViewModel : ViewModel() {
    private val mUnionRepository = UnionRepository.get()
    private val mScheduleRepository = ScheduleRepository.get()
    private val mGoalRepository = GoalRepository.get()
    private val mExecutor = Executors.newSingleThreadExecutor()

    /**
     * Специальный LiveData для наблюдения за id родителя и фильтрами по типу и по имени.
     */
    class InformationLiveData: LiveData<String>(){
        var parentID: String = ""
            private set
        var filterType: Int? = null
            private set
        var filterString: String? = null
            private set

        fun setData(parentID: String, filterType: Int?, filterName: String?){
            this.parentID = parentID
            this.filterType = filterType
            this.filterString = filterName
            value = parentID
        }

        fun setFilterType(filterType: Int?){
            this.filterType = filterType
            value = parentID
        }

        fun setFilterName(filterName: String?){
            this.filterString = filterName
            value = parentID
        }
    }
    val information: InformationLiveData = InformationLiveData()


    /*                        Первый этап наблюдения                        */
    private var mUnions: LiveData<List<Union>> =
        Transformations.switchMap(information) {
            when {
                information.filterType != null -> mUnionRepository.getUnionsFromType(information.filterType!!)
                information.filterString == "" -> MutableLiveData(emptyList())
                information.filterString != null -> mUnionRepository.getAllUnions()
                else -> mUnionRepository.getUnionsFromParent(it)
            }
        }


    /*                         Второй этап наблюдения                        */
    private var mActionTypesLiveData: LiveData<List<ActionType>> =
        Transformations.switchMap(mUnions) {
            mIsReceivedActionTypes = false
            mUnionRepository.getActionTypes(it)
        }
    private var mActionTypes: List<ActionType> = emptyList()
    private var mIsReceivedActionTypes: Boolean = false

    private var mGoalsLiveData: LiveData<List<Goal>> =
        Transformations.switchMap(mUnions) {
            mIsReceivedGoals = false
            mUnionRepository.getGoals(it)
        }
    private var mGoals: List<Goal> = emptyList()
    private var mIsReceivedGoals = false

    private var mSchedulesLiveData: LiveData<List<Schedule>> =
        Transformations.switchMap(mUnions) {
            mIsReceivedSchedules = false
            mUnionRepository.getSchedules(it)
        }
    private var mSchedules: List<Schedule> = emptyList()
    private var mIsReceivedSchedules = false

    private var mNotesLiveData: LiveData<List<Note>> =
        Transformations.switchMap(mUnions) {
            mIsReceivedNotes = false
            mUnionRepository.getNotes(it)
        }
    private var mNotes: List<Note> = emptyList()
    private var mIsReceivedNotes: Boolean = false

    private var mRemindersLiveData: LiveData<List<Reminder>> =
        Transformations.switchMap(mUnions) {
            mIsReceivedReminders = false
            mUnionRepository.getReminders(it)
        }
    private var mReminders: List<Reminder> = emptyList()
    private var mIsReceivedReminders: Boolean = false

    private var mFoldersLiveData: LiveData<List<Folder>> =
        Transformations.switchMap(mUnions) {
            mIsReceivedFolders = false
            mUnionRepository.getFolders(it)
        }
    private var mFolders: List<Folder> = emptyList()
    private var mIsReceivedFolders: Boolean = false


    /*                        Третий этап наблюдения                        */
    var data: MediatorLiveData<List<Pair<Int, ID>>> = MediatorLiveData()
        private set

    init {
        data.addSource(mActionTypesLiveData) { actionTypes ->
            val filter = information.filterString
            mActionTypes = if (filter != "" && filter != null)
                actionTypes.filter { it.name.contains(filter, ignoreCase=true) }
            else actionTypes
            mIsReceivedActionTypes = true

            if (mIsReceivedActionTypes && mIsReceivedGoals && mIsReceivedSchedules
                && mIsReceivedNotes && mIsReceivedReminders && mIsReceivedFolders)
                mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mGoalsLiveData){ goals ->
            val filter = information.filterString
            mGoals = if (filter != "" && filter != null) goals.filter {
                it.name.contains(filter, ignoreCase=true) || it.note.contains(filter, ignoreCase=true)
            } else goals
            mIsReceivedGoals = true

            if (mIsReceivedActionTypes && mIsReceivedGoals && mIsReceivedSchedules
                && mIsReceivedNotes && mIsReceivedReminders && mIsReceivedFolders)
                mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mSchedulesLiveData){ schedules ->
            val filter = information.filterString
            mSchedules = if (filter != "" && filter != null)
                schedules.filter { it.name.contains(filter, ignoreCase=true) }
            else schedules
            mIsReceivedSchedules = true

            if (mIsReceivedActionTypes && mIsReceivedGoals && mIsReceivedSchedules
                && mIsReceivedNotes && mIsReceivedReminders && mIsReceivedFolders)
                mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mNotesLiveData){ notes ->
            val filter = information.filterString
            mNotes = if (filter != "" && filter != null) notes.filter {
                it.name.contains(filter, ignoreCase=true) || it.note.contains(filter, ignoreCase=true)
            } else notes
            mIsReceivedNotes = true

            if (mIsReceivedActionTypes && mIsReceivedGoals && mIsReceivedSchedules
                && mIsReceivedNotes && mIsReceivedReminders && mIsReceivedFolders)
                mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mRemindersLiveData){ reminders ->
            val filter = information.filterString
            mReminders = if (filter != "" && filter != null)
                reminders.filter { it.text.contains(filter, ignoreCase=true) }
            else reminders
            mIsReceivedReminders = true

            if (mIsReceivedActionTypes && mIsReceivedGoals && mIsReceivedSchedules
                && mIsReceivedNotes && mIsReceivedReminders && mIsReceivedFolders)
                mExecutor.execute { data.postValue(updateData()) }
        }
        data.addSource(mFoldersLiveData){ folders ->
            val filter = information.filterString
            mFolders = if (filter != "" && filter != null)
                folders.filter { it.name.contains(filter, ignoreCase=true) }
            else folders
            mIsReceivedFolders = true

            if (mIsReceivedActionTypes && mIsReceivedGoals && mIsReceivedSchedules
                && mIsReceivedNotes && mIsReceivedReminders && mIsReceivedFolders)
                mExecutor.execute { data.postValue(updateData()) }
        }
    }
    private fun updateData(): List<Pair<Int, ID>>{
        val newData = mutableListOf<Pair<Int, ID>>()

        newData.addAll(mFolders.map { Pair(TYPE_FOLDER, it) })
        newData.addAll(mNotes.map { Pair(TYPE_NOTE, it) })
        newData.addAll(mReminders.map { Pair(TYPE_REMINDER, it) })
        newData.addAll(mGoals.map { Pair(TYPE_GOAL, it) })
        newData.addAll(mSchedules.map { Pair(TYPE_SCHEDULE, it) })
        newData.addAll(mActionTypes.map { Pair(TYPE_ACTION_TYPE, it) })

        return if (information.filterType != null || information.filterString != null) newData
        else {
            updateUnionsIndexList()
            newData.sortedBy { mUnions.value!!.indexOfFirst { union -> union.id == it.second.id } }
        }
    }

    // Так как мы сортируем по indexList, то возможна ситуация, когда номер в массиве
    // не совпадает с indexList. Это приведет к неприятным ситуациям, поэтому
    // это нужно исправить.
    private fun updateUnionsIndexList(){
        mUnions.value?.forEachIndexed { index, union ->
            // Union - ссылочная переменная, поэтому она изменяется сразу.
            union.indexList = index
        }
    }


    /*                        Доп. функции                        */
    fun deleteUnionWithChild(id: String){
        mUnionRepository.deleteUnionWithChild(id)
    }

    fun deleteUnionsWithChild(positions: List<Int>){
        mUnionRepository.deleteUnionsWithChild(mUnions.value!!.map { it.id }
            .filterIndexed { index, _ -> index in positions })
    }

    fun swap(fromPosition: Int, toPosition: Int){
        Collections.swap(data.value!!, fromPosition, toPosition)
        Collections.swap(mUnions.value!!, fromPosition, toPosition)
    }

    fun updateUnions(){
        if (information.filterType == null && information.filterString == null){
            updateUnionsIndexList()
            mUnionRepository.updateUnions(mUnions.value!!)
        }
    }

    fun moveUnionUp(position: Int){
        mExecutor.execute {
            // Не является LiveData, поэтому выполняем в отдельном потоке.
            val parent = mUnionRepository.getParentUnion(information.parentID)
            if (parent != null) {
                updateUnions()

                val union = mUnions.value!![position]
                union.parent = parent
                mUnionRepository.updateUnion(union)
            }
        }
    }

    fun moveUnionsUp(positions: List<Int>){
        mExecutor.execute {
            // Не является LiveData, поэтому выполняем в отдельном потоке.
            val parent = mUnionRepository.getParentUnion(information.parentID)
            if (parent != null) {
                updateUnions()
                for (position in positions){
                    val union = mUnions.value!![position]
                    union.parent = parent
                    mUnionRepository.updateUnion(union)
                }
            }
        }
    }

    fun editParentUnion(from: Int, to: Int){
        mExecutor.execute {
            val id = data.value!![from].second.id
            val union = mUnions.value!!.first { it.id == id }

            if (data.value!![to].first != TYPE_REMINDER) {
                updateUnions()
                union.parent = data.value!![to].second.id
                mUnionRepository.updateUnion(union)
            }
        }
    }

    fun setAchievedGoalWithChild(id: String){
        mUnionRepository.setAchievedGoalWithChild(id, true)
    }

    fun setActivityScheduleWithChild(id: String, isActive: Boolean) {
        mUnionRepository.setActivityScheduleWithChild(id, isActive)
    }

    fun updateGoal(goal: Goal){
        mGoalRepository.updateGoal(goal)
    }

    fun getPercentAchieved(id: String): LiveData<Int> = mUnionRepository.getPercentAchieved(id)
    fun existenceGoals(id: String): LiveData<Boolean> = mUnionRepository.existenceGoals(id)
}