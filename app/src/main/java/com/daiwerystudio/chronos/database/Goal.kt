/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors

/**
 * Константа для хранения имени базы данных.
 * */
private const val GOAL_DATABASE_NAME = "goal-database"

/**
 * Представляет из себя одну строку в базе данных.
 *
 * Интерфейс Serializable необходим для передачи объекта класса в пакете Bundle.
 * @property id уникальный идентификатор.
 * @property parent id родительской цели. Если родители отсутствуют, то значение равно "".
 * @property name имя. UI.
 * @property note заметка. UI.
 * @property isAchieved выполнена ли данная цель.
 * @property indexList индекс цели в массиве. По нему сортируются цели, чтобы
 * сохранить порядок, требуемый пользователем.
 */
@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var parent: String = "",
    var name: String = "",
    var note: String = "",
    var isAchieved: Boolean = false,
    var indexList: Int = -1
) : Serializable

/**
 * С помощью этого интерфейса выплоняются запросы к базе данных.
 *
 * Запросы написаны на SQLite.
 */
@Dao
interface GoalTypeDao {
    /**
     * Возвращает все цели без родителей в обертке LiveData в зависимости от isAchieved
     * и сортирует по имени.
     */
    @Query("SELECT * FROM goal_table WHERE parent='' AND isAchieved=(:isAchieved) ORDER BY name ASC")
    fun getGoalsWithoutParentFromSolve(isAchieved: Boolean): LiveData<List<Goal>>

    /**
     * Возвращает массив всех целей от какого-то родителя с заданным id в обертке LiveData
     * и сортирует по indexList.
     */
    @Query("SELECT * FROM goal_table WHERE parent=(:id) ORDER BY indexList ASC")
    fun getGoalsFromParent(id: String): LiveData<List<Goal>>

    /**
     * Удаление всего дерева от цели с заданным id.
     */
    @Query("WITH RECURSIVE sub_table(id, parent) " +
            "AS (SELECT id, parent " +
            "FROM goal_table " +
            "WHERE id=(:id) " +
            "UNION ALL " +
            "SELECT b.id, b.parent " +
            "FROM goal_table AS b " +
            "JOIN sub_table AS c ON c.id=b.parent) " +
            "DELETE FROM goal_table WHERE id IN (SELECT id FROM sub_table)")
    fun deleteGoalWithChild(id: String)

    /**
     * Устанавливает isAchieved значение true всем целям в дереве от цели с заданным id.
     */
    @Query("WITH RECURSIVE sub_table(id, parent) " +
            "AS (SELECT id, parent " +
            "FROM goal_table " +
            "WHERE id=(:id) " +
            "UNION ALL " +
            "SELECT b.id, b.parent " +
            "FROM goal_table AS b " +
            "JOIN sub_table AS c ON c.id=b.parent) " +
            "UPDATE goal_table SET isAchieved=1 WHERE id IN (SELECT id FROM sub_table)")
    fun setAchievedGoalWithChild(id: String)

    /**
     * Возвращает процент выполненных целей среди дочерних целей от цели с заданным id.
     * Используется только для UI. Возвращается в обертке LiveData.
     */
    @Query("SELECT 100*AVG(isAchieved=1) FROM goal_table WHERE parent=(:id)")
    fun getPercentAchieved(id: String): LiveData<Int>

    /**
     * Возвращаем цель с заданным id в обертке LiveData.
     */
    @Query("SELECT * FROM goal_table WHERE id=(:id)")
    fun getGoal(id: String): LiveData<Goal>

    /**
     * Обновляет заданную цель.
     */
    @Update
    fun updateGoal(goal: Goal)

    /**
     * Обновляет все цели в заданном массиве. Используется для установки indexList.
     */
    @Update
    fun updateListGoals(listGoal: List<Goal>)

    /**
     * Добавляет заданную цель в базу данных.
     */
    @Insert
    fun addGoal(goal: Goal)
}


/**
 * Класс базы данных с указанием, что нужно хранить и какой номер версии.
 */
@Database(entities = [Goal::class], version=1, exportSchema = false)
abstract class GoalDatabase : RoomDatabase() {
    abstract fun dao(): GoalTypeDao
}

/**
 * Класс, с помощью которого можно обратится к базе данных из любой точки программы.
 * Является синглтоном и объявляется в DataBaseApplication.
 * @see DataBaseApplication
 */
class GoalRepository private constructor(context: Context) {
    /**
     * Объект класса базы данных.
     */
    private val mDatabase: GoalDatabase = Room.databaseBuilder(context.applicationContext,
        GoalDatabase::class.java,
        GOAL_DATABASE_NAME).build()

    /**
     * Интрейфс DAO для взаимодействия с базой данных.
     */
    private val mDao = mDatabase.dao()

    /**
     * Отдельный поток для обновления, добавления и удаления action type.
     */
    private val mExecutor = Executors.newSingleThreadExecutor()

    /**
     * Возвращает все цели без родителей в обертке LiveData в зависимости от isAchieved
     * и сортирует по имени.
     */
    fun getGoalsWithoutParentFromSolve(isAchieved: Boolean): LiveData<List<Goal>> =
        mDao.getGoalsWithoutParentFromSolve(isAchieved)

    /**
     * Возвращает массив всех целей от какого-то родителя с заданным id в обертке LiveData
     * и сортирует по indexList.
     */
    fun getGoalsFromParent(id: String): LiveData<List<Goal>> = mDao.getGoalsFromParent(id)

    /**
     * Возвращает процент выполненных целей среди дочерних целей от цели с заданным id.
     * Используется только для UI. Возвращается в обертке LiveData.
     */
    fun getPercentAchieved(id: String): LiveData<Int> = mDao.getPercentAchieved(id)

    /**
     * Возвращаем цель с заданным id в обертке LiveData.
     */
    fun getGoal(id: String): LiveData<Goal> = mDao.getGoal(id)

    /**
     * Обновляем цель в отдельном потоке.
     */
    fun updateGoal(goal: Goal) {
        mExecutor.execute {
            mDao.updateGoal(goal)
        }
    }

    /**
     * Обновляет все цели в заданном массиве в отдельном потоке.
     * Используется для установки indexList.
     */
    fun updateListGoals(listGoal: List<Goal>){
        mExecutor.execute {
            mDao.updateListGoals(listGoal)
        }
    }

    /**
     * Добавляет заданную цель в базу данных в отдельном потоке.
     */
    fun addGoal(goal: Goal) {
        mExecutor.execute {
            mDao.addGoal(goal)
        }
    }

    /**
     * Удаляет все дерево от цели в отельном потоке.
     */
    fun deleteGoalWithChild(goal: Goal){
        mExecutor.execute {
            mDao.deleteGoalWithChild(goal.id)
        }
    }

    /**
     * Устанавливает isAchieved значение true всем целям в дереве от цели с заданным id
     * в отдельном потоке.
     */
    fun setAchievedGoalWithChild(goal: Goal){
        mExecutor.execute {
            mDao.setAchievedGoalWithChild(goal.id)
        }
    }

    /**
     * Создаение экземпляр синглтона.
     */
    companion object {
        private var INSTANCE: GoalRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = GoalRepository(context)
            }
        }

        fun get(): GoalRepository {
            return INSTANCE ?: throw IllegalStateException("GoalRepository must be initialized")
        }
    }
}

