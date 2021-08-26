/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.*
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import com.daiwerystudio.chronos.ui.schedule.ScheduleDialog

/**
 * Данный интерфейс означает, что у класса есть поле id. Нужен для обобщения DiffUtil на
 * ActionType, Goal и др.
 */
interface ID {
    val id: String
}

class CustomDiffUtil(private val oldList: List<ID>,
                             private val newList: List<ID>): DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    // Если только изменения UI, то посылаем пару из старых и новых данных.
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        return Pair(oldList[oldItemPosition], newList[newItemPosition])
    }
}

class ItemAnimator: DefaultItemAnimator(){
    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        val itemView = holder!!.itemView

        // После окончания анимации нужно вызвать dispatchAnimationFinished(holder).
        val listener = object : Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                dispatchAnimationFinished(holder)
            }
        }

        val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.anim_add_item)
        animation.setAnimationListener(listener)
        itemView.startAnimation(animation)

        return true
    }

    override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder,
                               preInfo: ItemHolderInfo, postInfo: ItemHolderInfo): Boolean {
        return false
    }
}

/**
 * Абстрактный класс для всех холдеров.
 */
open class RawHolder(view: View) : RecyclerView.ViewHolder(view) {
    open fun bind(item: ID) {}
    open fun updateUI(old: ID, new: ID) {}
}

/**
 * Абстрактный класс для холдера ActionType с инициализацией UI и слушателей.
 */
abstract class ActionTypeAbstractHolder(val binding: ItemRecyclerViewActionTypeBinding,
                                        private val fragmentManager: FragmentManager):
    RawHolder(binding.root) {
    lateinit var actionType: ActionType

    init {
        itemView.setOnClickListener{ onClicked() }
        binding.edit.setOnClickListener{
            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("actionType", actionType)
                putBoolean("isCreated", false)
            }
            dialog.show(fragmentManager, "ActionTypeDialog")
        }
    }

    override fun bind(item: ID) {
        this.actionType = item as ActionType
        binding.actionType = actionType
    }

    override fun updateUI(old: ID, new: ID) {
        new as ActionType
        old as ActionType
        this.actionType = new
        if (old.name != new.name) binding.name.text = new.name
        if (old.color != new.color) binding.color.setColorFilter(new.color)
    }

    abstract fun onClicked()
}

/**
 * Абстрактный класс для холдера Goal с инициализацией UI и слушателей.
 */
abstract class GoalAbstractHolder(val binding: ItemRecyclerViewGoalBinding,
                                  private val fragmentManager: FragmentManager):
    RawHolder(binding.root) {
    lateinit var goal: Goal

    init {
        itemView.setOnClickListener{ onClicked() }
        binding.edit.setOnClickListener{
            val dialog = GoalDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("goal", goal)
                putBoolean("isCreated", false)
            }
            dialog.show(fragmentManager, "GoalDialog")
        }
        binding.checkBox.setOnClickListener { onAchieved() }
    }

    override fun bind(item: ID) {
        setStaticUI(item as Goal)
        binding.checkBox.isChecked = item.isAchieved
    }

    open fun setStaticUI(goal: Goal){
        this.goal = goal
        binding.goal = goal
        binding.isAchieved = goal.isAchieved

        // Percent удаляется, так как это не RoomLiveData.
        setPercentAchieved()
    }

    override fun updateUI(old: ID, new: ID) {
        setStaticUI(new as Goal)
        if (new.isAchieved != binding.checkBox.isChecked) binding.checkBox.isChecked = new.isAchieved
    }

    abstract fun onAchieved()
    abstract fun setPercentAchieved()
    abstract fun onClicked()
}

/**
 * Абстрактный класс для холдера Schedule с инициализацией UI и слушателей.
 */
