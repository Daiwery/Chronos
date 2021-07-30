 package com.daiwerystudio.chronos.ui.timetable

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.ActionDrawable
import com.daiwerystudio.chronos.ClockDrawable
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionTimetable
import com.daiwerystudio.chronos.database.Timetable
import com.daiwerystudio.chronos.databinding.FragmentActionsTimetableBinding
import com.daiwerystudio.chronos.databinding.ListItemActionTimetableBinding
import java.util.*
import java.util.concurrent.Executors
import kotlin.properties.Delegates


class ActionsTimetableFragment(var timetable: Timetable, var dayIndex: Int) : Fragment() {
    // ViewModel
    private val viewModel: ActionsTimetableViewModel
    by lazy { ViewModelProvider(this).get(ActionsTimetableViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentActionsTimetableBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentActionsTimetableBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
        }
        // Support move and swipe. Смотри ниже.
        itemTouchHelper.attachToRecyclerView( binding.recyclerView)


        // Setting clock
        binding.clock.setImageDrawable(ClockDrawable(emptyList()))

        // Setting fab
        binding.fab.setOnClickListener{ v: View ->
            // Action
            val action = ActionTimetable(timetableId = timetable.id,
                dayIndex = dayIndex, indexList = viewModel.actions.value?.size!!)

            // Dialog
            val dialog = ActionTimetableDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("action", action)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionTimetableDialog")
        }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.updateData(timetable.id, dayIndex)
        // Observation
        viewModel.actions.observe(viewLifecycleOwner, Observer { actions ->
            // RecyclerView
            (binding.recyclerView.adapter as Adapter).setData(actions)
            // Clock
            Executors.newSingleThreadExecutor().execute {
                binding.clock.setImageDrawable(ClockDrawable(viewModel.getActionsDrawable(actions)))
            }
        })

    }


    override fun onPause() {
        viewModel.updateListActionTimetable(viewModel.actions.value!!)

        super.onPause()
    }



    // Support animation recyclerView
    private class DiffUtilCallback(private val oldList: List<ActionTimetable>,
                                   private val newList: List<ActionTimetable>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].actionTypeId == newList[newPosition].actionTypeId
        }
    }

    private inner class Holder(private val binding: ListItemActionTimetableBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var action: ActionTimetable


        init {
            itemView.setOnClickListener(this)
        }

        fun bind(action: ActionTimetable, position: Int) {
            this.action = action

            val liveActionType = viewModel.getActionType(action.actionTypeId)
            liveActionType.observe(viewLifecycleOwner, Observer { actionType ->
                binding.actionType = actionType
            })
        }

        override fun onClick(v: View) {
            val dialog = ActionTimetableDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("action", action)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionTimetableDialog")
        }
    }

    private inner class Adapter(var actions: List<ActionTimetable>): RecyclerView.Adapter<Holder>(){
        fun setData(newActions: List<ActionTimetable>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilCallback(actions, newActions)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update data
            this.actions = newActions
            // Animation
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
            return Holder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.list_item_action_timetable,
                    parent, false))
        }

        override fun getItemCount() = actions.size


        override fun onBindViewHolder(holder: Holder, position: Int) {
            val action = actions[position]
            holder.bind(action, position)
        }
    }


    // Support move and swiped
    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        ItemTouchHelper.SimpleCallback(UP or DOWN, RIGHT or LEFT) {

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            // Yeah, symmetry
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            // Update index
            viewModel.actions.value!![from].indexList = to
            viewModel.actions.value!![to].indexList = from

            // Update recyclerView
            Collections.swap(viewModel.actions.value!!, from, to)
            recyclerView.adapter?.notifyItemMoved(from, to)


            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            viewModel.deleteActionTimetable(viewModel.actions.value!![viewHolder.adapterPosition])
        }


        }

        ItemTouchHelper(simpleItemTouchCallback)
    }

}