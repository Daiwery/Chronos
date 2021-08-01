package com.daiwerystudio.chronos.ui.timetable

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Timetable
import com.daiwerystudio.chronos.databinding.FragmentChildTimetableBinding
import com.google.android.material.tabs.TabLayoutMediator

class ChildTimetableFragment : Fragment() {
    // ViewModel
    private val viewModel: ChildTimetableViewModel
    by lazy { ViewModelProvider(this).get(ChildTimetableViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentChildTimetableBinding
    // Bundle
    private var bundle = Bundle()
    private lateinit var timetable: Timetable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        timetable = arguments?.getSerializable("timetable") as Timetable
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentChildTimetableBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting checkBox
        binding.checkBox.isChecked = timetable.isActive
        binding.checkBox.setOnClickListener {
            timetable.isActive = binding.checkBox.isChecked
            viewModel.updateTimetable(timetable)
        }

        updateUI()

        return view
    }

    private fun updateUI(){
        // Setting ViewPager2 and TabLayout
        binding.viewPager2.adapter = PagerAdapter(this, timetable)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.text = resources.getString(R.string.day)+" "+(position+1).toString()
        }.attach()

        // Menu
        val appCompatActivity = activity as AppCompatActivity
        val appBar = appCompatActivity.supportActionBar
        appBar?.title = timetable.name
    }


    override fun onStart() {
        super.onStart()
        bundle.clear()
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
                TimetableDialog(timetable, false)
                    .show(activity?.supportFragmentManager!!, "TimetableDialog")
                val liveDataTimetable = viewModel.getTimetable(timetable.id)
                liveDataTimetable.observe(viewLifecycleOwner, Observer { _timetable ->
                        timetable = _timetable
                        updateUI()
                })

                return true
            }
            R.id.delete -> {
                viewModel.deleteTimetableWithActions(timetable)
                requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    class PagerAdapter(fragment: Fragment, var timetable: Timetable): FragmentStateAdapter(fragment){
        override fun getItemCount(): Int = timetable.countDays

        override fun createFragment(position: Int): Fragment {
            return ActionsTimetableFragment(timetable, position)
        }

    }

}