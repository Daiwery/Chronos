/*
* Дата создания: 19.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import java.io.Serializable

private const val UNION_DATABASE_NAME = "union-database"

/* Предупреждение: getGoalWithChild не используется константу TYPE_GOAL,
* getActionTypeWithChild не используется константу TYPE_ACTION_TYPE  */
const val TYPE_ACTION_TYPE = 0
const val TYPE_GOAL = 1
const val TYPE_SCHEDULE = 2
const val TYPE_NOTE = 3
const val TYPE_REMINDER = 4

/**
 * Данный класс представляет из себя соединение ActionType, Goal и др. в древовидную структуру.
 * @property id уникальный идентификатор. Совпадает с id у соответствующего значения ActionType, Goal и др.
 * @property parent id родителя. Совпадает с id у соответствующего значения ActionType, Goal и др.
 * @property type тип, который представляет из себя union.
 */
@Entity(tableName = "union_table")
data class Union(
    @PrimaryKey val id: String,
    var parent: String,
    var type: Int,
) : Serializable


@Dao
interface UnionDao{
    @Query("SELECT * FROM union_table WHERE parent=(:parent)")
    fun getUnionsFromParent(parent: String): LiveData<List<Union>>

    @Query("WITH RECURSIVE sub_table(id, parent, type) " +
            "AS (SELECT id, parent, type FROM union_table WHERE parent=(:id)" +
            "UNION ALL " +
            "SELECT a.id, a.parent, a.type FROM union_table AS a JOIN sub_table AS b ON a.parent=b.id AND (b.type!=(:type) OR a.parent='')) " +
            "SELECT * FROM union_table WHERE id IN (SELECT id FROM sub_table WHERE type=(:type))")
    fun getUnionsFromParentAndType(id: String, type: Int): LiveData<List<Union>>

    @Query("WITH RECURSIVE sub_table(id, parent) " +
        "AS (SELECT id, parent FROM union_table WHERE id=(:id) " +
        "UNION ALL " +
        "SELECT a.id, a.parent FROM union_table AS a JOIN sub_table AS b ON a.parent=b.id) " +
        "SELECT * FROM union_table WHERE id IN (SELECT id FROM sub_table)")
    fun getUnionWithChild(id: String): List<Union>

    @Query("SELECT parent FROM union_table WHERE id=(:id)")
    fun getParentUnion(id: String): String?

    // Для значения type используется абсолютное значение, а не переменная TYPE_GOAL.
    @Query("WITH RECURSIVE sub_table(id, parent, type) " +
            "AS (SELECT id, parent, type FROM union_table WHERE id=(:id) " +
            "UNION ALL " +
            "SELECT a.id, a.parent, a.type FROM union_table AS a JOIN sub_table AS b ON a.parent=b.id) " +
            "SELECT id FROM union_table WHERE id IN (SELECT id FROM sub_table WHERE type=1)")
    fun getGoalWithChild(id: String): List<String>

    // Для значения type используется абсолютное значение, а не переменная TYPE_ACTION_TYPE.
    @Query("WITH RECURSIVE sub_table(id, parent, type) " +
            "AS (SELECT id, parent, type FROM union_table WHERE parent=(:id) " +
            "UNION ALL " +
            "SELECT a.id, a.parent, a.type FROM union_table AS a JOIN sub_table AS b ON a.parent=b.id) " +
            "SELECT * FROM union_table WHERE id IN (SELECT id FROM sub_table)")
    fun getActionTypeWithChild(id: String): LiveData<List<Union>>

    @Delete
    fun deleteUnions(unions: List<Union>)

    @Update
    fun updateUnion(union: Union)

    @Insert
    fun addUnion(union: Union)
}

@Database(entities = [Union::class], version=1, exportSchema=false)
abstract class UnionDatabase : RoomDatabase() {
    abstract fun dao(): UnionDao
}

