/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.room.*
import com.daiwerystudio.chronos.ui.union.ID
import java.io.Serializable

private const val REMINDER_DATABASE_NAME = "reminder-database"

/**
 * @property id уникальный идентификатор
 * @property text текст напоминания. UI.
 * @property time время напоминания. UI.
 */
@Entity(tableName = "reminder_table")
data class Reminder(
    @PrimaryKey override val id: String,
    var text: String = "",
    var time: Long = System.currentTimeMillis()
) : Serializable, ID


@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_table WHERE id IN (:ids)")
    fun getReminders(ids: List<String>): LiveData<List<Reminder>>

    @Query("DELETE FROM reminder_table WHERE id IN (:ids)")
    fun deleteReminders(ids: List<String>)

    @Update
    fun updateReminder(reminder: Reminder)

    @Insert
    fun addReminder(reminder: Reminder)
}


@Database(entities = [Reminder::class], version=1, exportSchema=false)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun dao(): ReminderDao
}


/**
 * Является синглтоном и инициализируется в DataBaseApplication.
 * @see DataBaseApplication
 */
class ReminderRepository private constructor(context: Context) {
    private val mDatabase: ReminderDatabase =
        Room.databaseBuilder(context.applicationContext,
            ReminderDatabase::class.java,
            REMINDER_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mHandlerThread = HandlerThread("ReminderRepository")
    private var mHandler: Handler

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }


    fun getReminders(ids: List<String>): LiveData<List<Reminder>> = mDao.getReminders(ids)

    fun deleteReminders(ids: List<String>) {
        mHandler.post { mDao.deleteReminders(ids) }
    }

    fun updateReminder(reminder: Reminder){
        mHandler.post { mDao.updateReminder(reminder) }
    }

    fun addReminder(reminder: Reminder){
        mHandler.post { mDao.addReminder(reminder) }
    }


    companion object {
        private var INSTANCE: ReminderRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ReminderRepository(context)
            }
        }

        fun get(): ReminderRepository {
            return INSTANCE ?: throw IllegalStateException("NoteRepository must be initialized")
        }
    }
}
