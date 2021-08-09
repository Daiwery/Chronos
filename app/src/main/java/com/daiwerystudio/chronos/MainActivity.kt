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

/**
 * Данный класс представляет из себя начало всего и вся. Здесь происходит настройка основных
 * компонентов приложения: NavHost, NavController, BottomNavigationView. Строится и показывается
 * первое, что увидит пользователь: activity_main.xml
 */
class MainActivity : AppCompatActivity() {
    /**
     * Переменная, которая управляет навигацией в приложении между фрагментами.
     */
    private lateinit var mNavController: NavController

    /**
     * Связь с базой данных. Нужен для получения количества испорченных расписаний, чтобы
     * установить бейдж на меню.
     */
    private var mScheduleRepository = ScheduleRepository.get()

    /**
     * Количество активных и испорченных расписаний в обертке LiveData. С помощью подписки
     * на него устанавливаем бейдж.
     */
    private var mCountCorrupted = mScheduleRepository.getCountActiveCorruptedSchedules()

    /**
     * Переопределение функции суперкласса. Запускается в начале жизненного цикла.
     * Здесь настраиваться все необходимое для навигации в приложении.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Настройка компонентов навигации.
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        mNavController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_action_type,
            R.id.navigation_day_container,
            R.id.navigation_preview_goal,
            R.id.navigation_preview_schedule)
        )
        setupActionBarWithNavController(mNavController, appBarConfiguration)
        navView.setupWithNavController(mNavController)

        // Чтобы нижняя панель показывалась только в основных частях приложения.
        mNavController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_action_type ->  navView.visibility = View.VISIBLE
                R.id.navigation_day_container ->  navView.visibility = View.VISIBLE
                R.id.navigation_preview_goal ->  navView.visibility = View.VISIBLE
                R.id.navigation_preview_schedule -> navView.visibility = View.VISIBLE
                else -> navView.visibility = View.GONE
            }
        }

        val badge = navView.getOrCreateBadge(R.id.navigation_preview_schedule)
        mCountCorrupted.observeForever {
            if (it != null && it > 0) {
                badge.isVisible = true
                badge.number = it
            } else badge.isVisible = false
        }

    }

    /**
     * Функция нужна для включения поддержки кнопки push up (стрелка обратно).
     */
    override fun onSupportNavigateUp(): Boolean {
        return mNavController.navigateUp() || super.onSupportNavigateUp()
    }
}

