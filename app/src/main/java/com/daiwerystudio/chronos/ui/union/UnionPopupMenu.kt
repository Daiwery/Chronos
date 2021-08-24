/*
* Дата создания: 20.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import com.daiwerystudio.chronos.ui.schedule.ScheduleDialog
import java.util.*


class UnionPopupMenu(val fragmentManager: FragmentManager,
                     val context: Context,
                     val view: View) : PopupMenu(context, view){
    init {
        menuInflater.inflate(R.menu.menu_create_union_item, menu)
        setOnMenuItemClickListener (object : OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                when (item?.itemId){
                    R.id.create_action_type -> {
                        val id = UUID.randomUUID().toString()
                        val actionType = ActionType(id=id)
                        val union = Union(id=id, parent=mUnionBuilder?.getParent() ?: "",
                            type=TYPE_ACTION_TYPE, indexList=mUnionBuilder?.getIndexList() ?: 0)

                        val dialog = ActionTypeDialog()
                        dialog.arguments = Bundle().apply{
                            putSerializable("actionType", actionType)
                            putSerializable("union", union)
                            putBoolean("isCreated", true)
                        }
                        dialog.show(fragmentManager, "ActionTypeDialog")

                        return true
                    }

                    R.id.create_goal -> {
                        val id = UUID.randomUUID().toString()
                        val goal = Goal(id=id)
                        val union = Union(id=id, parent=mUnionBuilder?.getParent() ?: "",
                            type=TYPE_GOAL, indexList=mUnionBuilder?.getIndexList() ?: 0)

                        val dialog = GoalDialog()
                        dialog.arguments = Bundle().apply{
                            putSerializable("goal", goal)
                            putSerializable("union", union)
                            putBoolean("isCreated", true)
                        }
                        dialog.show(fragmentManager, "GoalDialog")

                        return true
                    }

                    R.id.create_periodic_schedule -> {
                        val id = UUID.randomUUID().toString()
                        val schedule = Schedule(id=id, type=TYPE_SCHEDULE_PERIODIC)
                        val union = Union(id=id, parent=mUnionBuilder?.getParent() ?: "",
                            type=TYPE_SCHEDULE, indexList=mUnionBuilder?.getIndexList() ?: 0)

                        val dialog = ScheduleDialog()
                        dialog.arguments = Bundle().apply{
                            putSerializable("schedule", schedule)
                            putSerializable("union", union)
                            putBoolean("isCreated", true)
                        }
                        dialog.show(fragmentManager, "ScheduleDialog")

                        return true
                    }

                    R.id.create_once_schedule -> {
                        val id = UUID.randomUUID().toString()
                        val schedule = Schedule(id=id, type=TYPE_SCHEDULE_ONCE)
                        val union = Union(id=id, parent=mUnionBuilder?.getParent() ?: "",
                            type=TYPE_SCHEDULE, indexList=mUnionBuilder?.getIndexList() ?: 0)

                        val dialog = ScheduleDialog()
                        dialog.arguments = Bundle().apply{
                            putSerializable("schedule", schedule)
                            putSerializable("union", union)
                            putBoolean("isCreated", true)
                        }
                        dialog.show(fragmentManager, "ScheduleDialog")

                        return true
                    }

                    R.id.create_note -> {
                        val id = UUID.randomUUID().toString()
                        val note = Note(id=id)
                        val union = Union(id=id, parent=mUnionBuilder?.getParent() ?: "",
                            type=TYPE_NOTE, indexList=mUnionBuilder?.getIndexList() ?: 0)

                        val bundle = Bundle().apply{
                            putSerializable("note", note)
                            putSerializable("union", union)
                        }
                        view.findNavController().navigate(R.id.action_global_navigation_note, bundle)

                        return true
                    }

                    R.id.create_reminder -> {
                        val id = UUID.randomUUID().toString()
                        val reminder = Reminder(id=id)
                        val union = Union(id=id, parent=mUnionBuilder?.getParent() ?: "",
                            type=TYPE_REMINDER, indexList=mUnionBuilder?.getIndexList() ?: 0)

                        val dialog = ReminderDialog()
                        dialog.arguments = Bundle().apply{
                            putSerializable("reminder", reminder)
                            putSerializable("union", union)
                            putBoolean("isCreated", true)
                        }
                        dialog.show(fragmentManager, "ReminderDialog")

                        return true
                    }
                    else -> return false
                }
            }
        })
    }

    private var mUnionBuilder: UnionBuilder? = null
    interface UnionBuilder{
        fun getParent(): String
        fun getIndexList(): Int
    }
    fun setUnionBuilder(unionBuilder: UnionBuilder){
        mUnionBuilder = unionBuilder
    }
}
