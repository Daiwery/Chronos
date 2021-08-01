package com.daiwerystudio.chronos.database

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors


private const val ACTION_TYPE_DATABASE_NAME = "action_type-database"



@Entity(tableName = "action_type_table")
data class ActionType(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var parent: String = "",
    var name: String = "",
    var color: Int = Color.rgb((Math.random()*255).toInt(), (Math.random()*255).toInt(), (Math.random()*255).toInt())
) : Serializable



@Dao
interface ActionTypeDao {
    @Query("SELECT * FROM action_type_table WHERE parent=(:id) ORDER BY name ASC")
    fun getActionTypesFromParent(id: String): LiveData<List<ActionType>>

    @Query("SELECT * FROM action_type_table WHERE id=(:id)")
    fun getActionType(id: String): LiveData<ActionType>

    @Query("SELECT color FROM action_type_table WHERE id=(:id)")
    fun getColor(id: String): Int

    @Query("SELECT COUNT(*) FROM action_type_table WHERE parent=(:id)")
    fun getCountChild(id: String): LiveData<Int>

    @Query("SELECT COUNT(*) FROM action_type_table")
    fun getCount(): Int

    @Query("WITH RECURSIVE sub_table(id, parent) " +
            "AS (SELECT id, parent " +
            "FROM action_type_table " +
            "WHERE id=(:id) " +
            "UNION ALL " +
            "SELECT b.id, b.parent " +
            "FROM action_type_table AS b " +
            "JOIN sub_table AS c ON c.id=b.parent) " +
            "DELETE FROM action_type_table WHERE id IN (SELECT id FROM sub_table)")
    fun deleteActionTypeWithChild(id: String)

    @Update
    fun updateActionType(actionType: ActionType)

    @Insert
    fun addActionType(actionType: ActionType)

    @Delete
    fun deleteActionType(actionType: ActionType)
}



@Database(entities = [ActionType::class], version=1, exportSchema = false)
abstract class ActionTypeDatabase : RoomDatabase() {
    abstract fun dao(): ActionTypeDao
}



class ActionTypeRepository private constructor(context: Context) {
    private val database: ActionTypeDatabase =
        Room.databaseBuilder(context.applicationContext, ActionTypeDatabase::class.java, ACTION_TYPE_DATABASE_NAME).build()
    private val dao = database.dao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getActionTypesFromParent(id: String): LiveData<List<ActionType>> = dao.getActionTypesFromParent(id)

    fun getActionType(id: String): LiveData<ActionType> = dao.getActionType(id)

    fun getColor(id: String): Int = dao.getColor(id)

    fun getCountChild(id: String): LiveData<Int> = dao.getCountChild(id)

    fun updateActionType(actionType: ActionType) {
        executor.execute {
            dao.updateActionType(actionType)
        }
    }

    fun addActionType(actionType: ActionType) {
        executor.execute {
            dao.addActionType(actionType)
        }
    }


    fun deleteActionTypeWithChild(actionType: ActionType){
        executor.execute {
            dao.deleteActionTypeWithChild(actionType.id)
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

