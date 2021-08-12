/*
* Дата создания: 08.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


/**
 * Константа для хранения имени базы данных.
 */
private const val ACTION_DATABASE_NAME = "action-database"

/**
 * Представляет из себя одну строку в базе данных.
 * Времена startTime и endTime - это времена не от начала дня, а от начала эпохи (без учета
 * часового пояса, он учитывается в UI)
 *
 * Интерфейс Serializable необходим для передачи объекта класса в пакете Bundle.
 * @property id уникальный идентификатор.
 * @property actionTypeId id типа действие
 * @property startTime начало действия. В секундах.
 * @property endTime конец действия. В секундах.
 */
@Entity(tableName = "action_table")
data class Action(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var actionTypeId: String="",
    var startTime: Long = System.currentTimeMillis()/1000-60*60,
    var endTime: Long = System.currentTimeMillis()/1000
) : Serializable

/**
 * С помощью этого интерфейса выполняются запросы к базе данных.
 *
 * Запросы написаны на SQLite.
 */
@Dao
interface ActionDao {
    /**
     * Возвращает все действия в обертке LiveData, у которых либо начало, либо конец находятся в указанном интервале.
     */
    @Query("SELECT * FROM action_table WHERE " +
            "(startTime >= (:time1) AND startTime <= (:time2)) " +
            "OR (endTime >= (:time1) AND endTime <= (:time2)) ORDER BY startTime")
    fun getActionsFromInterval(time1: Long, time2: Long): LiveData<List<Action>>

    /**
     * Обновляет заданное действие.
     */
    @Update
    fun updateAction(action: Action)

    /**
     * Добавляет заданное действие.
     */
    @Insert
    fun addAction(action: Action)

    /**
     * Удаляет заданное действие.
     */
    @Delete
    fun deleteAction(action: Action)
}

/**
 * Класс базы данных с указанием, что нужно хранить и какой номер версии.
 */
@Database(entities = [Action::class], version=1, exportSchema = false)
abstract class ActionDatabase : RoomDatabase() {
    abstract fun dao(): ActionDao
}

/**
 * Класс, с помощью которого можно обратится к базе данных из любой точки программы.
 * Является синглтоном и объявляется в DataBaseApplication.
 * @see DataBaseApplication
 */
class ActionRepository private constructor(context: Context) {
    /**
     * Объект класса базы данных.
     */
    private val mDatabase: ActionDatabase = Room.databaseBuilder(context.applicationContext,
        ActionDatabase::class.java,
        ACTION_DATABASE_NAME).build()
    /**
     * Интрейфс DAO для взаимодействия с базой данных.
     */
    private val mDao = mDatabase.dao()
    /**
     * Отдельный поток для обновления, добавления и удаления.
     */
    private val mExecutor = Executors.newSingleThreadExecutor()

    /**
     * Возвращает все действия в обертке LiveData, у которых либо начало,
     * либо конец находятся в указанном интервале.
     */
    fun getActionsFromInterval(time1: Long, time2: Long): LiveData<List<Action>> =
        mDao.getActionsFromInterval(time1, time2)

    /**
     * Обновляет заданное действие в отдельном потоке.
     */
    fun updateAction(action: Action) {
        mExecutor.execute {
            mDao.updateAction(action)
        }
    }

    /**
     * Добавляет заданное действие в отдельном потоке.
     */
    fun addAction(action: Action) {
        mExecutor.execute {
            mDao.addAction(action)
        }
    }

    /**
     * Удаляет заданное действие.
     */
    fun deleteAction(action: Action) {
        mExecutor.execute {
            mDao.deleteAction(action)
        }
    }

    /**
     * Создаение экземпляр синглтона.
     */
    companion object {
        private var INSTANCE: ActionRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ActionRepository(context)
            }
        }

        fun get(): ActionRepository {
            return INSTANCE ?: throw IllegalStateException("ActionRepository must beinitialized")
        }
    }
}

