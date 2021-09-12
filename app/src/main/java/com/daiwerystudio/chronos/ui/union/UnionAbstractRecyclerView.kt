/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.*
import com.daiwerystudio.chronos.ui.FORMAT_DAY
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.daiwerystudio.chronos.ui.folder.FolderDialog
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import com.daiwerystudio.chronos.ui.schedule.ScheduleDialog
import java.time.format.FormatStyle

/**
 * Данный интерфейс означает, что у класса есть поле id. Нужен для обобщения DiffUtil на
 * ActionType, Goal и др.
 */
interface ID {
    val id: String
}

class UnionDiffUtil(private val oldList: List<ID>,
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
        setDeadline()
        setPercentAchieved()
    }

    override fun updateUI(old: ID, new: ID) {
        setStaticUI(new as Goal)
        if (new.isAchieved != binding.checkBox.isChecked) binding.checkBox.isChecked = new.isAchieved
    }

    open fun setDeadline(){
        binding.deadlineTextView.text = (formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_TIME)+
                " - " + formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_DAY))
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
        binding.start.text =  formatTime(schedule.start, true, FormatStyle.SHORT, FORMAT_DAY)
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
abstract class ReminderAbstractHolder(val binding: ItemRecyclerViewReminderBinding,
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
        setTime()
    }

    override fun updateUI(old: ID, new: ID) {
        this.reminder = new as Reminder
        binding.reminder = reminder
        setTime()
    }

    open fun setTime(){
        binding.timeTextView.text = (formatTime(reminder.time, true, FormatStyle.SHORT, FORMAT_TIME)+
                " - " + formatTime(reminder.time, true, FormatStyle.SHORT, FORMAT_DAY))
    }
}

/**
 * Абстрактный класс для холдера Folder с инициализацией UI и слушателей.
 */
abstract class FolderAbstractHolder(val binding: ItemRecyclerViewFolderBinding,
                                        private val fragmentManager: FragmentManager):
    RawHolder(binding.root) {
    lateinit var folder: Folder

    init {
        itemView.setOnClickListener{ onClicked() }
        binding.edit.setOnClickListener{
            val dialog = FolderDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("folder", folder)
                putBoolean("isCreated", false)
            }
            dialog.show(fragmentManager, "FolderDialog")
        }
    }

    override fun bind(item: ID) {
        this.folder = item as Folder
        binding.folder = folder
    }

    override fun updateUI(old: ID, new: ID) {
        new as Folder
        old as Folder
        this.folder = new
        if (old.name != new.name) binding.name.text = new.name
    }

    abstract fun onClicked()
}

/**
 * Абстрактный класс для адаптера RecyclerView. Он сам определяет, какой тип холдера нужно
 * создать и чем его заполнить. Но обертка каким классом холдера решается в реализации
 * этого класса в конкретных методах.
 */
abstract class UnionAbstractAdapter(var data: List<Pair<Int, ID>>,
                                    private val layoutInflater: LayoutInflater): RecyclerView.Adapter<RawHolder>() {
    open fun updateData(newData: List<Pair<Int, ID>>) {
        val diffUtilCallback = UnionDiffUtil(data.map { it.second }, newData.map { it.second })
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
            TYPE_FOLDER -> createFolderHolder(
                DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_folder,
                    parent, false))
            else -> throw IllegalArgumentException("Invalid type")
        }
    }
    abstract fun createActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): RawHolder
    abstract fun createGoalHolder(binding: ItemRecyclerViewGoalBinding): RawHolder
    abstract fun createScheduleHolder(binding: ItemRecyclerViewScheduleBinding): RawHolder
    abstract fun createNoteHolder(binding: ItemRecyclerViewNoteBinding): RawHolder
    abstract fun createReminderHolder(binding: ItemRecyclerViewReminderBinding): RawHolder
    abstract fun createFolderHolder(binding: ItemRecyclerViewFolderBinding): RawHolder

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


