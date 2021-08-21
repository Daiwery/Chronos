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

private const val NOTE_DATABASE_NAME = "note-database"

/**
 * @property id уникальный идентификатор
 * @property name имя заметки. UI.
 * @property note сама заметка. UI.
 */
@Entity(tableName = "note_table")
data class Note(
    @PrimaryKey override val id: String,
    var name: String = "",
    var note: String = ""
) : Serializable, ID


@Dao
interface NoteDao {
    @Query("SELECT * FROM note_table WHERE id IN (:ids)")
    fun getNotes(ids: List<String>): LiveData<List<Note>>

    @Query("SELECT * FROM note_table WHERE id=(:id)")
    fun getNote(id: String): LiveData<Note>

    @Query("DELETE FROM note_table WHERE id IN (:ids)")
    fun deleteNotes(ids: List<String>)

    @Update
    fun updateNote(note: Note)

    @Insert
    fun addNote(note: Note)
}


@Database(entities = [Note::class], version=1, exportSchema=false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun dao(): NoteDao
}


/**
 * Является синглтоном и инициализируется в DataBaseApplication.
 * @see DataBaseApplication
 */
class NoteRepository private constructor(context: Context) {
    private val mDatabase: NoteDatabase =
        Room.databaseBuilder(context.applicationContext,
        NoteDatabase::class.java,
        NOTE_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mHandlerThread = HandlerThread("NoteRepository")
    private var mHandler: Handler

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }


    fun getNotes(ids: List<String>): LiveData<List<Note>> = mDao.getNotes(ids)

    fun getNote(id: String): LiveData<Note> = mDao.getNote(id)

    fun deleteNotes(ids: List<String>) {
        mHandler.post { mDao.deleteNotes(ids) }
    }

    fun updateNote(note: Note){
        mHandler.post { mDao.updateNote(note) }
    }

    fun addNote(note: Note){
        mHandler.post { mDao.addNote(note) }
    }


    companion object {
        private var INSTANCE: NoteRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = NoteRepository(context)
            }
        }

        fun get(): NoteRepository {
            return INSTANCE ?: throw IllegalStateException("NoteRepository must be initialized")
        }
    }
}