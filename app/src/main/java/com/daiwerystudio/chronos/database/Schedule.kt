package com.daiwerystudio.chronos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


private const val SCHEDULE_DATABASE_NAME = "schedule-database"

const val TYPE_SCHEDULE_RELATIVE = 0
const val TYPE_SCHEDULE_ABSOLUTE = 1


@Entity(tableName = "schedule_table")
data class Schedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var dayStart: Long = System.currentTimeMillis()/(1000*60*60*24),
    var name: String = "",
    var countDays: Int = 7,
    var isActive: Boolean = true,
    var type: Int = TYPE_SCHEDULE_RELATIVE,
    var defaultStartDayTime: Long = 6*60*60,
) : Serializable


@Entity(tableName = "actions_schedule_table")
data class ActionSchedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var scheduleId: String,
    var dayIndex: Int,
    var indexList: Int,
    var actionTypeId: String = "",

    // Мне не совсем это нравится. Похоже на костыль. Но все работает.
    // В конце концов, есть другие варианты записать время?
    var startTime: Long = 0,
    var endTime: Long = 0,
    var startAfter: Long = 0,
    var duration: Long = 0
) : Serializable



@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_table WHERE isActive=(:isActive)")
    fun getSchedulesFromActive(isActive: Boolean): LiveData<List<Schedule>>

    @Query("SELECT * FROM schedule_table WHERE id=(:id)")
    fun getSchedule(id: String): LiveData<Schedule>

    @Query("SELECT * FROM actions_schedule_table " +
            "WHERE scheduleId=(:scheduleId) AND dayIndex=(:dayIndex)" +
            "ORDER BY indexList ASC")
    fun getActionsRelativeScheduleFromDayIndex(scheduleId: String, dayIndex: Int): LiveData<List<ActionSchedule>>

    @Query("SELECT * FROM actions_schedule_table " +
            "WHERE scheduleId=(:scheduleId) AND dayIndex=(:dayIndex)" +
            "ORDER BY startTime ASC")
    fun getActionsAbsoluteScheduleFromDayIndex(scheduleId: String, dayIndex: Int): LiveData<List<ActionSchedule>>

    @Query("DELETE FROM actions_schedule_table WHERE scheduleId=(:scheduleId)")
    fun deleteActionsScheduleFromTimetableId(scheduleId: String)

    @Update
    fun updateSchedule(schedule: Schedule)

    @Insert
    fun addSchedule(schedule: Schedule)

    @Delete
    fun deleteSchedule(schedule: Schedule)

    @Update
    fun updateActionSchedule(actionSchedule: ActionSchedule)

    @Update
    fun updateListActionsSchedule(listActionsSchedule: List<ActionSchedule>)

    @Insert
    fun addActionSchedule(actionSchedule: ActionSchedule)

    @Delete
    fun deleteActionSchedule(actionSchedule: ActionSchedule)
}


@Database(entities = [Schedule::class, ActionSchedule::class], version=1, exportSchema=false)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun dao(): ScheduleDao
}


class ScheduleRepository private constructor(context: Context) {
    private val database: ScheduleDatabase =
        Room.databaseBuilder(context.applicationContext, ScheduleDatabase::class.java, SCHEDULE_DATABASE_NAME).build()
    private val dao = database.dao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getSchedulesFromActive(isActive: Boolean): LiveData<List<Schedule>> = dao.getSchedulesFromActive(isActive)

    fun getActionsAbsoluteScheduleFromDayIndex(timetableId: String, dayIndex: Int):
            LiveData<List<ActionSchedule>> = dao.getActionsAbsoluteScheduleFromDayIndex(timetableId, dayIndex)

    fun getActionsRelativeScheduleFromDayIndex(timetableId: String, dayIndex: Int):
            LiveData<List<ActionSchedule>> = dao.getActionsRelativeScheduleFromDayIndex(timetableId, dayIndex)

    fun getSchedule(id: String): LiveData<Schedule> = dao.getSchedule(id)

    fun updateSchedule(schedule: Schedule){
        executor.execute {
            dao.updateSchedule(schedule)
        }
    }

    fun addSchedule(schedule: Schedule){
        executor.execute {
            dao.addSchedule(schedule)
        }
    }

    fun deleteScheduleWithActions(schedule: Schedule){
        executor.execute {
            dao.deleteSchedule(schedule)
            dao.deleteActionsScheduleFromTimetableId(schedule.id)
        }
    }

    fun updateListActionsSchedule(listActionSchedule: List<ActionSchedule>){
        executor.execute {
            dao.updateListActionsSchedule(listActionSchedule)
        }
    }

    fun updateActionSchedule(actionSchedule: ActionSchedule){
        executor.execute {
            dao.updateActionSchedule(actionSchedule)
        }
    }

    fun addActionSchedule(actionSchedule: ActionSchedule){
        executor.execute {
            dao.addActionSchedule(actionSchedule)
        }
    }

    fun deleteActionSchedule(actionSchedule: ActionSchedule){
        executor.execute {
            dao.deleteActionSchedule(actionSchedule)
        }
    }


    companion object {
        private var INSTANCE: ScheduleRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ScheduleRepository(context)
            }
        }

        fun get(): ScheduleRepository {
            return INSTANCE ?: throw IllegalStateException("ScheduleRepository must be initialized")
        }
    }
}
