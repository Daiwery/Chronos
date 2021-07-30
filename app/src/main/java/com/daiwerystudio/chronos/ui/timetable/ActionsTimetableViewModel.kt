package com.daiwerystudio.chronos.ui.timetable

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.ActionDrawable
import com.daiwerystudio.chronos.database.ActionTimetable
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.TimetableRepository
import com.daiwerystudio.chronos.database.ActionTypeRepository
import java.util.*


class ActionsTimetableViewModel : ViewModel() {
    private val timetableRepository = TimetableRepository.get()
    lateinit var actions: LiveData<MutableList<ActionTimetable>>

    fun updateData(timetableId: String, dayIndex: Int){
        actions = timetableRepository.getActionsTimetableFromDayIndex(timetableId, dayIndex)
    }
    fun deleteActionTimetable(actionTimetable: ActionTimetable){
        timetableRepository.deleteActionTimetable(actionTimetable)
    }

    fun updateListActionTimetable(listActionTimetable: List<ActionTimetable>){
        timetableRepository.updateListActionTimetable(listActionTimetable)
    }


    private val actionTypeRepository = ActionTypeRepository.get()
    fun getActionType(id: String): LiveData<ActionType> = actionTypeRepository.getActionType(id)


    fun getActionsDrawable(actions: List<ActionTimetable>): List<ActionDrawable> {
        val actionsDrawable = mutableListOf<ActionDrawable>()

        actions.forEachIndexed { i, action ->
            val color = actionTypeRepository.getColor(action.actionTypeId)
            var start = action.timeStart/(60f*24)
            if (i != 0) start += actionsDrawable[i - 1].end

            actionsDrawable.add(ActionDrawable(color, start, start+action.duration/(60f*24)))
        }

        return actionsDrawable
    }
}