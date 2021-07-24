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
import com.daiwerystudio.chronos.databinding.FragmentNotActiveTimetableBinding
import com.daiwerystudio.chronos.databinding.ListItemNotActiveTimetableBinding


class NotActiveTimetableFragment : Fragment() {
    // ViewModel
    private val viewModel: NotActiveTimetableViewModel
    by lazy { ViewModelProvider(this).get(NotActiveTimetableViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentNotActiveTimetableBinding
    // Bundle
    var bundle = Bundle()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentNotActiveTimetableBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = NotActiveTimetableAdapter(emptyList())
        }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observation
        viewModel.notActiveTimetables.observe(viewLifecycleOwner, Observer { notActiveTimetables ->
            binding.recyclerView.adapter = NotActiveTimetableAdapter(notActiveTimetables)
        })
    }

    private inner class NotActiveTimetableHolder(private val binding: ListItemNotActiveTimetableBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var notActiveTimetable: Timetable

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(notActiveTimetable: Timetable) {
            this.notActiveTimetable = notActiveTimetable

            binding.timetable = this.notActiveTimetable
        }

        override fun onClick(v: View) {
            bundle.putSerializable("timetable", notActiveTimetable)
            v.findNavController().navigate(R.id.action_navigation_timetable_to_navigation_child_timetable, bundle)
        }
    }

    private inner class NotActiveTimetableAdapter(var notActiveTimetables: List<Timetable>):
        RecyclerView.Adapter<NotActiveTimetableHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotActiveTimetableHolder {
            val binding = DataBindingUtil.inflate<ListItemNotActiveTimetableBinding>(
                layoutInflater,
                R.layout.list_item_not_active_timetable,
                parent,
                false)
            return NotActiveTimetableHolder(binding)
        }

        override fun getItemCount() = notActiveTimetables.size

        override fun onBindViewHolder(holder: NotActiveTimetableHolder, position: Int) {
            val notActiveTimetable = notActiveTimetables[position]
            holder.bind(notActiveTimetable)
        }
    }


}