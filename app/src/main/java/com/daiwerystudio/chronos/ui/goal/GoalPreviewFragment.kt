/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentGoalPreviewBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Класс фрагмента, который увидит пользователь после нажатия кнопки на нижней панели.
 * Содержит в себе только два элемента: TabLayout и ViewPager, так как используется как
 * контейнер для AchievedGoalFragment и NotAchievedGoalFragment.
 * @see AchievedGoalFragment
 * @see NotAchievedGoalFragment
 */
class GoalPreviewFragment: Fragment() {
    /**
     * Data Binding
     */
    private lateinit var binding: FragmentGoalPreviewBinding

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentGoalPreviewBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.viewPager2.adapter = PagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = resources.getString(R.string.not_achieved)
                1 -> tab.text = resources.getString(R.string.achieved)
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
                0 -> NotAchievedGoalFragment()
                1 -> AchievedGoalFragment()
                else -> throw IllegalAccessException("Invalid position")
            }
        }

    }
}