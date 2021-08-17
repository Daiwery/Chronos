package com.daiwerystudio.chronos.ui.union

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.FragmentUnionPreviewBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionTypeBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewScheduleBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.schedule.ScheduleDialog
import java.util.*

class UnionPreviewFragment : Fragment() {
    private val viewModel: UnionViewModel
    by lazy { ViewModelProvider(this).get(UnionViewModel::class.java) }

    private lateinit var binding: FragmentUnionPreviewBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.parentID.value = ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionPreviewBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).updateData(it)
        })


        binding.fab.setOnClickListener{
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.menu_create_union_item, popup.menu)
            popup.setOnMenuItemClickListener (object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem?): Boolean {
                    when (item?.itemId){
                        R.id.create_action_type -> {
                            val id = UUID.randomUUID().toString()
                            val actionType = ActionType(id=id)
                            val union = Union(id=id, parent="", type=TYPE_ACTION_TYPE, indexList=viewModel.data.value!!.size)

                            val dialog = ActionTypeDialog()
                            dialog.arguments = Bundle().apply{
                                putSerializable("actionType", actionType)
                                putSerializable("union", union)
                                putBoolean("isCreated", true)
                            }
                            dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")

                            return true
                        }
                        R.id.create_goal -> {
                            val id = UUID.randomUUID().toString()
                            val goal = Goal(id=id)
                            val union = Union(id=id, parent="", type=TYPE_GOAL, indexList=viewModel.data.value!!.size)

                            val dialog = GoalDialog()
                            dialog.arguments = Bundle().apply{
                                putSerializable("goal", goal)
                                putSerializable("union", union)
                                putBoolean("isCreated", true)
                            }
                            dialog.show(requireActivity().supportFragmentManager, "GoalDialog")

                            return true
                        }
                        R.id.create_schedule -> {
                            val id = UUID.randomUUID().toString()
                            val schedule = Schedule(id=id)
                            val union = Union(id=id, parent="", type= TYPE_SCHEDULE, indexList=viewModel.data.value!!.size)

                            val dialog = ScheduleDialog()
                            dialog.arguments = Bundle().apply{
                                putSerializable("schedule", schedule)
                                putSerializable("union", union)
                                putBoolean("isCreated", true)
                            }
                            dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")

                            return true
                        }
                        else -> return false
                    }
                }
            })
            popup.show()
        }

        return view
    }

    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    private inner class ActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): ActionTypeRawHolder(binding){
        override fun onClick() {
            val bundle = Bundle().apply {
                putString("parentID", actionType.id)
            }
            itemView.findNavController().navigate(R.id.action_navigation_union_preview_to_navigation_union_action_type, bundle)
        }

        override fun onEdit() {
            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("actionType", actionType)
                putBoolean("isCreated", false)
            }
            dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
        }
    }


    private inner class GoalHolder(binding: ItemRecyclerViewGoalBinding) : GoalRawHolder(binding){
        override fun onClick() {
            val bundle = Bundle().apply {
                putString("parentID", goal.id)
            }
            itemView.findNavController().navigate(R.id.action_navigation_union_preview_to_navigation_union_goal, bundle)
        }

        override fun onEdit() {
            val dialog = GoalDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("goal", goal)
                putBoolean("isCreated", false)
            }
            dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
        }

        override fun onAchieved() {
            viewModel.setAchievedGoalWithChild(goal.id, !goal.isAchieved)
        }
    }


    private inner class ScheduleHolder(binding: ItemRecyclerViewScheduleBinding): ScheduleRawHolder(binding) {
        override fun onClick() {
            val bundle = Bundle().apply {
                putString("parentID", schedule.id)
            }
            itemView.findNavController().navigate(R.id.action_navigation_union_preview_to_navigation_union_schedule, bundle)
        }

        override fun onEdit() {
            val dialog = ScheduleDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("schedule", schedule)
                putBoolean("isCreated", false)
            }
            dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")
        }

        override fun onActive() {
            schedule.isActive = !schedule.isActive
            viewModel.updateSchedule(schedule)
        }
    }


    private inner class Adapter: UnionAdapter(emptyList(), layoutInflater){
        override fun updateData(newData: List<Pair<Int, ID>>) {
            super.updateData(newData)

            if (data.isEmpty()) setEmptyView()
            else setNullView()
        }

        override fun createActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): RecyclerView.ViewHolder {
            return ActionTypeHolder(binding)
        }

        override fun createGoalHolder(binding: ItemRecyclerViewGoalBinding): RecyclerView.ViewHolder {
            return GoalHolder(binding)
        }

        override fun createScheduleHolder(binding: ItemRecyclerViewScheduleBinding): RecyclerView.ViewHolder {
            return ScheduleHolder(binding)
        }

        override fun bindActionTypeHolder(holder: RecyclerView.ViewHolder, actionType: ActionType) {
            (holder as ActionTypeHolder).bind(actionType)
        }

        override fun bindGoalHolder(holder: RecyclerView.ViewHolder, goal: Goal) {
            (holder as GoalHolder).bind(goal)
        }

        override fun bindScheduleHolder(holder: RecyclerView.ViewHolder, schedule: Schedule) {
            (holder as ScheduleHolder).bind(schedule)
        }
    }


    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        CustomItemTouchCallback(requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
        private val mAdapter = binding.recyclerView.adapter!! as Adapter

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            viewModel.swap(from, to)
            Collections.swap(mAdapter.data, from, to)
            mAdapter.notifyItemMoved(from, to)

            return true
        }

        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder) {
            viewModel.deleteUnionWithChild(mAdapter.data[viewHolder.adapterPosition].second.id)
        }

        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }


    override fun onPause() {
        super.onPause()

        viewModel.updateUnions()
    }
}