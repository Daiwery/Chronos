/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors

/**
 * Возможная модификация: всю логику обрабатки actions schedule в расписании (см. TimeView
 * в ClockViewGroup) перенести на плечи SQLite, а не делать это в котлине.
 *
 * Возмодная модификация: расчеты startTime и endTime в относительных расписаниях делать в
 * SQLite, а не во фрагменте.
 */

/**
 * Константа для хранения имени базы данных.
 * */
private const val SCHEDULE_DATABASE_NAME = "schedule-database"

/**
 * Константы типов расписания.
 *
 * Предупреждение: эти константы не используются в Data Binding (ActionScheduleDialog).
 * При смене значений, не забудь поменять и там.
 */
const val TYPE_SCHEDULE_RELATIVE = 0
const val TYPE_SCHEDULE_ABSOLUTE = 1

/**
 * Класс для хранения расписания и его параметров в базе данных.
 *
 * Интерфейс Serializable необходим для передачи объекта класса в пакете Bundle.
 *
 * @property id уникальный идентификатор.
 * @property dayStart день, с которого начинает работать данное расписание. Это значение
 * само по себе фиктивное. Но оно необходимо для определения номера дня расписания по
 * текущему дню.
 * @property name имя. UI.
 * @property countDays количество дней в расписании. То есть через сколько дней оно повторяется.
 * @property isActive активно ли расписание.
 * @property type тип расписание. Абсолютный (действия задаются конкретными временами)
 * или относительный (действия задаются временем после прошлого действия и длительностью).
 * @property defaultStartDayTime стандартное время пробуждение. Нужно для отсительного расписания
 * как начало отсчета. В секундах.
 * @property isCorrupted содержится ли в расписании ошибка. Если да, то оно не будет
 * использоваться при создании расписания на день.
 */
@Entity(tableName = "schedule_table")
data class Schedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var dayStart: Long = System.currentTimeMillis()/(1000*60*60*24),
    var name: String = "",
    var countDays: Int = 7,
    var isActive: Boolean = true,
    var type: Int = TYPE_SCHEDULE_RELATIVE,
    var defaultStartDayTime: Long = 6*60*60,
    var isCorrupted: Boolean = false
) : Serializable

/**
 * Класс для хранения действия в расписании.
 *
 * Интерфейс Serializable необходим для передачи объекта класса в пакете Bundle.
 *
 * @property id уникальный идентификатор.
 * @property scheduleID id расписания, к которому относится данное действие.
 * @property dayIndex номер дня, к которому относится данное действие.
 * @property indexList индекс действия в массиве. По нему сортируются действия, чтобы сохранить
 * порядок требуемый пользователем. Необходим только для относительного расписания. Также требуется,
 * чтобы indexList был индексом в массиве (т.е. все соседние отличались на 1).
 * Это нужно для расчета startTime и endTime. См. getActionsRelativeScheduleFromDayIndex
 * @property actionTypeId id типа действия, к которому относится данное действие в расписании.
 * @property startTime фактическое время начала действия. В абсолютном расписании изменяется
 * напрямую пользователем, в относительном считается отдельно по двум последним свойствам.
 * Оно необходимо для всех алгоритмов обработки действий расписаний. В секундах. В относительном
 * расписании может быть больше 24 часов по той причине, что начало дня идет с defaultStartDayTime.
 * В этом случае время находится в окне с 00:00 до defaultStartDayTime в СЛЕДУЮЩЕМ дне.
 * @property endTime фактическое время конца действия. А обсолютном расписании изменяется
 * напрямую пользователем, в отнсительном считается отдельно по двум последним свойствам.
 * Оно необходимо для всех алгоритмов обработки действий расписаний. В секундах. В относительном
 * расписании может быть больше 24 часов по той причине, что начало дня идет с defaultStartDayTime.
 * В этом случае время находится в окне с 00:00 до defaultStartDayTime в СЛЕДУЮЩЕМ  дне.
 * @property startAfter время начала действия после окончания прошлого. В отсительном расписании
 * изменяется напрямую пользователем, в абсолютном фиктивно. В секундах.
 * @property duration длительность действия. В отсительном расписании изменяется
 * напрямую пользователем, в абсолютном фиктивно. В секундах.
 * @property isCorrupted содержится ли в действии ошибка.
 */
@Entity(tableName = "actions_schedule_table")
data class ActionSchedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var scheduleID: String,
    var dayIndex: Int,
    var indexList: Int,
    var actionTypeId: String = "",
    var startTime: Long = 0,
    var endTime: Long = 0,
    var startAfter: Long = 0,
    var duration: Long = 0,
    var isCorrupted: Boolean = false
) : Serializable

