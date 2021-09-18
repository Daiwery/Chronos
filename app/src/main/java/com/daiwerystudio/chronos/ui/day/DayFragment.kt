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
*/

package com.daiwerystudio.chronos.ui.day

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.Reminder
import com.daiwerystudio.chronos.database.TYPE_GOAL
import com.daiwerystudio.chronos.database.TYPE_REMINDER
import com.daiwerystudio.chronos.databinding.FragmentDayBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewReminderBinding
import com.daiwerystudio.chronos.ui.FORMAT_DAY
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import com.daiwerystudio.chronos.ui.union.*
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class DayFragment: Fragment() {
    private val viewModel: DayViewModel
        by lazy { ViewModelProvider(this).get(DayViewModel::class.java) }
    private lateinit var binding: FragmentDayBinding
    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = UnionSimpleCallback(0, ItemTouchHelper.LEFT )
        simpleItemTouchCallback.backgroundRight = ColorDrawable(Color.parseColor("#CA0000"))
        simpleItemTouchCallback.iconRight = ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_baseline_delete_24)?.apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        simpleItemTouchCallback.setSwipeItemListener(object : UnionSimpleCallback.SwipeListener{
            override fun swipeLeft(position: Int) {
                AlertDialog.Builder(context, R.style.Style_AlertDialog)
                    .setTitle(R.string.are_you_sure)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteItem(position)
                    }
                    .setNegativeButton(R.string.no){ _, _ -> }
                    .setCancelable(false).create().show()
            }

            override fun swipeRight(position: Int) {}
        })

        ItemTouchHelper(simpleItemTouchCallback)
    }


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
//            adapter = Adapter(emptyList())
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) binding.fab.show()
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) binding.fab.hide()
            }
        })

        viewModel.day.observe(viewLifecycleOwner, {
            binding.toolBar.title =
                LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        })

        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            binding.loadingClock.visibility = View.VISIBLE
            binding.clock.setActionsSchedule(it)
        })

        viewModel.data.observe(viewLifecycleOwner, {
//            (binding.recyclerView.adapter as Adapter).updateData(it)

            binding.clock.setGoalsTimes(it.filter { item -> item.first == TYPE_GOAL }
                .map{ item -> ((item.second as Goal).deadline+viewModel.local)%(24*60*60*1000) })
            binding.clock.setRemindersTimes(it.filter { item -> item.first == TYPE_REMINDER }
                .map{ item -> ((item.second as Reminder).time+viewModel.local)%(24*60*60*1000) })
        })

        binding.toolBar.setOnClickListener {
            if (binding.calendarView.visibility == View.VISIBLE) {
                binding.calendarView.visibility = View.GONE
                binding.imageView9.rotation = 0f
            }
            else {
                binding.calendarView.visibility = View.VISIBLE
                binding.imageView9.rotation = 90f
            }
        }

        // По какой-то причине, если календарь инициализируется невидимым,
        // то его размер становится равен 0.
        binding.calendarView.visibility = View.GONE
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.day.value = LocalDate.of(year, month+1, dayOfMonth).toEpochDay()
        }

        binding.clock.setFinishedListener{ binding.loadingClock.visibility = View.GONE }
        binding.clock.setMustActionTypeListener{}

        binding.fab.setOnClickListener{
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


//    inner class GoalHolder(binding: ItemRecyclerViewGoalBinding) :
//        GoalAbstractHolder(binding, requireActivity().supportFragmentManager){
//
//        override fun setStaticUI(goal: Goal) {
//            super.setStaticUI(goal)
//            // Делаем невидимым прогресс бар.
//            binding.textView21.visibility = View.GONE
//            binding.progressTextView.visibility = View.GONE
//            binding.progressBar.visibility = View.GONE
//        }
//
//        override fun setDeadline() {
//            binding.deadlineTextView.text = formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_TIME)
//        }
//
//        override fun onAchieved() {
//            goal.isAchieved = binding.checkBox.isChecked
//            viewModel.updateGoal(goal)
//        }
//
//        override fun setPercentAchieved() {}
//        override fun onClicked() {
//            val dialog = GoalDialog()
//            dialog.arguments = Bundle().apply{
//                putSerializable("goal", goal)
//                putBoolean("isTemporal", true)
//                putBoolean("isCreated", false)
//            }
//            dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
//        }
//    }
//
//    inner class ReminderHolder(binding: ItemRecyclerViewReminderBinding):
//        ReminderAbstractHolder(binding, requireActivity().supportFragmentManager){
//            override fun setTime() {
//                binding.timeTextView.text = formatTime(reminder.time, true, FormatStyle.SHORT, FORMAT_TIME)
//            }
//        }
//
//    private inner class Adapter(var data: List<Pair<Int, ID>>): RecyclerView.Adapter<RawHolder>(){
//        fun updateData(newData: List<Pair<Int, ID>>){
//            val diffUtilCallback = UnionDiffUtil(data.map { it.second }, newData.map { it.second })
//            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
//
//            data = newData.map{ it.copy() }
//            diffResult.dispatchUpdatesTo(this)
//
//            if (data.isEmpty()) setEmptyView()
//            else setNullView()
//        }
//
//        override fun getItemCount() = data.size
//
//        override fun getItemViewType(position: Int): Int = data[position].first
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RawHolder {
//            return when(viewType){
//                TYPE_GOAL -> GoalHolder(DataBindingUtil.inflate(layoutInflater,
//                    R.layout.item_recycler_view_goal,
//                    parent, false))
//                TYPE_REMINDER -> ReminderHolder(DataBindingUtil.inflate(layoutInflater,
//                    R.layout.item_recycler_view_reminder,
//                    parent, false))
//                else -> throw IllegalArgumentException("Invalid type")
//            }
//        }
//
//        override fun onBindViewHolder(holder: RawHolder, position: Int) {
//            holder.bind(data[position].second)
//        }
//    }
}