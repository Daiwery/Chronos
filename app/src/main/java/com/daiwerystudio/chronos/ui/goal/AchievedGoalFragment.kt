package com.daiwerystudio.chronos.ui.goal

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.databinding.FragmentAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.ListItemAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.ListItemNotAchievedGoalBinding

class AchievedGoalFragment : Fragment() {
    // ViewModel
    private val viewModel: AchievedGoalViewModel
    by lazy { ViewModelProvider(this).get(AchievedGoalViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentAchievedGoalBinding
    // Bundle
    private var bundle = Bundle()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentAchievedGoalBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AchievedGoalAdapter(emptyList())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observation of actionTypes
        viewModel.achievedGoals.observe(viewLifecycleOwner, Observer { achievedGoals ->
            binding.recyclerView.adapter = AchievedGoalAdapter(achievedGoals)
        })
    }

    private inner class AchievedGoalHolder(private val binding: ListItemAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var achievedGoal: Goal

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(achievedGoal: Goal) {
            this.achievedGoal = achievedGoal

            binding.goalName.text = achievedGoal.name
        }

        override fun onClick(v: View) {
//            bundle.putSerializable("parentActionType", actionType)
//            v.findNavController().navigate(R.id.action_navigation_action_type_to_navigation_child_action_type, bundle)
        }
    }

    private inner class AchievedGoalAdapter(var achievedGoals: List<Goal>): RecyclerView.Adapter<AchievedGoalHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievedGoalHolder {
            val binding = DataBindingUtil.inflate<ListItemAchievedGoalBinding>(
                layoutInflater,
                R.layout.list_item_action_type,
                parent,
                false)
            return AchievedGoalHolder(binding)
        }

        override fun getItemCount() = achievedGoals.size

        override fun onBindViewHolder(holder: AchievedGoalHolder, position: Int) {
            val achievedGoal = achievedGoals[position]
            holder.bind(achievedGoal)
        }
    }

}