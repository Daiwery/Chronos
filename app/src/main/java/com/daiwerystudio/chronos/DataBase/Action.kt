package com.daiwerystudio.chronos.DataBase

import android.content.Context
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
    var start: Long,
    var end: Long
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
    @Query("SELECT * FROM action_table WHERE (:time1) <= start <= (:time2) OR (:time1) <= end <= (:time2)")
    fun getActionsFromTimes(time1: Long, time2: Long): LiveData<List<Action>>

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

    fun getActionsFromTimes(time1: Long, time2: Long): LiveData<List<Action>> = actionDao.getActionsFromTimes(time1, time2)

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

