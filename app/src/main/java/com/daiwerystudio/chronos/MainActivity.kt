/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.daiwerystudio.chronos.MainApplication.Companion.CHANNEL_TRACKING
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.receivers.StopTrackerReceiver
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var mNavController: NavController
    private lateinit var preferences: SharedPreferences
    private var mActionTypeRepository = ActionTypeRepository.get()
    private lateinit var mNotificationBuilder: NotificationCompat.Builder
    private lateinit var mNotificationManager: NotificationManagerCompat
    private val mTimer: Handler = Handler(Looper.getMainLooper())
    private val runnableTimer = RunnableTimer()
    private var mHandler: Handler = Handler(Looper.getMainLooper())



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        mNavController = navHostFragment.navController
        navView.setupWithNavController(mNavController)

        mNavController.addOnDestinationChangedListener { _, destination, _ ->
            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            manager?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            currentFocus?.clearFocus()
            when (destination.id) {
                R.id.navigation_union_preview ->  navView.visibility = View.VISIBLE
                R.id.navigation_day ->  navView.visibility = View.VISIBLE
                R.id.navigation_time_tracker -> navView.visibility = View.VISIBLE
                else -> navView.visibility = View.GONE
            }
        }

        mNotificationManager = NotificationManagerCompat.from(this)
        preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        showActionTracking()
    }

    fun showActionTracking(){
        if (preferences.getString(PREFERENCES_TRACKING_ACTION_TYPE_ID , "") ?: "" != "") {
            Executors.newSingleThreadExecutor().execute {
                val actionType = mActionTypeRepository.getActionTypeInThread(
                    preferences.getString(PREFERENCES_TRACKING_ACTION_TYPE_ID , "") ?: "")
                if (actionType != null) {
                    mHandler.post {
                        val intent = Intent(this, StopTrackerReceiver::class.java)
                        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

                        mNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_TRACKING)
                        mNotificationBuilder.setSmallIcon(R.drawable.ic_baseline_timer_24)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentTitle(actionType.name)
                            .setOnlyAlertOnce(true)
                            .setShowWhen(false)
                            .setOngoing(true)
                            .addAction(R.drawable.ic_baseline_timer_24,
                                getString(R.string.stop_tracking_action), pendingIntent)
                        mNotificationManager.notify(0, mNotificationBuilder.build())
                        mTimer.post(runnableTimer)
                    }
                }
            }
        }
    }

    private inner class RunnableTimer: Runnable{
        override fun run() {
            if (preferences.getString(PREFERENCES_TRACKING_ACTION_TYPE_ID , "") ?: "" != "")
                if (System.currentTimeMillis() - preferences.getLong(PREFERENCES_TRACKING_START_TIME, 0) > 0) {
                    val time = System.currentTimeMillis() - preferences.getLong(PREFERENCES_TRACKING_START_TIME, 0)
                    val content = if (time < 24*60*60*1000)
                        LocalTime.ofSecondOfDay(time/1000)
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    else (time/(60*60*1000)).toString()+
                            LocalTime.ofSecondOfDay(time/1000%(24*60*60))
                                .format(DateTimeFormatter.ofPattern(":mm:ss"))

                    mNotificationBuilder.setContentText(content)
                    mNotificationManager.notify(0, mNotificationBuilder.build())

                    mTimer.postDelayed(this, 1000L)
                }
        }
    }


    companion object {
        const val PREFERENCES_TRACKING_ACTION_TYPE_ID = "preferencesTrackingActionTypeID"
        const val PREFERENCES_TRACKING_START_TIME = "preferencesTrackingStartTime"
    }
}

