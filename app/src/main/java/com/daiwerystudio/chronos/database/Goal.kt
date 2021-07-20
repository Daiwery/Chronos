package com.daiwerystudio.chronos.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


private const val GOAL_DATABASE_NAME = "goal-database"

@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var parent: String? = null,
    var name: String = "",
    var note: String = "",
    var isAchieved: Boolean = false
) : Serializable

class GoalTypeConverters {
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }
}

@Dao
interface GoalTypeDao {
    @Query("SELECT * FROM goal_table WHERE parent='' AND isAchieved=(:isAchieved)")
    fun getGoalsWithoutParentFromSolve(isAchieved: Boolean): LiveData<List<Goal>>

    @Query("SELECT * FROM goal_table WHERE parent=(:id)")
    fun getGoalsFromParent(id: String): LiveData<List<Goal>>

    // Это функция нужна для реккурентного удаления всех goals от какого-то parent (см. ниже) (и не только)
    @Query("SELECT * FROM goal_table WHERE parent=(:id)")
    fun getGoalsFromParentAsList(id: String): List<Goal>

    @Query("SELECT 100*AVG(isAchieved=1) FROM goal_table WHERE parent=(:id)")
    fun getPercentAchieved(id: String): LiveData<Int>

    @Update
    fun updateGoal(goal: Goal)

    @Insert
    fun addGoal(goal: Goal)

    @Delete
    fun deleteGoal(goal: Goal)
}


@Database(entities = [Goal::class], version=1, exportSchema = false)
@TypeConverters(GoalTypeConverters::class)
abstract class GoalDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalTypeDao
}


class GoalRepository private constructor(context: Context) {
    private val database: GoalDatabase = Room.databaseBuilder(
        context.applicationContext,
        GoalDatabase::class.java,
        GOAL_DATABASE_NAME).build()
    private val goalDao = database.goalDao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getGoalsWithoutParentFromSolve(isAchieved: Boolean): LiveData<List<Goal>> = goalDao.getGoalsWithoutParentFromSolve(isAchieved)
    fun getGoalsFromParent(id: String): LiveData<List<Goal>> = goalDao.getGoalsFromParent(id)
    fun getPercentAchieved(id: String): LiveData<Int> = goalDao.getPercentAchieved(id)

    fun updateGoal(goal: Goal) {
        executor.execute {
            goalDao.updateGoal(goal)
        }
    }

    fun addGoal(goal: Goal) {
        executor.execute {
            goalDao.addGoal(goal)
        }
    }

    fun deleteGoalWithChild(goal: Goal){
        executor.execute {
            val childGoals = goalDao.getGoalsFromParentAsList(goal.id.toString())
            goalDao.deleteGoal(goal)
            for (childGoal in childGoals){
                deleteGoalWithChild(childGoal)
            }
        }
    }

    fun setAchievedGoalWithChild(goal: Goal){
        executor.execute {
            goal.isAchieved = true
            goalDao.updateGoal(goal)

            val childGoals = goalDao.getGoalsFromParentAsList(goal.id.toString())
            for (childGoal in childGoals){
                setAchievedGoalWithChild(childGoal)
            }
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
            return INSTANCE ?: throw IllegalStateException("ActRepository must beinitialized")
        }
    }
}

