/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 19.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union. Добавлена таблица "day_schedule_table"
* и логика взаимодействия с ней. Вместо отдельного потока добавлен отдельный looper.
*
* Дата изменения: 23.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлен тип расписания: периодический или на конкретный день.
*
* Дата изменения: 11.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: удаление таблицы дня в расписании. Теперь только один тип.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.room.*
import com.daiwerystudio.chronos.ui.union.ID
import java.io.Serializable
import java.util.*

private const val SCHEDULE_DATABASE_NAME = "schedule-database"

/* Предупреждение: данные константы не используются в getActionsScheduleFromDay*/
const val TYPE_SCHEDULE_PERIODIC = 0
const val TYPE_SCHEDULE_ONCE = 1


/**
 * @property id уникальный идентификатор.
 * @property start время, с которого начинает работать данное расписание. На работу оасписания не
 * влияет точное время, только день. Знание точного времени необходимо для учета локального времени.
 * @property name имя. UI.
 * @property type тип расписания. Периодический или на конкретный день.
 * @property countDays количество дней в расписании. То есть через сколько дней оно повторяется.
 * Нужно помнить, что при обновении расписания, количество дней не должно меняться,
 * так как база данных "day_schedule_table" при этом никак не меняется. В once_schedules фиктивно.
 * @property isActive активно ли расписание.
 */
@Entity(tableName = "schedule_table")
data class Schedule(
    @PrimaryKey override val id: String,
    var start: Long = System.currentTimeMillis(),
    var name: String = "",
    var type: Int,
    var countDays: Int = 7,
    var isActive: Boolean = true,
) : Serializable, ID

/**
 * @property id уникальный идентификатор.
 * @property scheduleID id расписания, к которому относится данное действие.
 * @property dayIndex номер дня, к которому относится данное действие.
 * @property actionTypeID id типа действия, к которому относится данное действие в расписании.
 * @property startTime фактическое время начала действия.
 * В этом случае время находится в окне с 00:00 до startDayTime в СЛЕДУЮЩЕМ дне.
 * @property endTime фактическое время конца действия.
 */
@Entity(tableName = "action_schedule_table")
data class ActionSchedule(
    @PrimaryKey override val id: String = UUID.randomUUID().toString(),
    var scheduleID: String,
    var dayIndex: Int,
    var actionTypeID: String = "",
    var startTime: Long = 0,
    var endTime: Long = 0,
) : Serializable, ID


@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_table WHERE id=(:id) ORDER BY name")
    fun getSchedule(id: String): LiveData<Schedule>

    @Query("SELECT * FROM schedule_table WHERE id IN (:ids)")
    fun getSchedules(ids: List<String>): LiveData<List<Schedule>>

    @Query("SELECT * FROM action_schedule_table " +
            "WHERE scheduleID=(:scheduleID) AND dayIndex=(:dayIndex) " +
            "ORDER BY startTime ASC")
    fun getActionsScheduleFromSchedule(scheduleID: String, dayIndex: Int): LiveData<List<ActionSchedule>>

    // Используются абсолютные значения для типа, а не константы.
    @Query("SELECT a.id, a.scheduleID, a.dayIndex, a.actionTypeId, a.startTime, a.endTime FROM action_schedule_table AS a, " +
            "(SELECT id, countDays, start, type FROM schedule_table WHERE isActive=1) AS b " +
            "WHERE a.scheduleID=b.id AND (:day)>=(b.start+(:local))/(1000*60*60*24) " +
            "AND ((a.dayIndex=((:day)-(b.start+(:local))/(1000*60*60*24))%b.countDays AND b.type=0) OR " +
            "(a.dayIndex=(:day)-(b.start+(:local))/(1000*60*60*24) AND b.type=1))")
    fun getActionsScheduleFromDay(day: Long, local: Int): LiveData<List<ActionSchedule>>

    @Query("UPDATE schedule_table SET isActive=(:isActive) WHERE id IN (:ids)")
    fun setActivitySchedules(ids: List<String>, isActive: Boolean)

    @Query("DELETE FROM schedule_table WHERE id IN (:ids)")
    fun deleteSchedules(ids: List<String>)

    @Query("DELETE FROM action_schedule_table WHERE scheduleID IN (:scheduleIDs)")
    fun deleteActionsScheduleFromSchedulesID(scheduleIDs: List<String>)

    @Query("DELETE FROM action_schedule_table WHERE id IN (:ids)")
    fun deleteActionsSchedule(ids: List<String>)

    @Update
    fun updateSchedule(schedule: Schedule)

    @Insert
    fun addSchedule(schedule: Schedule)

    @Update
    fun updateActionSchedule(actionSchedule: ActionSchedule)

    @Insert
    fun addActionSchedule(actionSchedule: ActionSchedule)
}


@Database(entities = [Schedule::class, ActionSchedule::class], version=1, exportSchema=false)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun dao(): ScheduleDao
}

class ScheduleRepository private constructor(context: Context) {
    private val mDatabase: ScheduleDatabase = Room.databaseBuilder(context.applicationContext,
        ScheduleDatabase::class.java,
        SCHEDULE_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mHandlerThread = HandlerThread("ScheduleRepository")
    private var mHandler: Handler

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }


    fun getSchedule(id: String): LiveData<Schedule> = mDao.getSchedule(id)

    fun getSchedules(ids: List<String>): LiveData<List<Schedule>> = mDao.getSchedules(ids)

    fun getActionsScheduleFromSchedule(scheduleID: String, dayIndex: Int): LiveData<List<ActionSchedule>> =
        mDao.getActionsScheduleFromSchedule(scheduleID, dayIndex)

    fun getActionsScheduleFromDay(day: Long, local: Int): LiveData<List<ActionSchedule>> =
        mDao.getActionsScheduleFromDay(day, local)

    fun completelyDeleteSchedules(ids: List<String>){
        mHandler.post {
            mDao.deleteActionsScheduleFromSchedulesID(ids)
            mDao.deleteSchedules(ids)
        }
    }

    fun setActivitySchedules(ids: List<String>, isActive: Boolean){
        mDao.setActivitySchedules(ids, isActive)
    }

    fun addSchedule(schedule: Schedule){
        mHandler.post { mDao.addSchedule(schedule) }
    }

    fun updateSchedule(schedule: Schedule){
        mHandler.post { mDao.updateSchedule(schedule) }
    }

    fun updateActionSchedule(actionSchedule: ActionSchedule){
        mHandler.post { mDao.updateActionSchedule(actionSchedule) }
    }

    fun addActionSchedule(actionSchedule: ActionSchedule){
        mHandler.post { mDao.addActionSchedule(actionSchedule) }
    }

    fun deleteActionsSchedule(ids: List<String>){
        mHandler.post { mDao.deleteActionsSchedule(ids) }
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