class UnionSimpleCallback(dragDirs: Int, swipeDirs: Int):
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    /*  Функции ниже означают, что функция onSwiped никогда не будет вызвана.  */
    override fun getSwipeEscapeVelocity(defaultValue: Float) = Float.MAX_VALUE
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = Float.MAX_VALUE

    /**
     * Находится ли холдер в взмахнутом состоянии. Определяется в onChildDraw: вышли ли
     * границы холедра за установленнную черту.
     */
    private var mIsSwipeLeft: Boolean = false
    private var mIsSwipeRight: Boolean = false

    /**
     * Позиция активного холдера в адаптере.
     */
    private var mSwipePosition: Int = 0

    /**
     * Так как mIsSwipe определяем в onChildDraw, которая запускается и при обратной анимации,
     * нам нужна переменная, в которой будет хранится, нужно ли спрашивать пользователя о взмахе.
     * Она устанавливается в onSelectedChanged, то есть в тот момент, когда пользователь
     * закончил взаимодействовать с холдером и когда холдер еще не вернулся обратно.
     */
    private var mIsActiveSwipeLeft: Boolean = false
    private var mIsActiveSwipeRight: Boolean = false

    var iconRight: Drawable? = null
    var backgroundRight: Drawable? = null
    var iconLeft: Drawable? = null
    var backgroundLeft: Drawable? = null
    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        var dx = dX
        when {
            dx < -viewHolder.itemView.width / 4f -> {
                dx = -viewHolder.itemView.width / 4f
                mIsSwipeLeft = true
                mIsSwipeRight = false
            }
            dx > viewHolder.itemView.width / 4f -> {
                dx = viewHolder.itemView.width / 4f
                mIsSwipeRight = true
                mIsSwipeLeft = false
            }
            else -> {
                mIsSwipeLeft = false
                mIsSwipeRight = false
            }
        }

        val itemView = viewHolder.itemView
        when {
            dx < 0 -> {
                iconLeft?.setBounds(0, 0, 0, 0)
                backgroundLeft?.setBounds(0, 0, 0, 0)

                backgroundRight?.setBounds(itemView.right - itemView.width/3,
                    itemView.top, itemView.right, itemView.bottom)

                iconRight?.also {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconRight = itemView.right - iconMargin/2
                    val iconLeft = iconRight - it.intrinsicWidth
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }
            }
            dx > 0 -> {
                iconRight?.setBounds(0, 0, 0, 0)
                backgroundRight?.setBounds(0, 0, 0, 0)

                backgroundLeft?.setBounds(itemView.left, itemView.top,
                    itemView.left+itemView.width/3, itemView.bottom)

                iconLeft?.also {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconLeft = itemView.left + iconMargin/2
                    val iconRight = iconLeft + it.intrinsicWidth
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }
            }
            else -> {
                iconRight?.setBounds(0, 0, 0, 0)
                backgroundRight?.setBounds(0, 0, 0, 0)
                iconLeft?.setBounds(0, 0, 0, 0)
                backgroundLeft?.setBounds(0, 0, 0, 0)
            }
        }

        backgroundRight?.draw(c)
        iconRight?.draw(c)
        backgroundLeft?.draw(c)
        iconLeft?.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dx, dY, actionState, isCurrentlyActive)
    }

    /**
     * Холдер, который перемещаем.
     */
    private var dragFromViewHolder: RecyclerView.ViewHolder? = null
    /**
     * Холдер, на который перместили.
     */
    private var dragToViewHolder: RecyclerView.ViewHolder? = null

    /**
     * Запускается, когда пользователь переместил холдер на другой холдер.
     */
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        clearViewDragToViewHolder()
        dragToViewHolder = target
        setViewDragToViewHolder()
        return true
    }

    /**
     * Запускается, когда меняется состояние холдера. То есть когда пользователь начинает
     * или заканчивает взаимодействовать с холдером.
     */
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder != null) mSwipePosition = viewHolder.absoluteAdapterPosition
        if (mIsSwipeLeft) mIsActiveSwipeLeft = true
        if (mIsSwipeRight) mIsActiveSwipeRight = true

        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                dragFromViewHolder = viewHolder!!
                setViewDragFromViewHolder()
            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                if (dragFromViewHolder != null && dragToViewHolder != null) {
                    mDragItemListener?.dragItem(dragFromViewHolder!!.absoluteAdapterPosition,
                        dragToViewHolder!!.absoluteAdapterPosition )
                    resetViewHolders()
                }
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    /**
     * Выполнятеся, когда хочет переместить холдер.
     */
    private fun setViewDragFromViewHolder(){
    }
    /**
     * Очищает DragFromViewHolder.
     */
    private fun clearViewDragFromViewHolder(){
    }

    /**
     * Выполняется, когда пользователь переместил холдер на другой холдер.
     */
    var backgroundDragToViewHolder: Drawable? = null
    private fun setViewDragToViewHolder(){
        dragToViewHolder?.also { it.itemView.background = backgroundDragToViewHolder }
    }
    /**
     * Очищает DragToViewHolder.
     */
    private fun clearViewDragToViewHolder(){
        dragToViewHolder?.also { it.itemView.background = null }
    }

    /**
     * Reset всех холдеров.
     */
    private fun resetViewHolders(){
        clearViewDragFromViewHolder()
        clearViewDragToViewHolder()
        dragFromViewHolder = null
        dragToViewHolder = null
    }

    /**
     * Запускается после окончания взаимодействия пользователя с холдером и окончания анимации
     * возвращения.
     */
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (mIsActiveSwipeLeft) {
            mSwipeListener?.swipeLeft(mSwipePosition)
            mIsActiveSwipeLeft = false
        }
        if (mIsActiveSwipeRight) {
            mSwipeListener?.swipeRight(mSwipePosition)
            mIsActiveSwipeRight = false
        }
    }

    /* Интерфейс, вызываемый при событии взмахивания.  */
    private var mSwipeListener: SwipeListener? = null
    interface SwipeListener{
        fun swipeLeft(position: Int)
        fun swipeRight(position: Int)
    }
    fun setSwipeItemListener(swipeListener: SwipeListener){
        mSwipeListener = swipeListener
    }

    /* Интерфейс, вызываемый при событии drag. */
    private var mDragItemListener: DragItemListener? = null
    fun interface DragItemListener{
        fun dragItem(dragFromPosition: Int, dragToPosition: Int)
    }
    fun setDragItemListener(dragItemListener: DragItemListener){
        mDragItemListener = dragItemListener
    }

}







