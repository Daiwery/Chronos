/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors

/**
 * Константа для хранения имени базы данных.
 * */
private const val ACTION_TYPE_DATABASE_NAME = "action_type-database"

/**
 * Представялет из себя базовую ячейку в базе данных: одну строку.
 *
 * Интерфейс Serializable необходим для передачи объекта класса в пакете Bundle.
 * @property id уникальный идентификатор .
 * @property parent id родительского Action Type. Если родитель отсутствует, то значение равно "".
 * @property name имя. UI. Вводится и изменяется пользователем.
 * @property color цвет. UI. Вводится и изменяется пользователем.
 */
@Entity(tableName = "action_type_table")
data class ActionType(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var parent: String = "",
    var name: String = "",
    var color: Int = Color.rgb((Math.random()*255).toInt(), (Math.random()*255).toInt(), (Math.random()*255).toInt())
) : Serializable

/**
 * С помощью этого интерфейса выплоняются запросы к базе данных.
 *
 * Запросы написаны на SQLite.
 */
@Dao
interface ActionTypeDao {
    /**
     * Возвращает массив всех action type в обертке LiveData от какого-то родителя с заданным id
     * и сортирует по имени.
     */
    @Query("SELECT * FROM action_type_table WHERE parent=(:id) ORDER BY name ASC")
    fun getActionTypesFromParent(id: String): LiveData<List<ActionType>>

    /**
     * Возвращает одного action type в обертке LiveData с заданным id.
     */
    @Query("SELECT * FROM action_type_table WHERE id=(:id)")
    fun getActionType(id: String): LiveData<ActionType>

    /**
     * Возвращает цвет action type с заданным id. Используется для визуализации в
     * TimeView в отдельном потоке, поэтому не имеет обретки LiveData.
     */
    @Query("SELECT color FROM action_type_table WHERE id=(:id)")
    fun getColor(id: String): Int

    /**
     * Возвращает количество детей у заданного action type с заданным id в обертке LiveData.
     * Ипользуется только для UI.
     */
    @Query("SELECT COUNT(*) FROM action_type_table WHERE parent=(:id)")
    fun getCountChild(id: String): LiveData<Int>

    /**
     * Удаляет все дерево от action type с заданным id: его, его детей, детей его детей и т.д.
     */
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

    /**
     * Обновляет action type.
     */
    @Update
    fun updateActionType(actionType: ActionType)

    /**
     * Добавляет в базу данных новый action type.
     */
    @Insert
    fun addActionType(actionType: ActionType)
}

/**
 * Класс базы данных с указанием, что нужно хранить и какой номер версии.
 */
@Database(entities = [ActionType::class], version=1, exportSchema=false)
abstract class ActionTypeDatabase : RoomDatabase() {
    abstract fun dao(): ActionTypeDao
}

/**
 * Класс, с помощью которого можно обратится к базе данных из любой точки программы.
 * Является синглтоном и объявляется в DataBaseApplication.
 * @see DataBaseApplication
 */
class ActionTypeRepository private constructor(context: Context) {
    /**
     * Объект класса базы данных.
     */
    private val mDatabase: ActionTypeDatabase = Room.databaseBuilder(context.applicationContext,
        ActionTypeDatabase::class.java,
        ACTION_TYPE_DATABASE_NAME).build()

    /**
     * Интрейфс DAO для взаимодействия с базой данных.
     */
    private val mDao = mDatabase.dao()

    /**
     * Отдельный поток для обновления, добавления и удаления action type.
     */
    private val mExecutor = Executors.newSingleThreadExecutor()

    /**
     * Возвращает всех action type от какого-то родителя с заданным id.
     */
    fun getActionTypesFromParent(id: String): LiveData<List<ActionType>> = mDao.getActionTypesFromParent(id)

    /**
     * Возвращает одного action type с заданным id.
     */
    fun getActionType(id: String): LiveData<ActionType> = mDao.getActionType(id)

    /**
     * Возвращает цвет action type с заданным id. Используется для визуализации в
     * TimeView в отдельном потоке, поэтому не имеет обретки LiveData.
     */
    fun getColor(id: String): Int = mDao.getColor(id)

    /**
     * Возвращает количество детей у заданного action type с заданным id. Ипользуется только
     * для UI.
     */
    fun getCountChild(id: String): LiveData<Int> = mDao.getCountChild(id)

    /**
     * Обновляет action type в отдельном потоке.
     */
    fun updateActionType(actionType: ActionType) {
        mExecutor.execute {
            mDao.updateActionType(actionType)
        }
    }

    /**
     * Добавляет в базу данных новый action type в отдельном потоке.
     */
    fun addActionType(actionType: ActionType) {
        mExecutor.execute {
            mDao.addActionType(actionType)
        }
    }

    /**
     * Удаляет все дерево от action type с заданным id: его, его детей, детей его детей и т.д.
     * в отдельном потоке
     */
    fun deleteActionTypeWithChild(actionType: ActionType){
        mExecutor.execute {
            mDao.deleteActionTypeWithChild(actionType.id)
        }
    }

    /**
     * Создание экземпляра синглтона.
     */
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

