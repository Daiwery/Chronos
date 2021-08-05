package com.daiwerystudio.chronos.ui.schedule

import android.annotation.SuppressLint
import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.databinding.FragmentScheduleBinding
import com.google.android.material.tabs.TabLayoutMediator


class ScheduleFragment : Fragment() {
    // ViewModel
    private val viewModel: ScheduleViewModel
    by lazy { ViewModelProvider(this).get(ScheduleViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentScheduleBinding
    // Arguments
    private lateinit var id: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Get arguments
        id = arguments?.getSerializable("id") as String
        viewModel.getSchedule(id)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentScheduleBinding.inflate(inflater, container, false)
        val view = binding.root


        // Setting ViewPager2 and TabLayout
        binding.viewPager2.adapter = PagerAdapter(this, null)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            if (binding.viewPager2.adapter!!.itemCount == 7) tab.text = resources.getStringArray(R.array.week)[position]
            else tab.text = resources.getString(R.string.day)+" "+(position+1).toString()
        }.attach()


        // Observe
        viewModel.schedule.observe(viewLifecycleOwner, { schedule ->
            // Binding
            binding.schedule = schedule
            // Title
            (activity as AppCompatActivity).supportActionBar?.title = schedule.name
            // ViewPager2
            (binding.viewPager2.adapter as PagerAdapter).setData(schedule)
            // TabLayout
            val index = (System.currentTimeMillis()/(1000*60*60*24)-schedule.dayStart).toInt()%schedule.countDays
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(index))
        })


        // Setting button
        binding.button.setOnClickListener {
            val schedule = viewModel.schedule.value!!
            val index = (System.currentTimeMillis()/(1000*60*60*24)-schedule.dayStart).toInt()%schedule.countDays
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(index))
        }


        // Setting switch
        binding.active.setOnClickListener {
            viewModel.schedule.value!!.isActive = binding.active.isChecked
            viewModel.updateSchedule(viewModel.schedule.value!!)
        }

        return view
    }

    // Set menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_delete_menu, menu)
    }

    // Click on element in menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                // Dialog
                val dialog = ScheduleDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("schedule", viewModel.schedule.value!!)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")

                return true
            }
            R.id.delete -> {
                // Dialog
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteScheduleWithActions(viewModel.schedule.value!!)
                        requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                    }
                    .setCancelable(false)
                    .create()
                    .show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private inner class PagerAdapter(fragment: Fragment, var schedule: Schedule?): FragmentStateAdapter(fragment){
        @SuppressLint("NotifyDataSetChanged")
        fun setData(newData: Schedule?) {
            schedule = newData
            notifyDataSetChanged()
            binding.loadingView.visibility = View.GONE
        }

        override fun getItemCount(): Int = schedule?.countDays ?: 0

        override fun createFragment(position: Int): Fragment {
            val fragment = DayScheduleFragment()
            fragment.arguments = Bundle().apply{
                putSerializable("schedule", schedule!!)
                putInt("dayIndex", position)
            }
            return fragment
        }

    }

}