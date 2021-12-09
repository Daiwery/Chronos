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
import android.view.ViewTreeObserver
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
    private var mInterstitialAd: InterstitialAd? = null
    private var mLoadingAd: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        mNavController = navHostFragment.navController
        navView.setupWithNavController(mNavController)

        mNotificationManager = NotificationManagerCompat.from(this)
        preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        showActionTracking()

        mNavController.addOnDestinationChangedListener { _, destination, _ ->
            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            manager?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            currentFocus?.clearFocus()

            val lastAdTime = preferences.getLong(PREFERENCES_LAST_AD_TIME, 0)
            if ((System.currentTimeMillis() - lastAdTime >= 1000L*60*60 ||
                System.currentTimeMillis() < lastAdTime) && !mLoadingAd) {
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this)
                    mInterstitialAd = null

                    val editor = preferences.edit()
                    editor.putLong(PREFERENCES_LAST_AD_TIME, System.currentTimeMillis()).apply()
                } else loadAd()
            }

            when (destination.id) {
                R.id.navigation_union_preview ->  navView.visibility = View.VISIBLE
                R.id.navigation_day ->  navView.visibility = View.VISIBLE
                R.id.navigation_time_tracker -> navView.visibility = View.VISIBLE
                else -> navView.visibility = View.GONE
            }
        }

        MobileAds.initialize(this) {}

        val lastAdTime = preferences.getLong(PREFERENCES_LAST_AD_TIME, 0)
        if (System.currentTimeMillis() - lastAdTime >= 1000L*60*60 ||
            System.currentTimeMillis() < lastAdTime) {
            loadAd()

            val content: View = findViewById(android.R.id.content)
            content.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        return if (!mLoadingAd) {
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                            true
                        } else false
                    }
                }
            )
        }

//        splashScreen.setOnExitAnimationListener { splashScreenView ->
//            // Create your custom animation.
//            val slideUp = ObjectAnimator.ofFloat(
//                splashScreenView,
//                View.TRANSLATION_Y,
//                0f,
//                -splashScreenView.height.toFloat()
//            )
//            slideUp.interpolator = AnticipateInterpolator()
//            slideUp.duration = 200L
//
//            // Call SplashScreenView.remove at the end of your custom animation.
//            slideUp.doOnEnd { splashScreenView.remove() }
//
//            // Run your animation.
//            slideUp.start()
//        }
    }

    private fun loadAd(){
        val adRequest = AdRequest.Builder().build()
        mLoadingAd = true
        // test Ad - ca-app-pub-3940256099942544/1033173712
        // real Ad - ca-app-pub-2027715765267500/4322925878
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712",
            adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    mLoadingAd = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mLoadingAd = false
                }
            })
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
        const val PREFERENCES_LAST_AD_TIME = "preferencesLastAdTime"
    }
}

