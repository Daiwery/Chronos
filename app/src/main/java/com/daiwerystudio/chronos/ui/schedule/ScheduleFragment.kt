/*
* Дата создания: 20.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentPeriodicScheduleBinding
import com.google.android.material.tabs.TabLayoutMediator

class ScheduleFragment : Fragment() {
    private val viewModel: ScheduleViewModel
        by lazy { ViewModelProvider(this).get(ScheduleViewModel::class.java) }
    private lateinit var binding: FragmentPeriodicScheduleBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.scheduleID.value = arguments?.getString("scheduleID")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentPeriodicScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.viewPager2.adapter = PagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            if (binding.viewPager2.adapter?.itemCount == 7) tab.text = resources.getStringArray(R.array.week)[position]
            else tab.text = resources.getString(R.string.day)+" "+(position+1).toString()
        }.attach()


        viewModel.schedule.observe(viewLifecycleOwner, {
            binding.toolBar.title = it.name
        })
        viewModel.daysScheduleIDs.observe(viewLifecycleOwner, {
            (binding.viewPager2.adapter as PagerAdapter).setDaysScheduleIDs(it)
            // if (it.size == 1) binding.tabLayout.visibility = View.GONE
        })


        binding.toolBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }

        return view
    }


    inner class PagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
        private var daysScheduleIDs: List<String> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun setDaysScheduleIDs(daysScheduleIDs: List<String>){
            this.daysScheduleIDs = daysScheduleIDs
            // С учетом того, что schedule.countDays нельзя изменить.
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = daysScheduleIDs.size

        override fun createFragment(position: Int): Fragment {
            val bundle = Bundle().apply {
                putSerializable("dayScheduleID", daysScheduleIDs[position])
            }
            return DayScheduleFragment().apply {
                arguments = bundle
            }
        }
    }
}