/*
* Дата создания: 19.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable

private const val UNION_DATABASE_NAME = "union-database"

/* Предупреждение: getGoalWithChild не используется константу TYPE_GOAL */
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
 * @property indexList порядковый номер в списке. По нему сортируется вывод данных, для того
 * чтобы сохранить порядок, требуемый пользователем.
 */
@Entity(tableName = "union_table")
data class Union(
    @PrimaryKey val id: String,
    var parent: String,
    var type: Int,
    var indexList: Int
) : Serializable


@Dao
interface UnionDao{
    @Query("SELECT * FROM union_table WHERE parent=(:parent) ORDER BY indexList")
    fun getUnionsFromParent(parent: String): LiveData<List<Union>>

    @Query("WITH RECURSIVE sub_table(id, parent) " +
        "AS (SELECT id, parent FROM union_table WHERE id=(:id) " +
        "UNION ALL " +
        "SELECT b.id, b.parent FROM union_table AS b " +
        "JOIN sub_table AS c ON c.id=b.parent) " +
        "SELECT * FROM union_table WHERE id IN (SELECT id FROM sub_table)")
    fun getUnionWithChild(id: String): List<Union>

    // Для значения type используется абсолютное значение, а не переменная TYPE_GOAL.
    @Query("WITH RECURSIVE sub_table(id, parent, type) " +
            "AS (SELECT id, parent, type FROM union_table WHERE id=(:id) " +
            "UNION ALL " +
            "SELECT b.id, b.parent, b.type FROM union_table AS b " +
            "JOIN sub_table AS c ON c.id=b.parent) " +
            "SELECT id FROM union_table WHERE id IN (SELECT id FROM sub_table WHERE type=1)")
    fun getGoalWithChild(id: String): List<String>

    @Delete
    fun deleteUnions(unions: List<Union>)

    @Update
    fun updateUnions(unions: List<Union>)

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

    fun getUnionsFromParent(parent: String): LiveData<List<Union>> = mDao.getUnionsFromParent(parent)

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

    fun updateUnions(unions: List<Union>){
        mHandler.post { mDao.updateUnions(unions) }
    }

    fun addUnion(union: Union){
        mHandler.post { mDao.addUnion(union) }
    }

    fun updateSchedule(schedule: Schedule) {
        mScheduleRepository.updateSchedule(schedule)
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