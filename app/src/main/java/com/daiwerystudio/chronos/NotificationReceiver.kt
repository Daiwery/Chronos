/*
* Дата создания: 03.10.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifyIntent = Intent(context, MainActivity::class.java)
        notifyIntent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, 0)

        val builder = NotificationCompat.Builder(context, MainApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
            .setContentTitle(context.getString(R.string.reminder))
            .setContentText(intent.getStringExtra("reminder_text"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify((System.currentTimeMillis()%Int.MAX_VALUE).toInt(), builder.build())
        }
    }
}

