/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.FragmentUnionGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionTypeBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewScheduleBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.daiwerystudio.chronos.ui.schedule.ScheduleDialog
import com.daiwerystudio.chronos.ui.union.*
import java.lang.IllegalArgumentException
import java.util.*

class UnionGoalFragment : Fragment() {
    private val viewModel: UnionGoalViewModel
        by lazy { ViewModelProvider(this).get(UnionGoalViewModel::class.java) }
    private lateinit var binding: FragmentUnionGoalBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.parentID.value = arguments?.getString("parentID")!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionGoalBinding.inflate(inflater, container, false)
        val view = binding.root

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.appBar.title = it.name
        })

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.appBar.title = it.name
        })

        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).updateData(it)
        })

        binding.editMore.setOnClickListener {
            val bundle = Bundle().apply {
                putString("goalID", viewModel.parentID.value!!)
            }
            it.findNavController().navigate(R.id.action_global_navigation_goal, bundle)
        }

        binding.fab.setOnClickListener{
            val popup = UnionPopupMenu(requireActivity().supportFragmentManager, requireContext(), it)
            popup.setUnionBuilder(object : UnionPopupMenu.UnionBuilder {
                override fun getParent(): String = viewModel.parentID.value!!
                override fun getIndexList(): Int = viewModel.data.value!!.size
            })
            popup.show()
        }

        binding.appBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    viewModel.updateUnions()
                    val dialog = GoalDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("goal", viewModel.parent.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.App_AlertDialog)
                        .setTitle(resources.getString(R.string.are_you_sure))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.deleteUnionWithChild(viewModel.parentID.value!!)
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

    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }


    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    private inner class ActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding):
        ActionTypeRawHolder(binding, requireActivity().supportFragmentManager)

    private inner class GoalHolder(binding: ItemRecyclerViewGoalBinding) :
        GoalRawHolder(binding, requireActivity().supportFragmentManager){
        override fun onAchieved() {
            viewModel.setAchievedGoalWithChild(goal.id, !goal.isAchieved)
        }
    }

    private inner class ScheduleHolder(binding: ItemRecyclerViewScheduleBinding):
        ScheduleRawHolder(binding, requireActivity().supportFragmentManager) {
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

        override fun createActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): RawHolder {
            return ActionTypeHolder(binding)
        }

        override fun createGoalHolder(binding: ItemRecyclerViewGoalBinding): RawHolder {
            return GoalHolder(binding)
        }

        override fun createScheduleHolder(binding: ItemRecyclerViewScheduleBinding): RawHolder {
            return ScheduleHolder(binding)
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