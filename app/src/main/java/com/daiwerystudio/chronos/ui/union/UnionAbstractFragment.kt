/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.*
import com.daiwerystudio.chronos.ui.ItemTouchDragCallback

/**
 * Абстрактный класс для union фрагмента.
 */
abstract class UnionAbstractFragment : Fragment() {
    abstract val viewModel: UnionViewModel
    val itemTouchHelper by lazy {
        val simpleItemTouchCallback = ItemTouchDragCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN)
        simpleItemTouchCallback.background = ColorDrawable(Color.parseColor("#CA0000"))
        simpleItemTouchCallback.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_delete_24_white)

        simpleItemTouchCallback.setSwipeItemListener{ position ->
            AlertDialog.Builder(context, R.style.App_AlertDialog)
                .setTitle(R.string.are_you_sure)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.data.value?.also { viewModel.deleteUnionWithChild(it[position].second.id) }
                }
                .setNegativeButton(R.string.no){ _, _ -> }
                .setCancelable(false).create().show()
        }
        simpleItemTouchCallback.setDragItemListener{dragFromPosition, dragToPosition ->
            AlertDialog.Builder(context, R.style.App_AlertDialog)
                .setTitle(R.string.are_you_sure)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.editParentUnion(dragFromPosition, dragToPosition)
                }
                .setNegativeButton(R.string.no){ _, _ -> }
                .setCancelable(false).create().show()
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.showing.setData(arguments?.getString("parentID") ?: "",
            arguments?.getInt("typeShowing") ?: -1)
    }


    open inner class ActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding):
        ActionTypeAbstractHolder(binding, requireActivity().supportFragmentManager) {
        override fun onClicked() {
            val bundle = Bundle().apply {
                putString("parentID", actionType.id)
                putInt("typeShowing", viewModel.showing.typeShowing)
            }
            itemView.findNavController().navigate(R.id.action_global_navigation_union_action_type, bundle)
        }
    }

    open inner class GoalHolder(binding: ItemRecyclerViewGoalBinding) :
        GoalAbstractHolder(binding, requireActivity().supportFragmentManager){

        override fun onAchieved() {
            // Копируем, чтобы recyclerView смог засечь изменения.
            val mGoal = goal.copy()
            mGoal.isAchieved = binding.checkBox.isChecked
            if (binding.checkBox.isChecked) viewModel.setAchievedGoalWithChild(goal.id)
            else viewModel.updateGoal(mGoal)
        }

        override fun setPercentAchieved() {
            // Percent удаляется, так как это не RoomLiveData.
            val percent = viewModel.getPercentAchieved(goal.id)
            percent.observe(viewLifecycleOwner, {
                binding.progressBar.progress = it
                binding.progressTextView.text = ("$it%")
            })
        }

        override fun onClicked() {
            val bundle = Bundle().apply {
                putString("parentID", goal.id)
                putInt("typeShowing", viewModel.showing.typeShowing)
            }
            itemView.findNavController().navigate(R.id.action_global_navigation_union_goal, bundle)
        }
    }

    open inner class ScheduleHolder(binding: ItemRecyclerViewScheduleBinding):
        ScheduleAbstractHolder(binding, requireActivity().supportFragmentManager) {
        override fun onActive() {
            schedule.isActive = !schedule.isActive
            viewModel.updateSchedule(schedule)
        }

        override fun onClicked() {
            val bundle = Bundle().apply {
                putString("parentID", schedule.id)
                putInt("typeShowing", viewModel.showing.typeShowing)
            }
            itemView.findNavController().navigate(R.id.action_global_navigation_union_schedule, bundle)
        }
    }

    open inner class NoteHolder(binding: ItemRecyclerViewNoteBinding):
        NoteAbstractHolder(binding) {
        override fun onClicked() {
            val bundle = Bundle().apply {
                putString("parentID", note.id)
                putInt("typeShowing", viewModel.showing.typeShowing)
            }
            itemView.findNavController().navigate(R.id.action_global_navigation_union_note, bundle)
        }
    }

    open inner class ReminderHolder(binding: ItemRecyclerViewReminderBinding):
        ReminderAbstractHolder(binding, requireActivity().supportFragmentManager)


    open inner class UnionAdapter: UnionAbstractAdapter(emptyList(), layoutInflater){
        override fun createActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): RawHolder =
            ActionTypeHolder(binding)

        override fun createGoalHolder(binding: ItemRecyclerViewGoalBinding): RawHolder =
            GoalHolder(binding)

        override fun createScheduleHolder(binding: ItemRecyclerViewScheduleBinding): RawHolder =
            ScheduleHolder(binding)

        override fun createNoteHolder(binding: ItemRecyclerViewNoteBinding): RawHolder =
            NoteHolder(binding)

        override fun createReminderHolder(binding: ItemRecyclerViewReminderBinding): RawHolder =
            ReminderHolder(binding)
    }
}