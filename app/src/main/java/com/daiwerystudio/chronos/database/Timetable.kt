package com.daiwerystudio.chronos.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


private const val TIMETABLE_DATABASE_NAME = "timetable-database"


@Entity(tableName = "timetable_table")
data class Timetable(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var countDays: Int = 7,
    var isActive: Boolean = true,
    var dayStart: Long = System.currentTimeMillis()/(1000*60*60*24)    // Days
) : Serializable


@Entity(tableName = "actions_timetable_table")
data class ActionTimetable(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var timeStart: Int = 5,     // Minutes
    var duration: Int = 90,     // Minutes
    var actionTypeId: String = "",
    var timetableId: String,
    var dayIndex: Int,
    var indexList: Int,
) : Serializable


@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_table WHERE isActive=(:isActive)")
    fun getTimetableFromActive(isActive: Boolean): LiveData<List<Timetable>>


    @Query("SELECT * FROM actions_timetable_table " +
            "WHERE timetableId=(:timetableId) AND dayIndex=(:dayIndex)" +
            "ORDER BY indexList ASC")
    fun getActionsTimetableFromDayIndex(timetableId: String, dayIndex: Int): LiveData<MutableList<ActionTimetable>>


    @Query("DELETE FROM actions_timetable_table WHERE timetableId=(:timetableId)")
    fun deleteActionsTimetableFromTimetableId(timetableId: String)


    @Query("SELECT * FROM timetable_table WHERE id=(:id)")
    fun getTimetable(id: String): LiveData<Timetable>


    @Update
    fun updateTimetable(timetable: Timetable)
    @Insert
    fun addTimetable(timetable: Timetable)
    @Delete
    fun deleteTimetable(timetable: Timetable)

    @Update
    fun updateActionTimetable(actionTimetable: ActionTimetable)
    @Update
    fun updateListActionTimetable(listActionTimetable: List<ActionTimetable>)
    @Insert
    fun addActionTimetable(actionTimetable: ActionTimetable)
    @Delete
    fun deleteActionTimetable(actionTimetable: ActionTimetable)
}


@Database(entities = [Timetable::class, ActionTimetable::class], version=1, exportSchema=false)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun dao(): TimetableDao
}


class TimetableRepository private constructor(context: Context) {
    private val database: TimetableDatabase = Room.databaseBuilder(
        context.applicationContext,
        TimetableDatabase::class.java,
        TIMETABLE_DATABASE_NAME).build()
    private val dao = database.dao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getTimetableFromActive(isActive: Boolean): LiveData<List<Timetable>> =
        dao.getTimetableFromActive(isActive)
    fun getActionsTimetableFromDayIndex(timetableId: String, dayIndex: Int): LiveData<MutableList<ActionTimetable>> =
        dao.getActionsTimetableFromDayIndex(timetableId, dayIndex)
    fun getTimetable(id: String): LiveData<Timetable> = dao.getTimetable(id)


    fun updateTimetable(timetable: Timetable){
        executor.execute {
            dao.updateTimetable(timetable)
        }
    }
    fun addTimetable(timetable: Timetable){
        executor.execute {
            dao.addTimetable(timetable)
        }
    }
    fun deleteTimetableWithActions(timetable: Timetable){
        executor.execute {
            dao.deleteTimetable(timetable)
            dao.deleteActionsTimetableFromTimetableId(timetable.id)
        }
    }


    fun updateListActionTimetable(listActionTimetable: List<ActionTimetable>){
        executor.execute {
            dao.updateListActionTimetable(listActionTimetable)
        }
    }
    fun updateActionTimetable(actionTimetable: ActionTimetable){
        executor.execute {
            dao.updateActionTimetable(actionTimetable)
        }
    }
    fun addActionTimetable(actionTimetable: ActionTimetable){
        executor.execute {
            dao.addActionTimetable(actionTimetable)
        }
    }
    fun deleteActionTimetable(actionTimetable: ActionTimetable){
        executor.execute {
            dao.deleteActionTimetable(actionTimetable)
        }
    }


    companion object {
        private var INSTANCE: TimetableRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = TimetableRepository(context)
            }
        }

        fun get(): TimetableRepository {
            return INSTANCE ?: throw IllegalStateException("TimetableRepository must beinitialized")
        }
    }
}
