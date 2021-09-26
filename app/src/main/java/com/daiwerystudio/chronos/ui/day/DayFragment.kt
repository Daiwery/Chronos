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

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.Reminder
import com.daiwerystudio.chronos.databinding.*
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
        binding.motionLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
        }

        viewModel.day.observe(viewLifecycleOwner, {
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
                ObjectAnimator.ofFloat(binding.imageView9, "rotation",  0f)
                    .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
            }
            else {
                binding.calendarView.visibility = View.VISIBLE
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
                    ObjectAnimator.ofFloat(binding.imageView9, "rotation",  0f)
                        .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
                }
            }
        })

        binding.calendarView.visibility = View.GONE
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.day.value = LocalDate.of(year, month+1, dayOfMonth).toEpochDay()
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
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

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
                DayViewModel.TYPE_SECTION -> SectionHolder(DataBindingUtil.inflate(
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
                DayViewModel.TYPE_SECTION -> (holder as SectionHolder)
                    .bind((data[position].second as DayViewModel.Section).data)
                DayViewModel.TYPE_GOAL -> (holder as GoalHolder).bind(data[position].second as Goal)
                DayViewModel.TYPE_REMINDER -> (holder as ReminderHolder).bind(data[position].second as Reminder)
                else -> throw IllegalArgumentException("Invalid type")
            }
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
            binding.edit.setOnClickListener{
                val dialog = GoalDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("goal", goal)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
            }
            binding.checkBox.setOnClickListener {
                goal.isAchieved = binding.checkBox.isChecked
                viewModel.updateGoal(goal)
            }

            // Делаем невидимым прогресс бар.
            binding.dragHandle.visibility = View.GONE
            binding.textView21.visibility = View.GONE
            binding.progressTextView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
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

            binding.dragHandle.visibility = View.GONE
        }

        private fun onClicked(){
            val dialog = ReminderDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("reminder", reminder)
                putBoolean("isCreated", false)
            }
            dialog.show(requireActivity().supportFragmentManager, "ReminderDialog")
        }

        fun bind(reminder: Reminder) {
            this.reminder = reminder
            binding.reminder = reminder
            binding.timeTextView.text = (formatTime(reminder.time, true, FormatStyle.SHORT, FORMAT_TIME))
        }
    }

    private class CustomDiffUtil(private val oldList: List<Pair<Int, Any>>,
                                 private val newList: List<Pair<Int, Any>>): DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].first == newList[newItemPosition].first
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].second == newList[newItemPosition].second
        }
    }
}