package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.databinding.ListItemNotAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.FragmentNotAchievedGoalBinding


class NotAchievedGoalFragment: Fragment() {
    // ViewModel
    private val viewModel: NotAchievedGoalViewModel
    by lazy { ViewModelProvider(this).get(NotAchievedGoalViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentNotAchievedGoalBinding
    // Bundle
    val bundle = Bundle()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentNotAchievedGoalBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = NotAchievedGoalAdapter(emptyList())
        }

        // Setting fab
        binding.fab.setOnClickListener{ v: View ->
            v.findNavController().navigate(R.id.action_navigation_goal_to_navigation_item_goal)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observation
       viewModel.notAchievedGoals.observe(viewLifecycleOwner, Observer { notAchievedGoals ->
            binding.recyclerView.adapter = NotAchievedGoalAdapter(notAchievedGoals)
        })
    }

    private inner class NotAchievedGoalHolder(private val binding: ListItemNotAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var notAchievedGoal: Goal

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(notAchievedGoal: Goal) {
            this.notAchievedGoal = notAchievedGoal

            binding.goalName.text = notAchievedGoal.name
            val percent = viewModel.getPercentAchieved(this.notAchievedGoal.id.toString())
            percent.observe(viewLifecycleOwner, Observer {
                    percent -> if (percent is Int) binding.progressBar.setProgress(percent)
            })
        }

        override fun onClick(v: View) {
            bundle.putSerializable("parentGoal", notAchievedGoal)
            v.findNavController().navigate(R.id.action_navigation_goal_to_navigation_child_goal, bundle)
        }
    }

    private inner class NotAchievedGoalAdapter(var notAchievedGoals: List<Goal>): RecyclerView.Adapter<NotAchievedGoalHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotAchievedGoalHolder {
            val binding = DataBindingUtil.inflate<ListItemNotAchievedGoalBinding>(
                layoutInflater,
                R.layout.list_item_not_achieved_goal,
                parent,
                false)
            return NotAchievedGoalHolder(binding)
        }

        override fun getItemCount() = notAchievedGoals.size

        override fun onBindViewHolder(holder: NotAchievedGoalHolder, position: Int) {
            val notAchievedGoal = notAchievedGoals[position]
            holder.bind(notAchievedGoal)
        }
    }
}