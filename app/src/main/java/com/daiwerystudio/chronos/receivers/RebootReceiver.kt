/*
* Дата создания: 10.10.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daiwerystudio.chronos.database.ReminderRepository
import java.util.*
import java.util.concurrent.Executors

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val mReminderRepository = ReminderRepository.get()
            Executors.newSingleThreadExecutor().execute {
                val reminders = mReminderRepository.getRemindersMoreThanTime(System.currentTimeMillis())
                reminders.forEach { reminder ->
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val notificationIntent = Intent(context, NotificationReceiver::class.java).apply {
                        putExtra("reminder_text", reminder.text)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(context,
                        (UUID.fromString(reminder.id).mostSignificantBits and Long.MAX_VALUE).toInt(),
                        notificationIntent, 0
                    )
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.time, pendingIntent)
                }
            }
        }
    }
}