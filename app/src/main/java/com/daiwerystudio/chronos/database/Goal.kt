package com.daiwerystudio.chronos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


private const val GOAL_DATABASE_NAME = "goal-database"

@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var parent: String = "",
    var name: String = "",
    var note: String = "",
    var isAchieved: Boolean = false,
    var indexList: Int = -1
) : Serializable


@Dao
interface GoalTypeDao {
    @Query("SELECT * FROM goal_table WHERE parent='' AND isAchieved=(:isAchieved) ORDER BY name ASC")
    fun getGoalsWithoutParentFromSolve(isAchieved: Boolean): LiveData<List<Goal>>

    @Query("SELECT * FROM goal_table WHERE parent=(:id) ORDER BY indexList ASC")
    fun getGoalsFromParent(id: String): LiveData<List<Goal>>

    @Query("WITH RECURSIVE sub_table(id, parent) " +
            "AS (SELECT id, parent " +
            "FROM goal_table " +
            "WHERE id=(:id) " +
            "UNION ALL " +
            "SELECT b.id, b.parent " +
            "FROM goal_table AS b " +
            "JOIN sub_table AS c ON c.id=b.parent) " +
            "DELETE FROM goal_table WHERE id IN (SELECT id FROM sub_table)")
    fun deleteGoalWithChild(id: String)

    @Query("WITH RECURSIVE sub_table(id, parent) " +
            "AS (SELECT id, parent " +
            "FROM goal_table " +
            "WHERE id=(:id) " +
            "UNION ALL " +
            "SELECT b.id, b.parent " +
            "FROM goal_table AS b " +
            "JOIN sub_table AS c ON c.id=b.parent) " +
            "UPDATE goal_table SET isAchieved=1 WHERE id IN (SELECT id FROM sub_table)")
    fun setAchievedGoalWithChild(id: String)

    @Query("SELECT 100*AVG(isAchieved=1) FROM goal_table WHERE parent=(:id)")
    fun getPercentAchieved(id: String): LiveData<Int>

    @Query("SELECT * FROM goal_table WHERE id=(:id)")
    fun getGoal(id: String): LiveData<Goal>

    @Update
    fun updateGoal(goal: Goal)

    @Update
    fun updateListGoals(listGoal: List<Goal>)

    @Insert
    fun addGoal(goal: Goal)

    @Delete
    fun deleteGoal(goal: Goal)
}


@Database(entities = [Goal::class], version=1, exportSchema = false)
abstract class GoalDatabase : RoomDatabase() {
    abstract fun dao(): GoalTypeDao
}


class GoalRepository private constructor(context: Context) {
    private val database: GoalDatabase =
        Room.databaseBuilder(context.applicationContext, GoalDatabase::class.java, GOAL_DATABASE_NAME).build()
    private val dao = database.dao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getGoalsWithoutParentFromSolve(isAchieved: Boolean): LiveData<List<Goal>> = dao.getGoalsWithoutParentFromSolve(isAchieved)

    fun getGoalsFromParent(id: String): LiveData<List<Goal>> = dao.getGoalsFromParent(id)

    fun getPercentAchieved(id: String): LiveData<Int> = dao.getPercentAchieved(id)

    fun getGoal(id: String): LiveData<Goal> = dao.getGoal(id)

    fun updateGoal(goal: Goal) {
        executor.execute {
            dao.updateGoal(goal)
        }
    }

    fun updateListGoals(listGoal: List<Goal>){
        executor.execute {
            dao.updateListGoals(listGoal)
        }
    }

    fun addGoal(goal: Goal) {
        executor.execute {
            dao.addGoal(goal)
        }
    }

    fun deleteGoalWithChild(goal: Goal){
        executor.execute {
            dao.deleteGoalWithChild(goal.id)
        }
    }

    fun setAchievedGoalWithChild(goal: Goal){
        executor.execute {
            dao.setAchievedGoalWithChild(goal.id)
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

