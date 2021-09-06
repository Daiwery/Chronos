/*
* Дата создания: 06.09.2021
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

private const val FOLDER_DATABASE_NAME = "folder-database"

/**
 * @property id уникальный идентификатор
 * @property name имя папки. UI.
 */
@Entity(tableName = "folder_table")
data class Folder(
    @PrimaryKey override val id: String,
    var name: String = "",
) : Serializable, ID


@Dao
interface FolderDao {
    @Query("SELECT * FROM folder_table WHERE id IN (:ids) ORDER BY name")
    fun getFolders(ids: List<String>): LiveData<List<Folder>>

    @Query("SELECT * FROM folder_table WHERE id=(:id)")
    fun getFolder(id: String): LiveData<Folder>

    @Query("DELETE FROM folder_table WHERE id IN (:ids)")
    fun deleteFolders(ids: List<String>)

    @Update
    fun updateFolder(folder: Folder)

    @Insert
    fun addFolder(folder: Folder)
}


@Database(entities = [Folder::class], version=1, exportSchema=false)
abstract class FolderDatabase : RoomDatabase() {
    abstract fun dao(): FolderDao
}


/**
 * Является синглтоном и инициализируется в DataBaseApplication.
 * @see DataBaseApplication
 */
class FolderRepository private constructor(context: Context) {
    private val mDatabase: FolderDatabase =
        Room.databaseBuilder(context.applicationContext,
            FolderDatabase::class.java,
            FOLDER_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mHandlerThread = HandlerThread("FolderRepository")
    private var mHandler: Handler

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }


    fun getFolders(ids: List<String>): LiveData<List<Folder>> = mDao.getFolders(ids)

    fun getFolder(id: String): LiveData<Folder> = mDao.getFolder(id)

    fun deleteFolders(ids: List<String>) {
        mHandler.post { mDao.deleteFolders(ids) }
    }

    fun updateFolder(folder: Folder){
        mHandler.post { mDao.updateFolder(folder) }
    }

    fun addFolder(folder: Folder){
        mHandler.post { mDao.addFolder(folder) }
    }


    companion object {
        private var INSTANCE: FolderRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = FolderRepository(context)
            }
        }

        fun get(): FolderRepository {
            return INSTANCE ?: throw IllegalStateException("FolderRepository must be initialized")
        }
    }
}