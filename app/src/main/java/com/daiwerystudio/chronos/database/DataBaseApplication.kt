package com.daiwerystudio.chronos.database

import android.app.Application
import android.util.Log


private const val DATABASE_TAG = "DATABASE"
class DataBaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        ActionTypeRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create action type database")

        ActionRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create action database")

        GoalRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create goal database")

        TimetableRepository.initialize(this)
        Log.d(DATABASE_TAG, "Create timetable database")
    }
}