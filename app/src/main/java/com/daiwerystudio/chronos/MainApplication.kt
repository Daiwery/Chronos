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
import java.util.*

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        // База данных.
        ActionTypeRepository.initialize(this)
        GoalRepository.initialize(this)
        ScheduleRepository.initialize(this)
        NoteRepository.initialize(this)
        ReminderRepository.initialize(this) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this,
                (UUID.fromString(it).mostSignificantBits and Long.MAX_VALUE).toInt(),
                intent, 0)
            alarmManager.cancel(pendingIntent)
        }
        FolderRepository.initialize(this)
        UnionRepository.initialize(this)
        ActionRepository.initialize(this)

        // Канал уведомлений от напоминаний.
        val name = getString(R.string.name_reminder_channel)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_REMINDERS, name, importance)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_REMINDERS = "channel_reminders"
    }
}