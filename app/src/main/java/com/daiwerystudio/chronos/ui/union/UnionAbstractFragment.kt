/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.daiwerystudio.chronos.databinding.*

/**
 * Абстрактный класс для union фрагмента.
 */
abstract class UnionAbstractFragment : Fragment() {
    abstract val viewModel: UnionViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.parentID.value = arguments?.getString("parentID") ?: ""
    }


    open inner class ActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding):
        ActionTypeAbstractHolder(binding, requireActivity().supportFragmentManager)

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
    }

    open inner class ScheduleHolder(binding: ItemRecyclerViewScheduleBinding):
        ScheduleAbstractHolder(binding, requireActivity().supportFragmentManager) {
        override fun onActive() {
            schedule.isActive = !schedule.isActive
            viewModel.updateSchedule(schedule)
        }
    }

    open inner class NoteHolder(binding: ItemRecyclerViewNoteBinding):
        NoteAbstractHolder(binding)

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