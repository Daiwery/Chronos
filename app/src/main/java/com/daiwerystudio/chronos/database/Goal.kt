/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.daiwerystudio.chronos.ui.union.ID
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors

private const val GOAL_DATABASE_NAME = "goal-database"

/**
 * @property id уникальный идентификатор
 * @property name название цели.
 * @property note заметка в цели.
 * @property isAchieved выполнена ли цель или нет.
 */
@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey override val id: String,
    var name: String = "",
    var note: String = "",
    var isAchieved: Boolean = false,
) : Serializable, ID


@Dao
interface GoalTypeDao {
    @Query("SELECT * FROM goal_table WHERE id IN (:ids)")
    fun getGoals(ids: List<String>): LiveData<List<Goal>>

    @Query("SELECT * FROM goal_table WHERE id=(:id)")
    fun getGoal(id: String): LiveData<Goal>

    @Query("UPDATE goal_table SET isAchieved=(:isAchieved) WHERE id IN (:ids)")
    fun setAchievedGoal(ids: List<String>, isAchieved: Boolean)

    @Query("DELETE FROM goal_table WHERE id IN (:ids)")
    fun deleteGoals(ids: List<String>)

    @Update
    fun updateGoal(goal: Goal)

    @Insert
    fun addGoal(goal: Goal)
}


@Database(entities = [Goal::class], version=1, exportSchema=false)
abstract class GoalDatabase : RoomDatabase() {
    abstract fun dao(): GoalTypeDao
}

/**
 * Является синглтоном и инициализируется в DataBaseApplication.
 * @see DataBaseApplication
 */
class GoalRepository private constructor(context: Context) {
    private val mDatabase: GoalDatabase = Room.databaseBuilder(context.applicationContext,
        GoalDatabase::class.java,
        GOAL_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mExecutor = Executors.newSingleThreadExecutor()


    fun getGoals(ids: List<String>): LiveData<List<Goal>> = mDao.getGoals(ids)

    fun getGoal(id: String): LiveData<Goal> = mDao.getGoal(id)


    fun setAchievedGoal(ids: List<String>, isAchieved: Boolean){
        mDao.setAchievedGoal(ids, isAchieved)
    }

    fun deleteGoals(ids: List<String>){
        mExecutor.execute{
            mDao.deleteGoals(ids)
        }
    }

    fun updateGoal(goal: Goal) {
        mExecutor.execute {
            mDao.updateGoal(goal)
        }
    }

    fun addGoal(goal: Goal) {
        mExecutor.execute {
            mDao.addGoal(goal)
        }
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

