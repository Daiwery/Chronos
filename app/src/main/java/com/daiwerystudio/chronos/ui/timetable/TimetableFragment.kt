package com.daiwerystudio.chronos.ui.timetable

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentTimetableBinding
import com.google.android.material.tabs.TabLayoutMediator


class TimetableFragment : Fragment() {
    // Data Binding
    private lateinit var binding: FragmentTimetableBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentTimetableBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting ViewPager2 and TabLayout
        binding.viewPager2.adapter = PagerAdapter(activity as AppCompatActivity)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = resources.getString(R.string.active)
                1 -> tab.text = resources.getString(R.string.not_active)
            }
        }.attach()

        return view
    }

    class PagerAdapter(activity: AppCompatActivity): FragmentStateAdapter(activity){
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ActiveTimetableFragment()
                1 -> NotActiveTimetableFragment()
                else -> throw IllegalAccessException("Invalid position")
            }
        }

    }

}