/*
* Дата создания: 05.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.daiwerystudio.chronos.database.ScheduleRepository
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var mNavController: NavController
//    private var mScheduleRepository = ScheduleRepository.get()
//    private var mCountCorrupted = mScheduleRepository.getCountActiveCorruptedSchedules()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Настройка компонентов навигации.
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        mNavController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_union_preview,
            R.id.navigation_day_container)
        )
        setupActionBarWithNavController(mNavController, appBarConfiguration)
        navView.setupWithNavController(mNavController)

        // Чтобы нижняя панель показывалась только в основных частях приложения.
        mNavController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_union_preview ->  navView.visibility = View.VISIBLE
                R.id.navigation_day_container ->  navView.visibility = View.VISIBLE
                else -> navView.visibility = View.GONE
            }
        }

//        val badge = navView.getOrCreateBadge(R.id.navigation_preview_schedule)
//        mCountCorrupted.observeForever {
//            if (it != null && it > 0) {
//                badge.isVisible = true
//                badge.number = it
//            } else badge.isVisible = false
//        }

    }

    override fun onSupportNavigateUp(): Boolean {
        return mNavController.navigateUp() || super.onSupportNavigateUp()
    }
}

