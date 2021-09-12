/*
* Дата создания: 20.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

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
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.TYPE_SCHEDULE_ONCE
import com.daiwerystudio.chronos.databinding.FragmentScheduleBinding
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
            tab.text = resources.getString(R.string.day)+" "+(position+1).toString()
        }.attach()

        viewModel.schedule.observe(viewLifecycleOwner, {
            binding.toolBar.title = it.name
            (binding.viewPager2.adapter as PagerAdapter).setSchedule(it)
            if (it.type == TYPE_SCHEDULE_ONCE) binding.tabLayout.visibility = View.GONE
        })

        binding.toolBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    val dialog = ScheduleDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("schedule", viewModel.schedule.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.Style_AlertDialog)
                        .setTitle(resources.getString(R.string.are_you_sure))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.deleteUnionWithChild(viewModel.schedule.value!!.id)
                            requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                            requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
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
        private var schedule: Schedule? = null

        fun setSchedule(schedule: Schedule){
            val lastCountDays = this.schedule?.countDays ?: 0
            this.schedule = schedule

            if (schedule.countDays > lastCountDays)
                notifyItemRangeInserted(lastCountDays, schedule.countDays-lastCountDays)
            if (schedule.countDays < lastCountDays)
                notifyItemRangeRemoved(schedule.countDays, lastCountDays-schedule.countDays)
        }

        override fun getItemCount(): Int = schedule?.countDays ?: 0

        override fun createFragment(position: Int): Fragment {
            val bundle = Bundle().apply {
                putString("scheduleID", schedule!!.id)
                putInt("dayIndex", position)
            }
            return DayScheduleFragment().apply { arguments = bundle }
        }
    }
}