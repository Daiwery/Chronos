package com.daiwerystudio.chronos.database

import android.app.Application
import android.util.Log


private val ACTION_TYPE_DATABASE_TAG = "action_type_database"
private val ACTION_DATABASE_TAG = "action_database"
private val GOAL_DATABASE_TAG = "action_database"
class DataBaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        ActionTypeRepository.initialize(this)
        Log.d(ACTION_TYPE_DATABASE_TAG, "Create action_type database")

        ActionRepository.initialize(this)
        Log.d(ACTION_DATABASE_TAG, "Create action database")

        GoalRepository.initialize(this)
        Log.d(GOAL_DATABASE_TAG, "Create goal database")
    }
}