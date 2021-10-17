/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 19.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union. Вместо отдельного потока добавлен отдельный looper.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.room.*
import com.daiwerystudio.chronos.ui.union.ID
import java.io.Serializable

private const val GOAL_DATABASE_NAME = "goal-database"

/* Предупреждение: getGoalsFromTimeInterval  */
const val TYPE_GOAL_INDEFINITE = 0
const val TYPE_GOAL_TEMPORARY = 1

/**
 * @property id уникальный идентификатор
 * @property name название цели.
 * @property type тип цели.
 * @property deadline дедлайн цели. У бессрочной цели фиктивно.
 * @property isAchieved выполнена ли цель или нет.
 */
@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey override val id: String,
    var name: String = "",
    var note: String = "",
    var type: Int = TYPE_GOAL_INDEFINITE,
    var deadline: Long = System.currentTimeMillis(),
    var isAchieved: Boolean = false,
) : Serializable, ID


@Dao
interface GoalTypeDao {
    @Query("SELECT * FROM goal_table WHERE id IN (:ids) ORDER BY name")
    fun getGoals(ids: List<String>): LiveData<List<Goal>>

    // Для значения type используется абсолютное значение, а не переменная TYPE_GOAL_TEMPORARY.
    @Query("SELECT * FROM goal_table WHERE deadline >= (:time1) AND deadline <= (:time2) AND type == 1")
    fun getGoalsFromTimeInterval(time1: Long, time2: Long): LiveData<List<Goal>>

    @Query("SELECT * FROM goal_table WHERE id=(:id)")
    fun getGoal(id: String): LiveData<Goal>

    @Query("SELECT AVG(isAchieved=1)*100 FROM goal_table WHERE id IN (:ids)")
    fun getPercentAchieved(ids: List<String>): Int

    @Query("UPDATE goal_table SET isAchieved=(:isAchieved) WHERE id IN (:ids)")
    fun setAchievedGoal(ids: List<String>, isAchieved: Boolean)

    @Query("DELETE FROM goal_table WHERE id IN (:ids)")
    fun deleteGoals(ids: List<String>)

    @Delete
    fun deleteGoal(goal: Goal)

    @Update
    fun updateGoal(goal: Goal)

    @Update
    fun updateGoals(goals: List<Goal>)

    @Insert
    fun addGoal(goal: Goal)
}


@Database(entities = [Goal::class], version=1, exportSchema=false)
abstract class GoalDatabase : RoomDatabase() {
    abstract fun dao(): GoalTypeDao
}

class GoalRepository private constructor(context: Context) {
    private val mDatabase: GoalDatabase = Room.databaseBuilder(context.applicationContext,
        GoalDatabase::class.java,
        GOAL_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mHandlerThread = HandlerThread("GoalRepository")
    private var mHandler: Handler

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }


    fun getGoals(ids: List<String>): LiveData<List<Goal>> = mDao.getGoals(ids)

    fun getGoalsFromTimeInterval(time1: Long, time2: Long): LiveData<List<Goal>> =
        mDao.getGoalsFromTimeInterval(time1, time2)

    fun getGoal(id: String): LiveData<Goal> = mDao.getGoal(id)

    fun getPercentAchieved(ids: List<String>): Int = mDao.getPercentAchieved(ids)

    fun setAchievedGoal(ids: List<String>, isAchieved: Boolean){
        mDao.setAchievedGoal(ids, isAchieved)
    }

    fun deleteGoals(ids: List<String>){
        mHandler.post { mDao.deleteGoals(ids) }
    }

    fun deleteGoal(goal: Goal){
        mHandler.post { mDao.deleteGoal(goal) }
    }

    fun updateGoal(goal: Goal) {
        mHandler.post { mDao.updateGoal(goal) }
    }

    fun updateGoals(goals: List<Goal>) {
        mHandler.post { mDao.updateGoals(goals) }
    }

    fun addGoal(goal: Goal) {
        mHandler.post { mDao.addGoal(goal) }
    }


    companion object {
        private var INSTANCE: GoalRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = GoalRepository(context)
            }
        }

        fun get(): GoalRepository {
            return INSTANCE ?: throw IllegalStateException("GoalRepository must be initialized")
        }
    }
}

