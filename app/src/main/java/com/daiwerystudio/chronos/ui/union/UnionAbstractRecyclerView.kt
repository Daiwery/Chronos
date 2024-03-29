/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DefaultItemAnimator


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
}

class UnionItemAnimator: DefaultItemAnimator(){
    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        val itemView = holder!!.itemView
        itemView.alpha = 0f
        val animation = ObjectAnimator.ofFloat(itemView, "alpha",  1f).setDuration(300)
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                dispatchAnimationFinished(holder)
            }
        })
        animation.start()

        return true
    }
}


/**
 * Абстрактный класс для всех холдеров.
 */
open class RawHolder(view: View) : RecyclerView.ViewHolder(view) {
    open fun bind(item: ID) {}
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
        binding.color.setColorFilter(actionType.color)
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
        this.goal = item as Goal
        binding.goal = goal
        if (binding.checkBox.isChecked != goal.isAchieved) binding.checkBox.isChecked = goal.isAchieved
        binding.deadlineTextView.text = (formatTime(goal.deadline, FormatStyle.SHORT,
            FORMAT_TIME, true, is24HourFormat(itemView.context)) + " - "
                + formatTime(goal.deadline, FormatStyle.SHORT, FORMAT_DAY,
            true, is24HourFormat(itemView.context)))
        setPercentAchieved()

        if (goal.note != "") binding.note.visibility = View.VISIBLE
        else binding.note.visibility = View.GONE

        if (goal.type == TYPE_GOAL_TEMPORARY) {
            binding.textView13.visibility = View.VISIBLE
            binding.deadlineTextView.visibility = View.VISIBLE
        } else {
            binding.textView13.visibility = View.GONE
            binding.deadlineTextView.visibility = View.GONE
        }
    }

    abstract fun onAchieved()
    abstract fun setPercentAchieved()
    abstract fun onClicked()
}

/**
 * Абстрактный класс для холдера Schedule с инициализацией UI и слушателей.
 */
abstract class ScheduleAbstractHolder(val binding: ItemRecyclerViewScheduleBinding,
                                      private val fragmentManager: FragmentManager):
    RawHolder(binding.root) {
    lateinit var schedule: Schedule

    init {
        itemView.setOnClickListener{ onClicked() }
        binding.edit.setOnClickListener{ onEdit() }
        binding.activeSwitch.setOnClickListener { onActive() }
    }

    override fun bind(item: ID) {
        this.schedule = item as Schedule
        binding.schedule = schedule
        binding.start.text = formatTime(schedule.start, FormatStyle.SHORT, FORMAT_DAY,
            true, is24HourFormat(itemView.context))
        when (schedule.type){
            TYPE_SCHEDULE_PERIODIC -> binding.type.text = itemView.context.getString(R.string.periodic_schedule)
            TYPE_SCHEDULE_ONCE -> binding.type.text = itemView.context.getString(R.string.once_schedule)
            else -> throw java.lang.IllegalArgumentException("Invalid type")
        }
        if (binding.activeSwitch.isChecked != schedule.isActive)
            binding.activeSwitch.isChecked = schedule.isActive

        if (schedule.type == TYPE_SCHEDULE_PERIODIC){
            binding.textView5.visibility = View.VISIBLE
            binding.countDays.visibility = View.VISIBLE
            binding.textView.visibility = View.VISIBLE
        } else {
            binding.textView5.visibility = View.GONE
            binding.countDays.visibility = View.GONE
            binding.textView.visibility = View.GONE
        }
    }

    abstract fun onActive()
    abstract fun onClicked()
    abstract fun onEdit()
}

/**
 * Абстрактный класс для холдера Note с инициализацией UI и слушателей.
 */
abstract class NoteAbstractHolder(val binding: ItemRecyclerViewNoteBinding):
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

        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintLayout)
        if (note.note == "") constraintSet.connect(R.id.name, ConstraintSet.BOTTOM,
                R.id.constraintLayout, ConstraintSet.BOTTOM)
        else constraintSet.clear(R.id.name, ConstraintSet.BOTTOM)
        constraintSet.applyTo(binding.constraintLayout)

        // Изменения должны быть последовательноми, а не происходить одновременно.
        if (note.note == "") binding.noteTextView.visibility = View.GONE
        else binding.noteTextView.visibility = View.VISIBLE
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
        itemView.setOnClickListener{ onClicked() }
        binding.edit.setOnClickListener{
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
        binding.timeTextView.text = (formatTime(reminder.time, FormatStyle.SHORT, FORMAT_TIME,
            true, is24HourFormat(itemView.context))+ " - "
                + formatTime(reminder.time, FormatStyle.SHORT, FORMAT_DAY,
            true, is24HourFormat(itemView.context)))
    }

    abstract fun onClicked()
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

    override fun onBindViewHolder(holder: RawHolder, position: Int) {
        holder.bind(data[position].second)
    }
}


