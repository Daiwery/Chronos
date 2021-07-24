package com.daiwerystudio.chronos.ui.timetable

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Timetable
import com.daiwerystudio.chronos.databinding.FragmentActiveTimetableBinding
import com.daiwerystudio.chronos.databinding.ListItemActiveTimetableBinding


class ActiveTimetableFragment : Fragment() {
    // ViewModel
    private val viewModel: ActiveTimetableViewModel
    by lazy { ViewModelProvider(this).get(ActiveTimetableViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentActiveTimetableBinding
    // Bundle
    var bundle = Bundle()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentActiveTimetableBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ActiveTimetableAdapter(emptyList())
        }

        // Setting fab
        binding.fab.setOnClickListener{ v: View ->
            TimetableDialog(Timetable(), true)
                .show(activity?.supportFragmentManager!!, "TimetableDialogBottomSheet")
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observation
        viewModel.activeTimetables.observe(viewLifecycleOwner, Observer { activeTimetables ->
            binding.recyclerView.adapter = ActiveTimetableAdapter(activeTimetables)
        })
    }

    private inner class ActiveTimetableHolder(private val binding: ListItemActiveTimetableBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var activeTimetable: Timetable

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(activeTimetable: Timetable) {
            this.activeTimetable = activeTimetable

            binding.timetable = this.activeTimetable
        }

        override fun onClick(v: View) {
            bundle.putSerializable("timetable", activeTimetable)
            v.findNavController().navigate(R.id.action_navigation_timetable_to_navigation_child_timetable, bundle)
        }
    }

    private inner class ActiveTimetableAdapter(var activeTimetables: List<Timetable>):
        RecyclerView.Adapter<ActiveTimetableHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveTimetableHolder {
            val binding = DataBindingUtil.inflate<ListItemActiveTimetableBinding>(
                layoutInflater,
                R.layout.list_item_active_timetable,
                parent,
                false)
            return ActiveTimetableHolder(binding)
        }

        override fun getItemCount() = activeTimetables.size

        override fun onBindViewHolder(holder: ActiveTimetableHolder, position: Int) {
            val activeTimetable = activeTimetables[position]
            holder.bind(activeTimetable)
        }
    }


}