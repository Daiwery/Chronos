/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.concurrent.Executors

private const val UNION_DATABASE_NAME = "union-database"

const val TYPE_ACTION_TYPE = 0
const val TYPE_GOAL = 1
const val TYPE_SCHEDULE = 2

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
    private val mExecutor = Executors.newSingleThreadExecutor()
    private val mActionTypeRepository = ActionTypeRepository.get()
    private val mGoalRepository = GoalRepository.get()
    private val mScheduleRepository = ScheduleRepository.get()

    fun getUnionsFromParent(parent: String): LiveData<List<Union>> = mDao.getUnionsFromParent(parent)

    fun getActionTypes(union: List<Union>): LiveData<List<ActionType>> =
        mActionTypeRepository.getActionTypes(union.filter { it.type == TYPE_ACTION_TYPE }.map { it.id })

    fun getGoals(union: List<Union>): LiveData<List<Goal>> =
        mGoalRepository.getGoals(union.filter { it.type == TYPE_GOAL }.map { it.id })

    fun getSchedules(union: List<Union>): LiveData<List<Schedule>> =
        mScheduleRepository.getSchedules(union.filter { it.type == TYPE_SCHEDULE }.map { it.id })

    fun deleteUnionWithChild(id: String){
        mExecutor.execute {
            val unions = mDao.getUnionWithChild(id)

            mDao.deleteUnions(unions)
            mActionTypeRepository.deleteActionTypes(unions.filter { it.type == TYPE_ACTION_TYPE }.map { it.id })
            mGoalRepository.deleteGoals(unions.filter { it.type == TYPE_GOAL }.map { it.id })
            mScheduleRepository.deleteSchedulesWithActions(unions.filter { it.type == TYPE_SCHEDULE }.map { it.id })
        }
    }

    fun setAchievedGoalWithChild(id: String, isAchieved: Boolean){
        mExecutor.execute {
            val ids = mDao.getGoalWithChild(id)
            mGoalRepository.setAchievedGoal(ids, isAchieved)
        }
    }

    fun updateUnions(unions: List<Union>){
        mExecutor.execute {
            mDao.updateUnions(unions)
        }
    }

    fun addUnion(union: Union){
        mExecutor.execute{
            mDao.addUnion(union)
        }
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