/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.*

/**
 * Абстрактный класс для union фрагмента.
 */
abstract class UnionAbstractFragment : Fragment() {
    abstract val viewModel: UnionViewModel


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
            viewModel.setAchievedGoalWithChild(goal.id, !goal.isAchieved)
        }

        override fun setPercentAchieved() {
            // Percent удаляется, так как это не RoomLiveData.
            val percent = viewModel.getPercentAchieved(goal.id)
            percent.observe(viewLifecycleOwner, {
                binding.progressBar.progress = it
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

    override fun onPause() {
        super.onPause()

        viewModel.updateUnions()
    }
}