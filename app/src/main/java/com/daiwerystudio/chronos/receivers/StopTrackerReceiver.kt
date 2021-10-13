/*
* Дата создания: 10.10.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/


package com.daiwerystudio.chronos.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.daiwerystudio.chronos.MainActivity
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionRepository

class StopTrackerReceiver :  BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val mActionRepository = ActionRepository.get()

        if (preferences.getString(MainActivity.PREFERENCES_TRACKING_ACTION_TYPE_ID, "") != "")
            if (System.currentTimeMillis() - preferences.getLong(MainActivity.PREFERENCES_TRACKING_START_TIME, 0) > 0){
                val action = Action()
                action.actionTypeID = preferences.getString(MainActivity.PREFERENCES_TRACKING_ACTION_TYPE_ID, "")!!
                action.startTime = preferences.getLong(MainActivity.PREFERENCES_TRACKING_START_TIME, 0)
                action.endTime = System.currentTimeMillis()

                mActionRepository.addAction(action)
                val editor = preferences.edit()
                editor.putString(MainActivity.PREFERENCES_TRACKING_ACTION_TYPE_ID, "").apply()
                NotificationManagerCompat.from(context).cancel(0)
            }
    }
}