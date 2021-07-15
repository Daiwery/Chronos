package com.daiwerystudio.chronos.DataBase

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


private const val ACTION_TYPE_DATABASE_NAME = "action_type-database"

@Entity(tableName = "action_type_table")
data class ActionType(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var parent: String = "",
    var name: String = UUID.randomUUID().toString(),
    var color: Int = Color.rgb((Math.random()*255).toInt(), (Math.random()*255).toInt(), (Math.random()*255).toInt())
) : Serializable

class ActionTypeTypeConverters {
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }
}

@Dao
interface ActionTypeDao {
    @Query("SELECT * FROM action_type_table WHERE parent=(:id)")
    fun getActionTypesFromParent(id: String): LiveData<List<ActionType>>

    // Это функция нужна для реккурентного удаления всех acts от какого-то parent (см. ниже)
    @Query("SELECT * FROM action_type_table WHERE parent=(:id)")
    fun getActionTypesFromParentAsList(id: String): List<ActionType>

    @Query("SELECT COUNT(*) FROM action_type_table")
    fun countRows(): LiveData<Int>

    @Query("SELECT * FROM action_type_table WHERE id=(:id)")
    fun getActionType(id: UUID): LiveData<ActionType>

    @Update
    fun updateActionType(actionType: ActionType)

    @Insert
    fun addActionType(actionType: ActionType)

    @Delete
    fun deleteActionType(actionType: ActionType)
}


@Database(entities = [ActionType::class], version=1, exportSchema = false)
@TypeConverters(ActionTypeTypeConverters::class)
abstract class ActionTypeDatabase : RoomDatabase() {
    abstract fun actionTypeDao(): ActionTypeDao
}


class ActionTypeRepository private constructor(context: Context) {
    private val database: ActionTypeDatabase = Room.databaseBuilder(
        context.applicationContext,
        ActionTypeDatabase::class.java,
        ACTION_TYPE_DATABASE_NAME).build()
    private val actionTypeDao = database.actionTypeDao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getActionTypesFromParent(id: String): LiveData<List<ActionType>> = actionTypeDao.getActionTypesFromParent(id)
    fun countRows(): LiveData<Int> = actionTypeDao.countRows()
    fun getActionType(id: UUID): LiveData<ActionType> = actionTypeDao.getActionType(id)


    fun updateActionType(actionType: ActionType) {
        executor.execute {
            actionTypeDao.updateActionType(actionType)
        }
    }

    fun addActionType(actionType: ActionType) {
        executor.execute {
            actionTypeDao.addActionType(actionType)
        }
    }


    fun deleteActionTypeWithChild(actionType: ActionType){
        executor.execute {
            val childActs = actionTypeDao.getActionTypesFromParentAsList(actionType.id.toString())
            actionTypeDao.deleteActionType(actionType)
            for (childAct in childActs){
                deleteActionTypeWithChild(childAct)
            }
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
            return INSTANCE ?: throw IllegalStateException("ActRepository must beinitialized")
        }
    }
}