open class UnionSimpleCallback(dragDirs: Int, swipeDirs: Int):
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

    /**
     * Холдер, который перемещаем.
     */
    private var dragFromViewHolder: RecyclerView.ViewHolder? = null
    /**
     * Холдер, на который перместили.
     */
    private var dragToViewHolder: RecyclerView.ViewHolder? = null

    /**
     * Время начала обработки события drag с конкретным холдером. Чтобы событие drag произошло,
     * нужно подождать некоторое время после начала.
     */
    private var timeStartDragging: Long = 0

    /**
     * Можно ли отправить событие drag.
     */
    private var permissionDragging: Boolean = false

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
     * Запускается, когда пользователь переместил холдеры.
     */
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        mMoveListener?.moveItem(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
        recyclerView.adapter?.notifyItemMoved(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
        return true
    }

    /**
     * При длительном нажатии теперь мы не начинам перетаскивать холдер.
     */
    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    /**
     * Это означает, что в ItemTouchHelper начнет обрабатывать наложение холдеров,
     * если процент их наложения больше x% (по умолчанию 50%)
     */
    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float = .1f

    /**
     * Здесь мы определяем, какое событие должно произойти: move или drag. Если current холдер
     * персекает холдер больше, меньше, чем на 90 процентов, то это событие drag. Иначе move.
     * Если функция вернет true, то target холдер будет передаваться дальше и в конечном итоге
     * попадет в onMove.
     */
    override fun canDropOver(recyclerView: RecyclerView,
                             current: RecyclerView.ViewHolder,
                             target: RecyclerView.ViewHolder): Boolean {
        val curY = current.itemView.translationY+current.itemView.top
        val ratio = if (current.itemView.translationY > 0)
            (curY+current.itemView.height-target.itemView.top)*1f/target.itemView.height
        else (target.itemView.bottom-curY)*1f/target.itemView.height

        return when {
            ratio < .25f -> {
                dragToViewHolder = null
                permissionDragging = false
                mDraggindViewHolderSetter?.endDrag()
                false
            }
            ratio < .75f -> {
                // Если это новый холдер, то это начало обработки события drag.
                if (dragToViewHolder != target) {
                    dragToViewHolder = target
                    timeStartDragging = System.currentTimeMillis()
                }
                else if (System.currentTimeMillis()-timeStartDragging > 100) {
                    permissionDragging = true
                    mDraggindViewHolderSetter?.startDrag(dragFromViewHolder!!, dragToViewHolder!!)
                }
                false
            }
            else -> {
                dragToViewHolder = null
                permissionDragging = false
                mDraggindViewHolderSetter?.endDrag()
                true
            }
        }
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
            ItemTouchHelper.ACTION_STATE_DRAG -> dragFromViewHolder = viewHolder
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                if (dragFromViewHolder != null && dragToViewHolder != null && permissionDragging) {
                    mDragListener?.dragItem(dragFromViewHolder!!.absoluteAdapterPosition,
                        dragToViewHolder!!.absoluteAdapterPosition )

                    mDraggindViewHolderSetter?.endDrag()
                    dragFromViewHolder = null
                    dragToViewHolder = null
                }
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
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


    /* Интерфейс, вызываемый при событии swipe.  */
    private var mSwipeListener: SwipeListener? = null
    interface SwipeListener{
        fun swipeLeft(position: Int)
        fun swipeRight(position: Int)
    }
    fun setSwipeItemListener(swipeListener: SwipeListener){
        mSwipeListener = swipeListener
    }

    /* Интерфейс, вызываемый при событии move. */
    private var mMoveListener: MoveListener? = null
    fun interface MoveListener{
        fun moveItem(fromPosition: Int, toPosition: Int)
    }
    fun setMoveListener(moveListener: MoveListener){
        mMoveListener = moveListener
    }

    /* Интерфейс, вызываемый при событии drag. */
    private var mDragListener: DragListener? = null
    fun interface DragListener{
        fun dragItem(fromPosition: Int, toPosition: Int)
    }
    fun setDragItemListener(dragListener: DragListener){
        mDragListener = dragListener
    }

    /* Интерфейс, вызываемый для установки внешнего вида холдеров при событии drag. */
    private var mDraggindViewHolderSetter: DraggindViewHolderSetter? = null
    interface DraggindViewHolderSetter{
        fun startDrag(dragFromViewHolder: RecyclerView.ViewHolder,
                      dragToViewHolder: RecyclerView.ViewHolder)
        fun endDrag()
    }
    fun setDraggindViewHolderSetter(draggindViewHolderSetter: DraggindViewHolderSetter){
        mDraggindViewHolderSetter = draggindViewHolderSetter
    }

}







