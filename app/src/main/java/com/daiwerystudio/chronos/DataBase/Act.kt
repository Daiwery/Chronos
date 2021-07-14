package com.daiwerystudio.chronos.DataBase

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*
import java.util.concurrent.Executors


private val ACT_DATABASE_TAG = "act_database"
private const val DATABASE_NAME = "act-database"

@Entity(tableName = "act_table")
data class Act(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var parent: String = "",
    var name: String = UUID.randomUUID().toString(),
    var color: Int = Color.rgb((Math.random()*255).toInt(), (Math.random()*255).toInt(), (Math.random()*255).toInt())
)

class ActTypeConverters {
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
interface ActDao {
    @Query("SELECT * FROM act_table WHERE parent=(:id)")
    fun getActsFromParent(id: String): LiveData<List<Act>>

    @Update
    fun updateAct(act: Act)

    @Insert
    fun addAct(act: Act)

    @Delete
    fun deleteAct(act: Act)
}


@Database(entities = [Act::class], version=1, exportSchema = false)
@TypeConverters(ActTypeConverters::class)
abstract class ActDatabase : RoomDatabase() {
    abstract fun actDao(): ActDao
}


class ActRepository private constructor(context: Context) {
    private val database : ActDatabase = Room.databaseBuilder(
        context.applicationContext,
        ActDatabase::class.java,
        DATABASE_NAME).build()
    private val actDao = database.actDao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getActsFromParent(id: String): LiveData<List<Act>> = actDao.getActsFromParent(id)

    fun updateAct(act: Act) {
        executor.execute {
            actDao.updateAct(act)
        }
    }
    fun addAct(act: Act) {
        executor.execute {
            actDao.addAct(act)
        }
    }
    fun deleteAct(act: Act) {
        executor.execute {
            actDao.deleteAct(act)
        }
    }


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

        Log.d(ACT_DATABASE_TAG, "Create act database")
    }
}
