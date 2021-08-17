/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.room.*
import com.daiwerystudio.chronos.ui.union.ID
import java.io.Serializable
import java.util.concurrent.Executors

private const val ACTION_TYPE_DATABASE_NAME = "action_type-database"

/**
 * @property id уникальный идентификатор.
 * @property name название типа действия.
 * @property color цвет действия.
 */
@Entity(tableName = "action_type_table")
data class ActionType(
    @PrimaryKey override val id: String,
    var name: String = "",
    var color: Int = Color.rgb((Math.random()*255).toInt(), (Math.random()*255).toInt(), (Math.random()*255).toInt())
) : Serializable, ID


@Dao
interface ActionTypeDao {
    @Query("SELECT * FROM action_type_table WHERE id IN (:ids)")
    fun getActionTypes(ids: List<String>): LiveData<List<ActionType>>

    @Query("SELECT * FROM action_type_table WHERE id=(:id)")
    fun getActionType(id: String): LiveData<ActionType>

    @Query("SELECT * FROM action_type_table")
    fun getAllActionType(): LiveData<List<ActionType>>

    @Query("DELETE FROM action_type_table WHERE id IN (:ids)")
    fun deleteActionTypes(ids: List<String>)

    @Query("SELECT color FROM action_type_table WHERE id=(:id)")
    fun getColor(id: String): Int

    @Update
    fun updateActionType(actionType: ActionType)

    @Insert
    fun addActionType(actionType: ActionType)
}


@Database(entities = [ActionType::class], version=1, exportSchema=false)
abstract class ActionTypeDatabase : RoomDatabase() {
    abstract fun dao(): ActionTypeDao
}

/**
 * Является синглтоном и инициализируется в DataBaseApplication.
 * @see DataBaseApplication
 */
class ActionTypeRepository private constructor(context: Context) {
    private val mDatabase: ActionTypeDatabase = Room.databaseBuilder(context.applicationContext,
        ActionTypeDatabase::class.java,
        ACTION_TYPE_DATABASE_NAME).build()
    private val mDao = mDatabase.dao()
    private val mExecutor = Executors.newSingleThreadExecutor()


    fun getActionTypes(ids: List<String>): LiveData<List<ActionType>> = mDao.getActionTypes(ids)

    fun getActionType(id: String): LiveData<ActionType> = mDao.getActionType(id)

    fun getAllActionType(): LiveData<List<ActionType>> = mDao.getAllActionType()

    fun getColor(id: String): Int = mDao.getColor(id)

    fun deleteActionTypes(ids: List<String>){
        mExecutor.execute{
            mDao.deleteActionTypes(ids)
        }
    }

    fun updateActionType(actionType: ActionType) {
        mExecutor.execute {
            mDao.updateActionType(actionType)
        }
    }

    fun addActionType(actionType: ActionType) {
        mExecutor.execute {
            mDao.addActionType(actionType)
        }
    }


    companion object {
        private var INSTANCE: ActionTypeRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ActionTypeRepository(context)
            }
        }

        fun get(): ActionTypeRepository {
            return INSTANCE ?: throw IllegalStateException("ActionTypeRepository must be initialized")
        }
    }
}

