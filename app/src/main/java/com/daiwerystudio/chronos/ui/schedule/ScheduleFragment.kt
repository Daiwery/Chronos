/*
* Дата создания: 20.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.DaySchedule
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.TYPE_SCHEDULE_RELATIVE
import com.daiwerystudio.chronos.databinding.FragmentScheduleBinding
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.google.android.material.tabs.TabLayoutMediator

class ScheduleFragment : Fragment() {
    private val viewModel: ScheduleViewModel
        by lazy { ViewModelProvider(this).get(ScheduleViewModel::class.java) }
    private lateinit var binding: FragmentScheduleBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.scheduleID.value = arguments?.getString("scheduleID")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.viewPager2.adapter = PagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            if (binding.viewPager2.adapter?.itemCount == 7) tab.text = resources.getStringArray(R.array.week)[position]
            else tab.text = resources.getString(R.string.day)+" "+position.toString()
        }.attach()


        viewModel.schedule.observe(viewLifecycleOwner, {
            binding.appBar.title = it.name
            (binding.viewPager2.adapter as PagerAdapter).setSchedule(it)
        })


        binding.appBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    val dialog = ScheduleDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("schedule", viewModel.schedule.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.App_AlertDialog)
                        .setTitle(resources.getString(R.string.are_you_sure))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                            viewModel.deleteUnionWithChild(viewModel.scheduleID.value!!)
                        }
                        .setNegativeButton(R.string.no){ _, _ -> }
                        .setCancelable(false)
                        .create()
                        .show()
                    true
                }
                else -> false
            }
        }

        return view
    }


    inner class PagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
        private var daysSchedule: List<DaySchedule> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun setSchedule(schedule: Schedule){
            if (schedule.countDays != daysSchedule.size){
                val days = viewModel.getDaysScheduleFromScheduleID(schedule.id)
                days.observe(viewLifecycleOwner, {
                    daysSchedule = it
                    // С учетом того, что schedule.countDays нельзя изменить.
                    notifyDataSetChanged()
                })
            }
        }

        override fun getItemCount(): Int = daysSchedule.size

        override fun createFragment(position: Int): Fragment {
            val bundle = Bundle().apply {
                putSerializable("daySchedule", daysSchedule[position])
            }
            return DayScheduleFragment().apply {
                arguments = bundle
            }
        }
    }
}