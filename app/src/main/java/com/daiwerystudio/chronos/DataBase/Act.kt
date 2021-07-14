package com.daiwerystudio.chronos.DataBase

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*


@Entity(tableName = "act_table")
data class Act(
    @PrimaryKey(autoGenerate = true) val id: Int,
    var parent: Int? = null,
    var name: String = "",
    var color: Int = Color.rgb(0, 0, 0)
)


@Dao
interface ActDao {
    @Query("SELECT * FROM act_table WHERE parent=(:id)")
    fun getActsFromParent(id: Int?): LiveData<List<Act>>
}


@Database(entities = [Act::class], version=1, exportSchema = false)
abstract class ActDatabase : RoomDatabase() {
    abstract fun actDao(): ActDao
}


private const val DATABASE_NAME = "act-database"
class ActRepository private constructor(context: Context) {
    private val database : ActDatabase = Room.databaseBuilder(
        context.applicationContext,
        ActDatabase::class.java,
        DATABASE_NAME).build()
    private val actDao = database.actDao()

    fun getActsFromParent(id: Int?): LiveData<List<Act>> = actDao.getActsFromParent(id)

    companion object {
        private var INSTANCE: ActRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = ActRepository(context)
            }
        }

        fun get(): ActRepository {
            return INSTANCE ?: throw IllegalStateException("ActRepository must beinitialized")
        }
    }
}


class ActIntentApplication: Application()
{
    override fun onCreate() {
        super.onCreate()
        ActRepository.initialize(this)
    }
}