/**
 * Является синглтоном и инициализируется в DataBaseApplication.
 *
 * Ключевой особенностью является то, что он может посылать запросы другим репозиториям.
 * @see DataBaseApplication
 */
class UnionRepository private constructor(context: Context) {
    private val mDatabase: UnionDatabase = Room.databaseBuilder(context.applicationContext,
        UnionDatabase::class.java,
        UNION_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mActionTypeRepository = ActionTypeRepository.get()
    private val mGoalRepository = GoalRepository.get()
    private val mScheduleRepository = ScheduleRepository.get()
    private val mNoteRepository = NoteRepository.get()
    private val mReminderRepository = ReminderRepository.get()
    private val mHandlerThread = HandlerThread("UnionRepository")
    private var mHandler: Handler

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }

    fun getUnionsFromParent(parent: String): LiveData<List<Union>> =
        mDao.getUnionsFromParent(parent)

    fun getUnionsFromParentAndType(id: String, type: Int): LiveData<List<Union>> =
        mDao.getUnionsFromParentAndType(id, type)

    fun getParentUnion(id: String): String? = mDao.getParentUnion(id)

    fun getActionTypes(union: List<Union>): LiveData<List<ActionType>> =
        mActionTypeRepository.getActionTypes(union.filter { it.type == TYPE_ACTION_TYPE }.map { it.id })

    fun getGoals(union: List<Union>): LiveData<List<Goal>> =
        mGoalRepository.getGoals(union.filter { it.type == TYPE_GOAL }.map { it.id })

    fun getSchedules(union: List<Union>): LiveData<List<Schedule>> =
        mScheduleRepository.getSchedules(union.filter { it.type == TYPE_SCHEDULE }.map { it.id })

    fun getNotes(union: List<Union>): LiveData<List<Note>> =
        mNoteRepository.getNotes(union.filter { it.type == TYPE_NOTE }.map { it.id })

    fun getReminders(union: List<Union>): LiveData<List<Reminder>> =
        mReminderRepository.getReminders(union.filter { it.type == TYPE_REMINDER }.map { it.id })

    fun deleteUnionWithChild(id: String){
        mHandler.post {
            val unions = mDao.getUnionWithChild(id)

            mDao.deleteUnions(unions)
            mActionTypeRepository.deleteActionTypes(unions.filter { it.type == TYPE_ACTION_TYPE }.map { it.id })
            mGoalRepository.deleteGoals(unions.filter { it.type == TYPE_GOAL }.map { it.id })
            mScheduleRepository.deleteCompletelySchedules(unions.filter { it.type == TYPE_SCHEDULE }.map { it.id })
            mNoteRepository.deleteNotes(unions.filter { it.type == TYPE_NOTE }.map { it.id })
            mReminderRepository.deleteReminders(unions.filter { it.type == TYPE_REMINDER }.map { it.id })
        }
    }

    fun setAchievedGoalWithChild(id: String, isAchieved: Boolean){
        mHandler.post {
            val ids = mDao.getGoalWithChild(id)
            mGoalRepository.setAchievedGoal(ids, isAchieved)
        }
    }

    fun getPercentAchieved(id: String): LiveData<Int>{
        // Percent удаляется, так как это не RoomLiveData.
        val percent = MutableLiveData<Int>()
        mHandler.post {
            val ids = mDao.getGoalWithChild(id)
            percent.postValue(mGoalRepository.getPercentAchieved(ids))
        }
        return percent
    }

    fun getActionTypeWithChild(id: String): LiveData<List<Union>> =
        mDao.getActionTypeWithChild(id)

    fun updateUnion(union: Union){
        mHandler.post { mDao.updateUnion(union) }
    }

    fun addUnion(union: Union){
        mHandler.post { mDao.addUnion(union) }
    }


    companion object {
        private var INSTANCE: UnionRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = UnionRepository(context)
            }
        }

        fun get(): UnionRepository {
            return INSTANCE ?: throw IllegalStateException("ActionTypeRepository must be initialized")
        }
    }
}