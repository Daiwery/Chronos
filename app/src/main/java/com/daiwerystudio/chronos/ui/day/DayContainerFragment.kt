/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.day

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.databinding.FragmentDayContainerBinding
import java.util.*

/**
 * Класс фрагмента, который видит пользователь при нажатии на кнопку в нижней панели.
 * Является контейнером для фрагментов DayFragment. Ключевой особенностью является то,
 * в pager-е почти бесконечное количество элементов (2147483648). Начальная позиция
 * равна половине бесконечности, то есть можно двигаться как влево, так и вправо.
 */
class DayContainerFragment : Fragment() {
    /**
     * Data Binding
     */
    private lateinit var binding: FragmentDayContainerBinding

    /**
     * Переменная для связи с локальным файлом настроек. Нужен для доступа к времени пробуждения.
     */
    private lateinit var preferences: SharedPreferences

    /**
     * Выполняется перед созданием UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDayContainerBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.pager.adapter = PagerAdapter(this)
        binding.pager.setCurrentItem(startPosition, false)


        return view
    }

    /**
     * Наследуется от FragmentStateAdapter. Является адаптером для pager. См. оф. документацию.
     * Ключевой особенностью является то, что у него почти бесконечное количество элементов
     * (2147483648). При создании устанавливаем позицию на середину бесконечности, чтобы можно
     * было крутить влево или вправо, после чего по разности считаем день.
     */
    inner class PagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
        override fun getItemCount(): Int = Int.MAX_VALUE

        override fun createFragment(position: Int): Fragment {
            val fragment = DayFragment()

            val time = System.currentTimeMillis()
            val startDayTime = preferences.getLong("startDayTime", 6*60*60L)
            val day = ((time+TimeZone.getDefault().getOffset(time)+startDayTime)/(1000*60*60*24)).toInt()  // Локальный день.

            fragment.arguments = Bundle().apply {
                putInt("day", day + (position - startPosition))
            }
            return fragment
        }
    }


    companion object {
        /**
         * Начальная позиция. Середина бесконечности, чтобы можно было крутить влево или вправо.
         */
        private const val startPosition: Int = 1073741824
    }
}