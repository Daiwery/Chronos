/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.database

import android.app.Application
import android.util.Log

/**
 * Константа с тегом для Log.
 */
private const val DATABASE_TAG = "DATABASE"

/**
 * Является подклассом Application. Объявлен в манифесте.
 * Создает репозитории всех баз данных.
 */
class DataBaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        ActionTypeRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create action type database")

        ActionRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create action database")

        GoalRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create goal database")

        ScheduleRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create timetable database")
    }
}