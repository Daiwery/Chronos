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


class PreviewScheduleFragment : Fragment() {
    // Data Binding
    private lateinit var binding: FragmentPreviewScheduleBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentPreviewScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting ViewPager2 and TabLayout
        binding.viewPager2.adapter = PagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = resources.getString(R.string.active)
                1 -> tab.text = resources.getString(R.string.not_active)
            }
        }.attach()

        return view
    }

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