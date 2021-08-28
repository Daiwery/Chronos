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

/* Предупреждение: данные константы не используются в Data Binding в item_recycler_view_schedule */
const val TYPE_SCHEDULE_PERIODIC = 0
const val TYPE_SCHEDULE_ONCE = 1

const val TYPE_DAY_SCHEDULE_RELATIVE = 0
const val TYPE_DAY_SCHEDULE_ABSOLUTE = 1


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
 * @property isCorrupted содержится ли в расписании ошибка. Если да, то оно не будет
 * использоваться при создании расписания на день.
 */
@Entity(tableName = "schedule_table")
data class Schedule(
    @PrimaryKey override val id: String,
    var start: Long = System.currentTimeMillis(),
    var name: String = "",
    var type: Int,
    var countDays: Int = 7,
    var isActive: Boolean = true,
    var isCorrupted: Boolean = false
) : Serializable, ID


/**
 * @property id уникальный идентификатор.
 * @property scheduleID id расписания, к которому относится данное действие.
 * @property dayIndex номер дня. В once_schedules фиктивно.
 * @property type тип дня. Абсолютный (действия задаются конкретными временами)
 * или относительный (действия задаются временем после прошлого действия и длительностью).
 * @property startDayTime время начала дня. Используется для относительного расписания.
 */
@Entity(tableName = "day_schedule_table")
data class DaySchedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var scheduleID: String,
    var dayIndex: Int,
    var type: Int = TYPE_DAY_SCHEDULE_RELATIVE,
    var startDayTime: Long = 6*60*60*1000
) : Serializable



/**
 * @property id уникальный идентификатор.
 * @property dayID id дня, к которому относится данное действие.
 * @property indexList индекс действия в массиве. По нему сортируются действия, чтобы сохранить
 * порядок требуемый пользователем. Необходим только для относительного расписания.
 * @property actionTypeId id типа действия, к которому относится данное действие в расписании.
 * @property startTime фактическое время начала действия. В абсолютном расписании изменяется
 * напрямую пользователем, в относительном считается отдельно по двум последним свойствам.
 * Оно необходимо для всех алгоритмов обработки действий в расписании. Измеряется в секундах.
 * В относительном расписании может быть больше 24 часов по той причине, что начало дня идет с startDayTime.
 * В этом случае время находится в окне с 00:00 до startDayTime в СЛЕДУЮЩЕМ дне.
 * @property endTime фактическое время конца действия. А обсолютном расписании изменяется
 * напрямую пользователем, в отнсительном считается отдельно по двум последним свойствам.
 * Оно необходимо для всех алгоритмов обработки действий расписаний.
 * В относительном расписании может быть больше 24 часов по той причине, что начало дня идет с startDayTime.
 * В этом случае время находится в окне с 00:00 до startDayTime в СЛЕДУЮЩЕМ  дне.
 * @property startAfter время начала действия после окончания прошлого. В отсительном расписании
 * изменяется напрямую пользователем, в абсолютном фиктивно.
 * @property duration длительность действия. В отсительном расписании изменяется
 * напрямую пользователем, в абсолютном фиктивно. Измеряется в секундах.
 * @property isCorrupted испорченно ли действие. Действие становится таковым, если пересекается
 * с другим действием.
 */
@Entity(tableName = "action_schedule_table")
data class ActionSchedule(
    @PrimaryKey override val id: String = UUID.randomUUID().toString(),
    var dayID: String,
    var indexList: Int,
    var actionTypeId: String = "",
    var startTime: Long = 0,
    var endTime: Long = 0,
    var startAfter: Long = 0,
    var duration: Long = 0,
    var isCorrupted: Boolean = false
) : Serializable, ID