/**
 * Вспомогательный класс. Нужен только для UI. Ctrl+B.
 * @property dayIndex индекс дня в расписании.
 * @property countCorrupted количество испорченных действий
 */
data class DaySchedule(
    var dayIndex: Int,
    var countCorrupted: Int
)


/**
 * С помощью этого интерфейса выплоняются запросы к базе данных.
 *
 * Запросы написаны на SQLite.
 */
@Dao
interface ScheduleDao {
    /**
     * Возвращает массив расписаний в обертке LiveData в зависимости от isActive
     * и сортируем по имени.
     */
    @Query("SELECT * FROM schedule_table WHERE isActive=(:isActive) ORDER BY name ASC")
    fun getSchedulesFromActive(isActive: Boolean): LiveData<List<Schedule>>

    /**
     * Возвращает расписание с заданным id в обертке LiveData.
     */
    @Query("SELECT * FROM schedule_table WHERE id=(:id)")
    fun getSchedule(id: String): LiveData<Schedule>

    /**
     * Возвращает массив actions schedule в обертке LiveData в зависимости от id расписания
     * и dayIndex. Используется для относительных расписаний, так как сортирует по indexList.
     * Дополнительно расчитывает startTime и endTime, так как это нужно для алгоритма обработки.
     */
    @Query("SELECT * FROM actions_schedule_table " +
            "WHERE scheduleId=(:scheduleID) AND dayIndex=(:dayIndex)" +
            "ORDER BY indexList ASC")
    fun getActionsRelativeScheduleFromDayIndex(scheduleID: String, dayIndex: Int): LiveData<List<ActionSchedule>>

    /**
     * Возвращает массив действий в обертке LiveData в зависимости от id расписания
     * и dayIndex. Используется для абсолютных расписаний, так как сортирует по startTime.
     */
    @Query("SELECT * FROM actions_schedule_table " +
            "WHERE scheduleId=(:scheduleID) AND dayIndex=(:dayIndex)" +
            "ORDER BY startTime ASC")
    fun getActionsAbsoluteScheduleFromDayIndex(scheduleID: String, dayIndex: Int): LiveData<List<ActionSchedule>>

    /**
     * Удаляет все действия в заданном расписании.
     */
    @Query("DELETE FROM actions_schedule_table WHERE scheduleID=(:scheduleID)")
    fun deleteActionsScheduleFromTimetableId(scheduleID: String)

    /**
     * Получает количество активных и испорченных расписаний в обертке LiveData.
     * Нужен для UI.
     */
    @Query("SELECT COUNT(*) FROM schedule_table WHERE isActive=1 AND isCorrupted=1")
    fun getCountActiveCorruptedSchedules(): LiveData<Int>

    /**
     * Получает масив из вспомогательного класса. Нужен для UI.
     */
    @Query("WITH RECURSIVE sub_table(dayIndex) " +
            "AS ( SELECT 0 UNION ALL SELECT dayIndex+1 FROM sub_table " +
            "WHERE dayIndex+1<(SELECT countDays FROM schedule_table WHERE id=(:scheduleID))) " +
            "SELECT dayIndex, " +
            "(SELECT COUNT(*) from actions_schedule_table " +
            "WHERE actions_schedule_table.dayIndex=sub_table.dayIndex " +
            "AND isCorrupted=1 AND scheduleID=(:scheduleID)) countCorrupted " +
            "FROM sub_table")
    fun getCorruptedDays(scheduleID: String): LiveData<List<DaySchedule>>

    /**
     * Обновляет расписание в базе данных.
     */
    @Update
    fun updateSchedule(schedule: Schedule)

    /**
     * Добавляет новое расписание в базу данных.
     */
    @Insert
    fun addSchedule(schedule: Schedule)

    /**
     * Удаляет расписание из базы данных.
     */
    @Delete
    fun deleteSchedule(schedule: Schedule)

    /**
     * Обновляет действие в базе данных.
     */
    @Update
    fun updateActionSchedule(actionSchedule: ActionSchedule)

    /**
     * Обновляет список действий в базе данных. Используется для установки indexList.
     */
    @Update
    fun updateListActionsSchedule(listActionsSchedule: List<ActionSchedule>)

    /**
     * Добавляет новое действие в базу данных.
     */
    @Insert
    fun addActionSchedule(actionSchedule: ActionSchedule)

    /**
     * Удаляет действие из базы данных.
     */
    @Delete
    fun deleteActionSchedule(actionSchedule: ActionSchedule)
}

/**
 * Класс базы данных с указанием, что нужно хранить и какой номер версии.
 */
