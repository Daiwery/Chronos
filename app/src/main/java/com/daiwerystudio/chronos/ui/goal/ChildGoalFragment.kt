package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.databinding.FragmentChildGoalBinding
import com.daiwerystudio.chronos.databinding.ListItemAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.ListItemNotAchievedGoalBinding


private const val TYPE_ACHIEVED_GOAL = 1
private const val TYPE_NOT_ACHIEVED_GOAL = 2

class ChildGoalFragment : Fragment() {
    // ViewModel
    private val viewModel: ChildGoalViewModel
    by lazy { ViewModelProvider(this).get(ChildGoalViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentChildGoalBinding
    // Bundle
    private var bundle = Bundle()
    private lateinit var parentGoal: Goal


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        // Get parentGoal and update goals
        parentGoal = arguments?.getSerializable("parentGoal") as Goal
        viewModel.getGoalsFromParent(parentGoal.id.toString())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentChildGoalBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.goal = parentGoal

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GoalAdapter(emptyList())
        }

        // Setting fab
        binding.fab.setOnClickListener{ v ->
            bundle.putSerializable("parentGoal", parentGoal)
            v.findNavController().navigate(R.id.action_navigation_child_goal_to_navigation_item_goal, bundle)
        }

        // Setting checkBox
        binding.isAchieved.setOnClickListener { v ->
            viewModel.setAchievedGoalWithChild(parentGoal)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observation of goals
        viewModel.goals.observe(viewLifecycleOwner, Observer {
                goals -> binding.recyclerView.adapter = GoalAdapter(goals)
        })
    }


    override fun onStart() {
        super.onStart()
        bundle.clear()
    }

    // Set menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_child, menu)
    }

    // Click on element in menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                bundle.putSerializable("goal", parentGoal)
                requireActivity().findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.action_navigation_child_goal_to_navigation_item_goal, bundle)
                return true
            }
            R.id.delete -> {
                viewModel.deleteGoalWithChild(parentGoal)
                requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private inner class AchievedGoalHolder(private val binding: ListItemAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var achievedGoal: Goal

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(achievedGoal: Goal) {
            this.achievedGoal = achievedGoal

            binding.goalName.text = this.achievedGoal.name
        }

        override fun onClick(v: View) {
            bundle.putSerializable("parentGoal", achievedGoal)
            v.findNavController().navigate(R.id.action_navigation_child_goal_self, bundle)
        }
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
            v.findNavController().navigate(R.id.action_navigation_child_goal_self, bundle)
        }
    }

    private inner class GoalAdapter(var goals: List<Goal>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
            return when (viewType){
                TYPE_ACHIEVED_GOAL -> AchievedGoalHolder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.list_item_achieved_goal,
                    parent,
                    false))

                TYPE_NOT_ACHIEVED_GOAL -> NotAchievedGoalHolder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.list_item_not_achieved_goal,
                    parent,
                    false))
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun getItemCount() = goals.size

        override fun getItemViewType(position: Int): Int {
            if (goals[position].isAchieved){
                return TYPE_ACHIEVED_GOAL
            } else {
                return TYPE_NOT_ACHIEVED_GOAL
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val goal = goals[position]
            when (this.getItemViewType(position)){
                TYPE_ACHIEVED_GOAL -> (holder as AchievedGoalHolder).bind(goal)
                TYPE_NOT_ACHIEVED_GOAL -> (holder as NotAchievedGoalHolder).bind(goal)
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }
    }

}