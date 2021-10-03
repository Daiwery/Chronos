/*
* Дата создания: 08.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 19.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: Вместо отдельного потока добавлен отдельный looper.
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

private const val ACTION_DATABASE_NAME = "action-database"

/**
 * @property id уникальный идентификатор.
 * @property actionTypeID id типа действие
 * @property startTime начало действия. Время от начала эпохи.
 * @property endTime конец действия. Время от начала эпохи.
 */
@Entity(tableName = "action_table")
data class Action(
    @PrimaryKey override val id: String = UUID.randomUUID().toString(),
    var actionTypeID: String="",
    var startTime: Long = System.currentTimeMillis()-60*60*1000,
    var endTime: Long = System.currentTimeMillis()
) : Serializable, ID


@Dao
interface ActionDao {
    @Query("SELECT * FROM action_table WHERE " +
            "(startTime >= (:time1) AND startTime <= (:time2)) " +
            "OR (endTime >= (:time1) AND endTime <= (:time2)) ORDER BY startTime")
    fun getActionsFromTimeInterval(time1: Long, time2: Long): LiveData<List<Action>>

    @Query("DELETE FROM action_table WHERE id in (:ids)")
    fun deleteActions(ids: List<String>)

    @Update
    fun updateAction(action: Action)

    @Insert
    fun addAction(action: Action)
}


@Database(entities = [Action::class], version=1, exportSchema = false)
abstract class ActionDatabase : RoomDatabase() {
    abstract fun dao(): ActionDao
}

class ActionRepository private constructor(context: Context) {
    private val mDatabase: ActionDatabase = Room.databaseBuilder(context.applicationContext,
        ActionDatabase::class.java,
        ACTION_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mHandlerThread = HandlerThread("ScheduleRepository")
    private var mHandler: Handler

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }

    fun getActionsFromTimeInterval(time1: Long, time2: Long): LiveData<List<Action>> =
        mDao.getActionsFromTimeInterval(time1, time2)

    fun updateAction(action: Action) {
        mHandler.post { mDao.updateAction(action) }
    }

    fun addAction(action: Action) {
        mHandler.post { mDao.addAction(action) }
    }

    fun deleteActions(ids: List<String>) {
        mHandler.post { mDao.deleteActions(ids) }
    }


    companion object {
        private var INSTANCE: ActionRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ActionRepository(context)
            }
        }

        fun get(): ActionRepository {
            return INSTANCE ?: throw IllegalStateException("ActionRepository must be initialized")
        }
    }
}