@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_table WHERE id=(:id) ORDER BY name")
    fun getSchedule(id: String): LiveData<Schedule>

    @Query("SELECT * FROM schedule_table WHERE id IN (:ids)")
    fun getSchedules(ids: List<String>): LiveData<List<Schedule>>

    @Query("SELECT * FROM schedule_table WHERE isActive=1 AND isCorrupted=0")
    fun getActiveAndNotCorruptSchedules(): LiveData<List<Schedule>>

    @Query("SELECT * FROM action_schedule_table " +
            "WHERE dayID=(:dayID) ORDER BY indexList ASC")
    fun getActionsRelativeScheduleFromDayID(dayID: String): LiveData<List<ActionSchedule>>

    @Query("SELECT * FROM action_schedule_table " +
            "WHERE dayID=(:dayID) ORDER BY startTime ASC")
    fun getActionsAbsoluteScheduleFromDayID(dayID: String): LiveData<List<ActionSchedule>>

    @Query("SELECT id FROM day_schedule_table WHERE scheduleID=(:scheduleID)")
    fun getIDsDaysScheduleFromScheduleID(scheduleID: String): LiveData<List<String>>

    @Query("SELECT * FROM day_schedule_table WHERE id=(:id)")
    fun getDaySchedule(id: String): LiveData<DaySchedule>

    @Query("DELETE FROM schedule_table WHERE id IN (:ids)")
    fun deleteSchedules(ids: List<String>)

    @Query("DELETE FROM day_schedule_table WHERE scheduleID IN (:scheduleIDs)")
    fun deleteDaysScheduleFromSchedulesID(scheduleIDs: List<String>)

    @Query("DELETE FROM action_schedule_table WHERE dayID IN " +
            "(SELECT id FROM day_schedule_table WHERE scheduleID IN (:scheduleIDs))")
    fun deleteActionsScheduleFromSchedulesID(scheduleIDs: List<String>)

    @Update
    fun updateSchedule(schedule: Schedule)

    @Insert
    fun addSchedule(schedule: Schedule)

    @Update
    fun updateDaySchedule(daySchedule: DaySchedule)

    @Insert
    fun addDaySchedule(daySchedule: DaySchedule)

    @Update
    fun updateActionSchedule(actionSchedule: ActionSchedule)

    @Insert
    fun addActionSchedule(actionSchedule: ActionSchedule)

    @Delete
    fun deleteActionSchedule(actionSchedule: ActionSchedule)
}


@Database(entities = [Schedule::class, DaySchedule::class, ActionSchedule::class], version=1, exportSchema=false)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun dao(): ScheduleDao
}

/**
 * Класс, с помощью которого можно обратится к базе данных из любой точки программы.
 * Является синглтоном и объявляется в DataBaseApplication.
 * @see DataBaseApplication
 */
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

    fun getActiveAndNotCorruptSchedules(): LiveData<List<Schedule>> =
        mDao.getActiveAndNotCorruptSchedules()

    fun getActionsRelativeScheduleFromDayID(dayID: String): LiveData<List<ActionSchedule>> =
        mDao.getActionsRelativeScheduleFromDayID(dayID)

    fun getActionsAbsoluteScheduleFromDayID(dayID: String): LiveData<List<ActionSchedule>> =
        mDao.getActionsAbsoluteScheduleFromDayID(dayID)

    fun getIDsDaysScheduleFromScheduleID(scheduleID: String): LiveData<List<String>> =
        mDao.getIDsDaysScheduleFromScheduleID(scheduleID)

    fun getDaySchedule(id: String): LiveData<DaySchedule> = mDao.getDaySchedule(id)


    fun deleteCompletelySchedules(ids: List<String>){
        mHandler.post {
            mDao.deleteActionsScheduleFromSchedulesID(ids)
            mDao.deleteDaysScheduleFromSchedulesID(ids)
            mDao.deleteSchedules(ids)
        }
    }

    fun createPeriodicSchedule(schedule: Schedule){
        mHandler.post {
            mDao.addSchedule(schedule)
            for (i in 0 until schedule.countDays)
                mDao.addDaySchedule(DaySchedule(scheduleID=schedule.id, dayIndex=i))
        }
    }

    fun createOnceSchedule(schedule: Schedule){
        mHandler.post {
            mDao.addSchedule(schedule)
            mDao.addDaySchedule(DaySchedule(scheduleID=schedule.id, dayIndex=0))
        }
    }

    fun updateSchedule(schedule: Schedule){
        mHandler.post { mDao.updateSchedule(schedule) }
    }

    fun updateDaySchedule(daySchedule: DaySchedule){
        mHandler.post { mDao.updateDaySchedule(daySchedule) }
    }

    fun updateActionSchedule(actionSchedule: ActionSchedule){
        mHandler.post { mDao.updateActionSchedule(actionSchedule) }
    }

    fun addActionSchedule(actionSchedule: ActionSchedule){
        mHandler.post { mDao.addActionSchedule(actionSchedule) }
    }

    fun deleteActionSchedule(actionSchedule: ActionSchedule){
        mHandler.post { mDao.deleteActionSchedule(actionSchedule) }
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