@Database(entities = [Schedule::class, ActionSchedule::class], version=1, exportSchema=false)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun dao(): ScheduleDao
}

/**
 * Класс, с помощью которого можно обратится к базе данных из любой точки программы.
 * Является синглтоном и объявляется в DataBaseApplication.
 * @see DataBaseApplication
 */
class ScheduleRepository private constructor(context: Context) {
    /**
     * Объект класса базы данных.
     */
    private val mDatabase: ScheduleDatabase = Room.databaseBuilder(context.applicationContext,
        ScheduleDatabase::class.java,
        SCHEDULE_DATABASE_NAME).build()
    /**
     * Интрейфс DAO для взаимодействия с базой данных.
     */
    private val mDao = mDatabase.dao()
    /**
     * Отдельный поток для обновления, добавления и удаления.
     */
    private val mExecutor = Executors.newSingleThreadExecutor()

    /**
     * Возвращает массив расписаний в обертке LiveData в зависимости от isActive
     * и сортируем по имени.
     */
    fun getSchedulesFromActive(isActive: Boolean): LiveData<List<Schedule>> =
        mDao.getSchedulesFromActive(isActive)

    /**
     * Возвращает массив действий в обертке LiveData в зависимости от id расписания
     * и dayIndex. Используется для абсолютных расписаний, так как сортирует по startTime.
     */
    fun getActionsAbsoluteScheduleFromDayIndex(scheduleID: String, dayIndex: Int):
            LiveData<List<ActionSchedule>> = mDao.getActionsAbsoluteScheduleFromDayIndex(scheduleID, dayIndex)

    /**
     * Возвращает массив actions schedule в обертке LiveData в зависимости от id расписания
     * и dayIndex. Используется для относительных расписаний, так как сортирует по indexList.
     */
    fun getActionsRelativeScheduleFromDayIndex(scheduleID: String, dayIndex: Int):
            LiveData<List<ActionSchedule>> = mDao.getActionsRelativeScheduleFromDayIndex(scheduleID, dayIndex)

    /**
     * Возвращает расписание с заданным id в обертке LiveData.
     */
    fun getSchedule(id: String): LiveData<Schedule> = mDao.getSchedule(id)

    /**
     * Получает количеств активных и испорченных расписаний в обертке LiveData.
     * Нужен в MainActivity для установки бейджа.
     */
    fun getCountActiveCorruptedSchedules(): LiveData<Int> = mDao.getCountActiveCorruptedSchedules()

    /**
     * Извлекает из таблицы словарь вида: индекс дня - количество испорченных действий.
     * Нужен для UI.
     */
    fun getCorruptedDays(scheduleID: String): LiveData<List<DaySchedule>> =
        mDao.getCorruptedDays(scheduleID)

    /**
     * Обновляет расписание в базе данных в отдельном потоке.
     */
    fun updateSchedule(schedule: Schedule){
        mExecutor.execute {
            mDao.updateSchedule(schedule)
        }
    }

    /**
     * Добавляет новое расписание в базу данных в отдельном потоке.
     */
    fun addSchedule(schedule: Schedule){
        mExecutor.execute {
            mDao.addSchedule(schedule)
        }
    }

    /**
     * Удаляет расписание со всеми действиями в отдельном потоке.
     */
    fun deleteScheduleWithActions(schedule: Schedule){
        mExecutor.execute {
            mDao.deleteSchedule(schedule)
            mDao.deleteActionsScheduleFromTimetableId(schedule.id)
        }
    }

    /**
     * Обновляет список действий в базе данных в отдельном потоке.
     * Используется для установки indexList.
     */
    fun updateListActionsSchedule(listActionSchedule: List<ActionSchedule>){
        mExecutor.execute {
            mDao.updateListActionsSchedule(listActionSchedule)
        }
    }

    /**
     * Обновляет действие в базе данных в отдельном потоке.
     */
    fun updateActionSchedule(actionSchedule: ActionSchedule){
        mExecutor.execute {
            mDao.updateActionSchedule(actionSchedule)
        }
    }

    /**
     * Добавляет новое действие в базу данных в отдельном потоке.
     */
    fun addActionSchedule(actionSchedule: ActionSchedule){
        mExecutor.execute {
            mDao.addActionSchedule(actionSchedule)
        }
    }

    /**
     * Удаляет действие из базы данных в отдельном потоке.
     */
    fun deleteActionSchedule(actionSchedule: ActionSchedule){
        mExecutor.execute {
            mDao.deleteActionSchedule(actionSchedule)
        }
    }

    /**
     * Создаение экземпляра синглтона.
     */
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
