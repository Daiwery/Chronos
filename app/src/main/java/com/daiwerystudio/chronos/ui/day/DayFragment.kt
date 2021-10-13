/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 31.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменения, связанные с изменениями в DayViewModel.
*
* Дата изменения: 05.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: вместо Action показываются цели и напоминания на этот день.
*
* Дата изменения: 24.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавления логики работы с ClockViewModel и добавление отдельного холдера
* для пересечения.
*/

package com.daiwerystudio.chronos.ui.day

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.Reminder
import com.daiwerystudio.chronos.databinding.*
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.day.DayFabMenu.Companion.TYPE_GOAL
import com.daiwerystudio.chronos.ui.day.DayFabMenu.Companion.TYPE_REMINDER
import com.daiwerystudio.chronos.ui.day.DayViewModel.Companion.TYPE_SECTION
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import com.daiwerystudio.chronos.ui.union.ID
import com.daiwerystudio.chronos.ui.union.UnionItemAnimator
import com.daiwerystudio.chronos.ui.union.UnionSimpleCallback
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class DayFragment: Fragment() {
    private val viewModel: DayViewModel
        by lazy { ViewModelProvider(this).get(DayViewModel::class.java) }
    private lateinit var binding: FragmentDayBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.day.value == null)
            viewModel.day.value = (System.currentTimeMillis()+viewModel.local)/(1000*60*60*24)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDayBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = UnionItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.day.observe(viewLifecycleOwner, {
            binding.calendarView.date = viewModel.day.value!!*24*60*60*1000L
            binding.toolBar.title = LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        })
        viewModel.mActionsSchedule.observe(viewLifecycleOwner, { setLoadingView() })
        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).updateData(it)
        })
        viewModel.actionDrawables.observe(viewLifecycleOwner, {
            binding.clock.setActionDrawables(it)
        })

        binding.toolBar.setOnClickListener {
            // Календарь ведет себя очень странно. Если делать по-другому, то он не будет работать.
            binding.motionLayout.transitionToStart()
            if (binding.calendarView.visibility == View.VISIBLE) {
                binding.calendarView.visibility = View.GONE
                binding.fab.show()
                ObjectAnimator.ofFloat(binding.imageView9, "rotation",  0f)
                    .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
            }
            else {
                binding.calendarView.visibility = View.VISIBLE
                binding.fab.hide()
                ObjectAnimator.ofFloat(binding.imageView9, "rotation",  90f)
                    .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
            }
        }

        binding.motionLayout.setTransitionListener(object : MotionLayout.TransitionListener{
            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int,
                                             positive: Boolean, progress: Float) {}
            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {}
            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int,
                                            endId: Int, progress: Float) {}
            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.end) {
                    binding.calendarView.visibility = View.GONE
                    binding.fab.show()
                    ObjectAnimator.ofFloat(binding.imageView9, "rotation",  0f)
                        .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
                }
            }
        })

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (binding.calendarView.visibility == View.GONE) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                        if (binding.fab.state != DayFabMenu.STATE_INVISIBLE) binding.fab.hide()
                        else binding.fab.show()
                }
            }
        })

        binding.calendarView.visibility = View.GONE
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.day.value = LocalDate.of(year, month+1, dayOfMonth).toEpochDay()
        }

        binding.fab.setOnMenuItemClickListener{
            when (it){
                TYPE_GOAL -> {
                    val goal = Goal(id=UUID.randomUUID().toString())
                    val day = (System.currentTimeMillis()+viewModel.local)/(24*60*60*1000)
                    goal.deadline += (viewModel.day.value!!-day)*24*60*60*1000L

                    val dialog = GoalDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("goal", goal)
                        putBoolean("isCreated", true)
                        putBoolean("isTemporal", true)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
                }

                TYPE_REMINDER -> {
                    val reminder = Reminder(id=UUID.randomUUID().toString())
                    val day = (System.currentTimeMillis()+viewModel.local)/(24*60*60*1000)
                    reminder.time += (viewModel.day.value!!-day)*24*60*60*1000L

                    val dialog = ReminderDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("reminder", reminder)
                        putBoolean("isCreated", true)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "ReminderDialog")
                }
                else -> throw IllegalArgumentException("Invalid type")
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.fab.state == DayFabMenu.STATE_OPENED) binding.fab.close()
                    else {
                        isEnabled = false
                        activity?.onBackPressed()
                    }
                }
            })

        binding.toolBar.setNavigationOnClickListener {
            viewModel.day.value = viewModel.day.value!!-1
        }
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.right -> {
                    viewModel.day.value = viewModel.day.value!!+1
                    true
                }
                else -> false
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val time = (System.currentTimeMillis()+viewModel.local)%(24*60*60*1000)-60*60*1000
        val ratio = time/(24*60*60*1000f)
        val scrollY = (binding.clock.getChildAt(0).height*ratio).toInt()
        ObjectAnimator.ofInt(binding.clock, "scrollY",  scrollY).setDuration(1000).start()
    }

    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }

    private fun setLoadingView(){
        binding.loadingView.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
    }


    private inner class Adapter(var data: List<Pair<Int, Any>>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        fun updateData(newData: List<Pair<Int, Any>>){
            val diffUtilCallback = CustomDiffUtil(data, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, true)

            data = newData
            diffResult.dispatchUpdatesTo(this)

            if (data.isEmpty()) setEmptyView() else setNullView()
        }

        override fun getItemCount() = data.size

        override fun getItemViewType(position: Int): Int {
            return data[position].first
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
            return when (viewType){
                TYPE_SECTION -> SectionHolder(DataBindingUtil.inflate(
                    layoutInflater, R.layout.item_recycler_view_action_section,
                    parent, false))
                DayViewModel.TYPE_GOAL -> GoalHolder(DataBindingUtil.inflate(
                    layoutInflater, R.layout.item_recycler_view_goal,
                    parent, false))
                DayViewModel.TYPE_REMINDER -> ReminderHolder(DataBindingUtil.inflate(
                    layoutInflater, R.layout.item_recycler_view_reminder,
                    parent, false))
                else -> throw IllegalArgumentException("Invalid type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            return when (getItemViewType(position)) {
                TYPE_SECTION -> (holder as SectionHolder)
                    .bind((data[position].second as DayViewModel.Section).data)
                DayViewModel.TYPE_GOAL -> (holder as GoalHolder).bind(data[position].second as Goal)
                DayViewModel.TYPE_REMINDER -> (holder as ReminderHolder).bind(data[position].second as Reminder)
                else -> throw IllegalArgumentException("Invalid type")
            }
        }

        /* Если холдер есть в selectedItems, то мы с ним ничего не делаем.
        Если же нет, то изменям прозрачность. */
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isNotEmpty())
                if (selectedItems.isNotEmpty())
                    if (holder.absoluteAdapterPosition in selectedItems) holder.itemView.alpha = 1f
                    else holder.itemView.alpha = 0.5f
                else holder.itemView.alpha = 1f
            else super.onBindViewHolder(holder, position, payloads)
        }
    }

    private inner class SectionHolder(val binding: ItemRecyclerViewActionSectionBinding):
        RecyclerView.ViewHolder(binding.root){
        private lateinit var data: List<Pair<ActionSchedule, ActionType?>>

        init {
            binding.recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = SectionAdapter(emptyList())
            }
        }

        fun bind(data: List<Pair<ActionSchedule, ActionType?>>){
            this.data = data.map { it.copy() }
            (binding.recyclerView.adapter as SectionAdapter).updateData(data)
        }
    }

    private inner class SectionAdapter(var data: List<Pair<ActionSchedule, ActionType?>>):
        RecyclerView.Adapter<ActionHolder>(){

        fun updateData(newData: List<Pair<ActionSchedule, ActionType?>>){
            data = newData
            notifyItemRangeChanged(0, data.size)
        }

        override fun getItemCount(): Int = data.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionHolder {
            return ActionHolder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_action,
                parent, false))
        }

        override fun onBindViewHolder(holder: ActionHolder, position: Int) {
            holder.bind(data[position])
            if (itemCount == 1) holder.itemView.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            else holder.itemView.layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        }
    }

    private inner class ActionHolder(private val binding: ItemRecyclerViewActionBinding):
        RecyclerView.ViewHolder(binding.root){
        private lateinit var actionSchedule: ActionSchedule
        private var actionType: ActionType? = null

        fun bind(item: Pair<ActionSchedule, ActionType?>) {
            this.actionSchedule = item.first
            this.actionType = item.second

            binding.time.text = (formatTime(actionSchedule.startTime, false, FormatStyle.SHORT, FORMAT_TIME) +
                    " - " + formatTime(actionSchedule.endTime, false, FormatStyle.SHORT, FORMAT_TIME))
            if (actionType == null) {
                binding.actionType = ActionType(id="", color=0, name="???")
                binding.invalid.visibility = View.VISIBLE
            } else {
                binding.invalid.visibility = View.GONE
                binding.actionType = actionType
            }
        }
    }

    private inner class GoalHolder(val binding: ItemRecyclerViewGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        lateinit var goal: Goal

        init {
            itemView.setOnClickListener { onClick() }
            binding.edit.setOnClickListener{ onClick() }
            binding.checkBox.setOnClickListener {
                goal.isAchieved = binding.checkBox.isChecked
                viewModel.updateGoal(goal)
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }

            binding.dragHandle.visibility = View.INVISIBLE
            binding.textView13.visibility = View.VISIBLE
            binding.deadlineTextView.visibility = View.VISIBLE

            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.constraintLayout)
            constraintSet.connect(R.id.imageView7, ConstraintSet.END,
                R.id.drag_handle, ConstraintSet.END)
            constraintSet.applyTo(binding.constraintLayout)
        }

        private fun onClick(){
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val dialog = GoalDialog()
                dialog.arguments = Bundle().apply {
                    putSerializable("goal", goal)
                    putBoolean("isCreated", false)
                    putBoolean("isTemporal", true)
                }
                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }

        fun bind(goal: Goal){
            this.goal = goal
            binding.goal = goal
            if (binding.checkBox.isChecked != goal.isAchieved) binding.checkBox.isChecked = goal.isAchieved
            binding.deadlineTextView.text = formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_TIME)
        }
    }

    private inner class ReminderHolder(val binding: ItemRecyclerViewReminderBinding):
        RecyclerView.ViewHolder(binding.root) {
        lateinit var reminder: Reminder

        init {
            itemView.setOnClickListener{ onClicked() }
            binding.edit.setOnClickListener{ onClicked() }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }

            binding.dragHandle.visibility = View.INVISIBLE

            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.constraintLayout)
            constraintSet.connect(R.id.imageView4, ConstraintSet.END,
                R.id.drag_handle, ConstraintSet.END)
            constraintSet.applyTo(binding.constraintLayout)
        }

        private fun onClicked(){
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val dialog = ReminderDialog()
                dialog.arguments = Bundle().apply {
                    putSerializable("reminder", reminder)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "ReminderDialog")
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }

        fun bind(reminder: Reminder) {
            this.reminder = reminder
            binding.reminder = reminder
            binding.timeTextView.text = (formatTime(reminder.time, true, FormatStyle.SHORT, FORMAT_TIME))
        }
    }

    // Будем хранить позиции выбранных холдеров.
    private val selectedItems: MutableList<Int> = mutableListOf()
    private var actionMode: ActionMode? = null

    private fun startActionMode(){
        actionMode = requireActivity().startActionMode(callback)
        actionMode?.title = "0"
    }

    private fun changeItem(position: Int){
        val index = selectedItems.indexOf(position)
        if (index == -1) selectedItems.add(position)
        else selectedItems.removeAt(index)

        binding.recyclerView.adapter?.notifyItemRangeChanged(0, viewModel.data.value!!.size, true)
        actionMode?.title = selectedItems.size.toString()

        if (selectedItems.size == 0) actionMode?.finish()
    }

    private val callback by lazy {
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.menu_action_bar, menu)
                menu?.findItem(R.id.up)?.isVisible = false
                binding.fab.hide()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.delete -> {
                        AlertDialog.Builder(context, R.style.Style_AlertDialog)
                            .setTitle(R.string.are_you_sure)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                // Нужно передать скопированное значение, из-за того, что
                                // после этот массив удалится, а функция выполняется
                                // в отдельном потоке.
                                viewModel.deleteItems(selectedItems.map { it })
                                Toast.makeText(requireContext(), R.string.text_toast_delete, Toast.LENGTH_SHORT).show()
                                actionMode?.finish()
                            }
                            .setNegativeButton(R.string.no){ _, _ ->
                                actionMode?.finish()
                            }
                            .setCancelable(false).create().show()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                actionMode = null
                selectedItems.clear()
                binding.recyclerView.adapter?.notifyItemRangeChanged(0, viewModel.data.value!!.size, false)
                binding.fab.show()
            }
        }
    }

    private class CustomDiffUtil(private val oldList: List<Pair<Int, Any>>,
                                 private val newList: List<Pair<Int, Any>>): DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return if (oldList[oldItemPosition].first == newList[newItemPosition].first)
                if (oldList[oldItemPosition].first != TYPE_SECTION)
                    (oldList[oldItemPosition].second as ID).id == (newList[newItemPosition].second as ID).id
                else true
            else false
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].second == newList[newItemPosition].second
        }
    }

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : UnionSimpleCallback(0, ItemTouchHelper.LEFT){

            override fun getMovementFlags(recyclerView: RecyclerView,
                                          viewHolder: RecyclerView.ViewHolder): Int {
                return if (viewHolder is SectionHolder) 0
                else super.getMovementFlags(recyclerView, viewHolder)
            }
        }

        simpleItemTouchCallback.backgroundRight = ColorDrawable(Color.parseColor("#CA0000"))
        simpleItemTouchCallback.iconRight = ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_baseline_delete_24)?.apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }

        simpleItemTouchCallback.setSwipeItemListener(object : UnionSimpleCallback.SwipeListener{
            override fun swipeRight(position: Int) {}

            override fun swipeLeft(position: Int) {
                AlertDialog.Builder(context, R.style.Style_AlertDialog)
                    .setTitle(R.string.are_you_sure)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteItem(position)
                        Toast.makeText(requireContext(), R.string.text_toast_delete, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.no){ _, _ -> }
                    .setCancelable(false).create().show()
            }
        })

        ItemTouchHelper(simpleItemTouchCallback)
    }
}