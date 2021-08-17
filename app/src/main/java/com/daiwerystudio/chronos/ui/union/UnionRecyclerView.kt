/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionTypeBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewScheduleBinding

/**
 * Данный интерфейс означает, что у класса есть поле id. Нужен для обобщения DiffUtil на
 * ActionType, Goal и др.
 */
interface ID {
    val id: String
}

private class CustomDiffUtil(private val oldList: List<ID>,
                             private val newList: List<ID>): DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return oldList[oldPosition].id == newList[newPosition].id
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return oldList[oldPosition] == newList[newPosition]
    }
}

/**
 * Абстрактный класс для холдера ActionType с инициализацией UI и слушателей.
 */
abstract class ActionTypeRawHolder(private val binding: ItemRecyclerViewActionTypeBinding):
    RecyclerView.ViewHolder(binding.root) {
    lateinit var actionType: ActionType

    init {
        itemView.setOnClickListener{ onClick() }
        binding.edit.setOnClickListener{ onEdit() }
    }

    open fun bind(actionType: ActionType) {
        this.actionType = actionType
        binding.actionType = actionType
    }

    abstract fun onClick()
    abstract fun onEdit()
}

/**
 * Абстрактный класс для холдера Goal с инициализацией UI и слушателей.
 */
abstract class GoalRawHolder(private val binding: ItemRecyclerViewGoalBinding):
    RecyclerView.ViewHolder(binding.root) {
    lateinit var goal: Goal

    init {
        itemView.setOnClickListener{ onClick() }
        binding.edit.setOnClickListener{ onEdit() }
        binding.checkBox.setOnClickListener { onAchieved() }
    }

    open fun bind(goal: Goal) {
        this.goal = goal
        binding.goal = goal
    }

    abstract fun onClick()
    abstract fun onEdit()
    abstract fun onAchieved()
}

/**
 * Абстрактный класс для холдера Schedule с инициализацией UI и слушателей.
 */
abstract class ScheduleRawHolder(private val binding: ItemRecyclerViewScheduleBinding):
    RecyclerView.ViewHolder(binding.root) {
    lateinit var schedule: Schedule

    init {
        itemView.setOnClickListener{ onClick() }
        binding.edit.setOnClickListener{ onEdit() }
        binding.activeSwitch.setOnClickListener { onActive() }
    }

    open fun bind(schedule: Schedule) {
        this.schedule = schedule
        binding.schedule = schedule
    }

    abstract fun onClick()
    abstract fun onEdit()
    abstract fun onActive()

}

/**
 * Абстрактный класс для адаптера RecyclerView. Он сам определяет, какой тип холдера нужно
 * создать и чем его заполнить. Но обертка каким классом холедера решается в реализации
 * этого класса в конкретных методах.
 */
abstract class UnionAdapter(var data: List<Pair<Int, ID>>,
                            private val layoutInflater: LayoutInflater): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    open fun updateData(newData: List<Pair<Int, ID>>) {
        val diffUtilCallback = CustomDiffUtil(data.map { it.second }, newData.map { it.second })
        val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

        data = newData
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].first

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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
            else -> throw IllegalArgumentException("Invalid type")
        }
    }
    abstract fun createActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): RecyclerView.ViewHolder
    abstract fun createGoalHolder(binding: ItemRecyclerViewGoalBinding): RecyclerView.ViewHolder
    abstract fun createScheduleHolder(binding: ItemRecyclerViewScheduleBinding): RecyclerView.ViewHolder

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_ACTION_TYPE -> bindActionTypeHolder(holder, data[position].second as ActionType)
            TYPE_GOAL -> bindGoalHolder(holder, data[position].second as Goal)
            TYPE_SCHEDULE -> bindScheduleHolder(holder, data[position].second as Schedule)
            else -> throw IllegalArgumentException("Invalid type")
        }
    }
    abstract fun bindActionTypeHolder(holder: RecyclerView.ViewHolder, actionType: ActionType)
    abstract fun bindGoalHolder(holder: RecyclerView.ViewHolder, goal: Goal)
    abstract fun bindScheduleHolder(holder: RecyclerView.ViewHolder, schedule: Schedule)
}








