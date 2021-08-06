package com.daiwerystudio.chronos.ui.goal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.databinding.FragmentNotAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewNotAchievedGoalBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator


class NotAchievedGoalFragment: Fragment() {
    // ViewModel
    private val viewModel: NotAchievedGoalViewModel
    by lazy { ViewModelProvider(this).get(NotAchievedGoalViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentNotAchievedGoalBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentNotAchievedGoalBinding.inflate(inflater, container, false)
        val view = binding.root
        setLoadingView()

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        // Observation
        viewModel.goals.observe(viewLifecycleOwner, { goals ->
            setLoadingView()
            (binding.recyclerView.adapter as Adapter).setData(goals)
        })
        // Support swipe.
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        // Setting fab
        binding.fab.setOnClickListener{
            // Dialog
            val dialog = GoalDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("goal", Goal())
                putBoolean("isCreated", true)
            }
            dialog.show(this.requireActivity().supportFragmentManager, "GoalDialog")
        }

        return view
    }


    private fun setLoadingView(){
        binding.loadingView.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
    }
    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }
    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    // Support animation recyclerView
    private class DiffUtilCallback(private val oldList: List<Goal>,
                                   private val newList: List<Goal>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }


    private inner class Holder(private val binding: ItemRecyclerViewNotAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var goal: Goal

        init {
            itemView.setOnClickListener(this)

            // Setting edit
            binding.edit.setOnClickListener{
                // Dialog
                val dialog = GoalDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("goal", goal)
                    putBoolean("isCreated", false)
                }

                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
            }

            // Setting CheckBox
            binding.checkBox.setOnClickListener { v ->
                // Dialog
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.setAchievedGoalWithChild(goal)
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                        (v as CheckBox).isChecked = false
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
        }

        fun bind(goal: Goal) {
            this.goal = goal

            binding.goalName.text = goal.name
            if (goal.note != ""){
                binding.note.text = goal.note
                binding.note.visibility = View.VISIBLE
            } else {
                binding.note.visibility = View.GONE
            }

            val lifePercent = viewModel.getPercentAchieved(this.goal.id)
            lifePercent.observe(viewLifecycleOwner, { percent ->
                if (percent is Int) binding.progressBar.progress = percent
            })
        }

        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
             v.findNavController().navigate(R.id.action_navigation_preview_goal_to_navigation_goal, bundle)
        }
    }

    private inner class Adapter(var goals: List<Goal>): RecyclerView.Adapter<Holder>(){
        // Cringe Logic for animation
        private var lastPosition = -1

        fun setData(newData: List<Goal>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilCallback(goals, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update data
            goals = newData
            // Notify
            diffResult.dispatchUpdatesTo(this)

            // Show view
            if (goals.isEmpty()){
                setEmptyView()
            } else {
                setNullView()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_not_achieved_goal,
                parent, false))
        }

        override fun getItemCount() = goals.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(goals[position])

            // Animation
            if (holder.adapterPosition > lastPosition){
                lastPosition = holder.adapterPosition

                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_add_item)
                holder.itemView.startAnimation(animation)
            }
        }
    }

    // Support swiped
    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        CustomItemTouchCallback(requireContext(),0,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

        private val mAdapter = binding.recyclerView.adapter!!

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder) {
            viewModel.deleteGoalWithChild(viewModel.goals.value!![viewHolder.adapterPosition])
        }

        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }
}