abstract class ScheduleAbstractHolder(private val binding: ItemRecyclerViewScheduleBinding,
                                      private val fragmentManager: FragmentManager):
    RawHolder(binding.root) {
    lateinit var schedule: Schedule

    init {
        itemView.setOnClickListener{ onClicked() }
        binding.edit.setOnClickListener{
            val dialog = ScheduleDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("schedule", schedule)
                putBoolean("isCreated", false)
            }
            dialog.show(fragmentManager, "ScheduleDialog")
        }
        binding.activeSwitch.setOnClickListener { onActive() }
    }

    override fun bind(item: ID) {
        setStaticUI(item as Schedule)
        binding.activeSwitch.isChecked = schedule.isActive
    }

    open fun setStaticUI(schedule: Schedule){
        this.schedule = schedule
        binding.schedule = schedule
        when (schedule.type){
            TYPE_SCHEDULE_PERIODIC -> binding.type.text = itemView.context.getString(R.string.periodic_schedule)
            TYPE_SCHEDULE_ONCE -> binding.type.text = itemView.context.getString(R.string.once_schedule)
            else -> throw java.lang.IllegalArgumentException("Invalid type")
        }
    }

    override fun updateUI(old: ID, new: ID) {
        setStaticUI(new as Schedule)
        if (schedule.isActive != binding.activeSwitch.isChecked)
            binding.activeSwitch.isChecked = schedule.isActive
    }

    abstract fun onActive()
    abstract fun onClicked()
}

/**
 * Абстрактный класс для холдера Note с инициализацией UI и слушателей.
 */
abstract class NoteAbstractHolder(private val binding: ItemRecyclerViewNoteBinding):
    RawHolder(binding.root) {
    lateinit var note: Note

    init {
        itemView.setOnClickListener{ onClicked() }
        binding.edit.setOnClickListener{
            val bundle = Bundle().apply{
                putSerializable("note", note)
            }
            itemView.findNavController().navigate(R.id.action_global_navigation_note, bundle)
        }
    }

    override fun bind(item: ID) {
        this.note = item as Note
        binding.note = note
    }

    override fun updateUI(old: ID, new: ID) {
        this.note = new as Note
        binding.note = note
    }

    abstract fun onClicked()
}

/**
 * Абстрактный класс для холдера Reminder с инициализацией UI и слушателей.
 */
abstract class ReminderAbstractHolder(private val binding: ItemRecyclerViewReminderBinding,
                                      private val fragmentManager: FragmentManager):
    RawHolder(binding.root) {
    lateinit var reminder: Reminder

    init {
        itemView.setOnClickListener{
            val dialog = ReminderDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("reminder", reminder)
                putBoolean("isCreated", false)
            }
            dialog.show(fragmentManager, "ReminderDialog")
        }
    }

    override fun bind(item: ID) {
        this.reminder = item as Reminder
        binding.reminder = reminder
    }

    override fun updateUI(old: ID, new: ID) {
        this.reminder = new as Reminder
        binding.reminder = reminder
    }
}

/**
 * Абстрактный класс для адаптера RecyclerView. Он сам определяет, какой тип холдера нужно
 * создать и чем его заполнить. Но обертка каким классом холдера решается в реализации
 * этого класса в конкретных методах.
 */
abstract class UnionAbstractAdapter(var data: List<Pair<Int, ID>>,
                                    private val layoutInflater: LayoutInflater): RecyclerView.Adapter<RawHolder>() {
    open fun updateData(newData: List<Pair<Int, ID>>) {
        val diffUtilCallback = CustomDiffUtil(data.map { it.second }, newData.map { it.second })
        val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

        data = newData
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].first

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RawHolder {
        return when (viewType) {
            TYPE_ACTION_TYPE -> createActionTypeHolder(
                DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_action_type,
                    parent, false))
            TYPE_GOAL -> createGoalHolder(
                DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_goal,
                    parent, false))
            TYPE_SCHEDULE -> createScheduleHolder(
                DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_schedule,
                    parent, false))
            TYPE_NOTE -> createNoteHolder(
                DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_note,
                    parent, false))
            TYPE_REMINDER -> createReminderHolder(
                DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_reminder,
                    parent, false))
            else -> throw IllegalArgumentException("Invalid type")
        }
    }
    abstract fun createActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): RawHolder
    abstract fun createGoalHolder(binding: ItemRecyclerViewGoalBinding): RawHolder
    abstract fun createScheduleHolder(binding: ItemRecyclerViewScheduleBinding): RawHolder
    abstract fun createNoteHolder(binding: ItemRecyclerViewNoteBinding): RawHolder
    abstract fun createReminderHolder(binding: ItemRecyclerViewReminderBinding): RawHolder

    override fun onBindViewHolder(holder: RawHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position)
        else {
            val pair = payloads.last() as Pair<*, *>
            holder.updateUI(pair.first as ID, pair.second as ID)
        }
    }

    override fun onBindViewHolder(holder: RawHolder, position: Int) {
        holder.bind(data[position].second)
    }
}








