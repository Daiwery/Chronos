/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentPreviewScheduleBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Класс фрагмента, который увидит пользователь после нажатия кнопки на нижней панели.
 * Содержит в себе только два элемента: TabLayout и ViewPager, так как используется как
 * контейнер для ActiveScheduleFragment и NotActiveScheduleFragment.
 * @see ActiveScheduleFragment
 * @see NotActiveScheduleFragment
 */
class PreviewScheduleFragment : Fragment() {
    /**
     * Data Binding
     */
    private lateinit var binding: FragmentPreviewScheduleBinding

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentPreviewScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.viewPager2.adapter = PagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = resources.getString(R.string.active)
                1 -> tab.text = resources.getString(R.string.not_active)
            }
        }.attach()

        return view
    }

    /**
     * Наследуется от FragmentStateAdapter. Является адаптером для pager. См. оф. документацию.
     */
    class PagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ActiveScheduleFragment()
                1 -> NotActiveScheduleFragment()
                else -> throw IllegalAccessException("Invalid position")
            }
        }

    }
}