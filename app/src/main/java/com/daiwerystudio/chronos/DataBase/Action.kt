package com.daiwerystudio.chronos.DataBase

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


private const val ACTION_DATABASE_NAME = "action-database"

@Entity(tableName = "action_table")
data class Action(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var idActionType: UUID,
    var startTime: Long,
    var endTime: Long
) : Serializable

class ActionTypeConverters {
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
interface ActionDao {
    @Query("SELECT * FROM action_table WHERE (:time1) <= startTime <= (:time2) OR (:time1) <= endTime <= (:time2)")
    fun getActionFromTimes(time1: Long, time2: Long): LiveData<List<Action>>

    @Update
    fun updateAction(action: Action)

    @Insert
    fun addAction(action: Action)

    @Delete
    fun deleteAction(action: Action)
}


@Database(entities = [Action::class], version=1, exportSchema = false)
@TypeConverters(ActionTypeConverters::class)
abstract class ActionDatabase : RoomDatabase() {
    abstract fun actionDao(): ActionDao
}


class ActionRepository private constructor(context: Context) {
    private val database: ActionDatabase = Room.databaseBuilder(
        context.applicationContext,
        ActionDatabase::class.java,
        ACTION_DATABASE_NAME).build()
    private val actionDao = database.actionDao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getActionFromTimes(time1: Long, time2: Long): LiveData<List<Action>> = actionDao.getActionFromTimes(time1, time2)

    fun updateAction(action: Action) {
        executor.execute {
            actionDao.updateAction(action)
        }
    }

    fun addAction(action: Action) {
        executor.execute {
            actionDao.addAction(action)
        }
    }


    companion object {
        private var INSTANCE: ActionRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ActionRepository(context)
            }
        }

        fun get(): ActionRepository {
            return INSTANCE ?: throw IllegalStateException("ActRepository must beinitialized")
        }
    }
}

