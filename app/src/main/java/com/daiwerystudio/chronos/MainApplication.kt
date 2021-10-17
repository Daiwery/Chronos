/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Изменения: 16.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменено: добавлен UnionRepository.
*
* Изменения: 16.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменено: добавление канала уведомлений и WorkManager
*/

package com.daiwerystudio.chronos

import android.app.*
import android.content.Context
import android.content.Intent
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.receivers.NotificationReceiver
import java.util.*

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        // База данных.
        ActionTypeRepository.initialize(this)
        GoalRepository.initialize(this)
        ScheduleRepository.initialize(this)
        NoteRepository.initialize(this)
        ReminderRepository.initialize(this,
            object : ReminderRepository.ChangeReminderListener {
                override fun deleteReminder(id: String) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(this@MainApplication, NotificationReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(this@MainApplication,
                        (UUID.fromString(id).mostSignificantBits and Long.MAX_VALUE).toInt(),
                        intent, 0)
                    alarmManager.cancel(pendingIntent)
                }

                override fun changeReminder(reminder: Reminder) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(this@MainApplication, NotificationReceiver::class.java).apply {
                        putExtra("reminder_text", reminder.text)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(this@MainApplication,
                        (UUID.fromString(reminder.id).mostSignificantBits and Long.MAX_VALUE).toInt(),
                        intent, 0)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.time, pendingIntent)
                }

            })
        FolderRepository.initialize(this)
        UnionRepository.initialize(this)
        ActionRepository.initialize(this)

        // Канал уведомлений от напоминаний.
        var name = getString(R.string.name_reminder_channel)
        var importance = NotificationManager.IMPORTANCE_DEFAULT
        var channel = NotificationChannel(CHANNEL_REMINDERS, name, importance)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Канал уведомления от трекинга действий.
        name = getString(R.string.name_tracking_channel)
        importance = NotificationManager.IMPORTANCE_LOW
        channel = NotificationChannel(CHANNEL_TRACKING, name, importance)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_REMINDERS = "channel_reminders"
        const val CHANNEL_TRACKING = "channel_tracking"
    }
}