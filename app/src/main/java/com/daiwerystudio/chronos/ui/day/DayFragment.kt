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
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import com.daiwerystudio.chronos.ui.union.*
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
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.day.observe(viewLifecycleOwner, {
            binding.toolBar.title =
                LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        })

        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            binding.loadingClock.visibility = View.VISIBLE
            binding.clock.setActionsSchedule(it)
        })

        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).updateData(it)

            binding.clock.setGoalsTimes(it.filter { item -> item.first == TYPE_GOAL }
                .map{ item -> ((item.second as Goal).deadline+viewModel.local)%(24*60*60*1000) })
            binding.clock.setRemindersTimes(it.filter { item -> item.first == TYPE_REMINDER }
                .map{ item -> ((item.second as Reminder).time+viewModel.local)%(24*60*60*1000) })
        })

        binding.toolBar.setOnClickListener {
            if (binding.motionLayout.progress > 0.5) binding.motionLayout.transitionToStart()
            else binding.motionLayout.transitionToEnd()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.day.value = LocalDate.of(year, month+1, dayOfMonth).toEpochDay()
        }

        binding.clock.setFinishedListener{ binding.loadingClock.visibility = View.GONE }
        binding.clock.setClickSectionListener{}
        binding.clock.setCountSectionsListener{}
        binding.clock.setMustActionTypeListener{}

        binding.fab.setOnClickListener{
            val popup = PopupMenu(context, it)
            popup.menuInflater.inflate(R.menu.menu_create_day_item, popup.menu)
            popup.setOnMenuItemClickListener (object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem?): Boolean {
                    when (item?.itemId){
                        R.id.create_goal -> {
                            val id = UUID.randomUUID().toString()
                            val goal = Goal(id=id)
                            goal.deadline = System.currentTimeMillis()

                            val dialog = GoalDialog()
                            dialog.arguments = Bundle().apply{
                                putSerializable("goal", goal)
                                putBoolean("isCreated", true)
                            }
                            dialog.show(requireActivity().supportFragmentManager, "GoalDialog")

                            return true
                        }

                        R.id.create_reminder -> {
                            val id = UUID.randomUUID().toString()
                            val reminder = Reminder(id=id)

                            val dialog = ReminderDialog()
                            dialog.arguments = Bundle().apply{
                                putSerializable("reminder", reminder)
                                putBoolean("isCreated", true)
                            }
                            dialog.show(requireActivity().supportFragmentManager, "ReminderDialog")

                            return true
                        }
                        else -> return false
                    }
                }
            })
            popup.show()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // Мы не можем изначально поставить размер appBarLayout равный ?attr/actionBarSize
        // или выполнить код ниже в функции выше, так как при этом у CalendarView не будет срабатывать
        // onClickListener.
//        val position = viewModel.day.value!!-(System.currentTimeMillis()+viewModel.local)/(1000*60*60*24)
//        if (position == 0L) binding.motionLayout.transitionToEnd()

        // В функции выше делать нельзя, так как height там пока что равно 0.
        val currentTime = (System.currentTimeMillis()+viewModel.local)%(24*60*60*1000)-60*60*1000
        val ratio = currentTime/(24*60*60*1000f)
        val scrollY = (binding.clock.getChildAt(0).height*ratio).toInt()
        binding.clock.scrollY = scrollY
    }


    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    inner class GoalHolder(binding: ItemRecyclerViewGoalBinding) :
        GoalAbstractHolder(binding, requireActivity().supportFragmentManager){

        override fun setStaticUI(goal: Goal) {
            super.setStaticUI(goal)
            // Делаем невидимым прогресс бар.
            binding.isAchieved = true
        }

        override fun setDeadline() {
            binding.deadlineTextView.text = formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_TIME)
        }

        override fun onAchieved() {
            goal.isAchieved = binding.checkBox.isChecked
            viewModel.updateGoal(goal)
        }

        override fun setPercentAchieved() {}
        override fun onClicked() {
            val dialog = GoalDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("goal", goal)
                putBoolean("isTemporal", true)
                putBoolean("isCreated", false)
            }
            dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
        }
    }

    inner class ReminderHolder(binding: ItemRecyclerViewReminderBinding):
        ReminderAbstractHolder(binding, requireActivity().supportFragmentManager){
            override fun setTime() {
                binding.timeTextView.text = formatTime(reminder.time, true, FormatStyle.SHORT, FORMAT_TIME)
            }
        }

    private inner class Adapter(var data: List<Pair<Int, ID>>): RecyclerView.Adapter<RawHolder>(){
        fun updateData(newData: List<Pair<Int, ID>>){
            val diffUtilCallback = CustomDiffUtil(data.map { it.second }, newData.map { it.second })
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            data = newData.map{ it.copy() }
            diffResult.dispatchUpdatesTo(this)

            if (data.isEmpty()) setEmptyView()
            else setNullView()
        }

        override fun getItemCount() = data.size

        override fun getItemViewType(position: Int): Int = data[position].first

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RawHolder {
            return when(viewType){
                TYPE_GOAL -> GoalHolder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_goal,
                    parent, false))
                TYPE_REMINDER -> ReminderHolder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_reminder,
                    parent, false))
                else -> throw IllegalArgumentException("Invalid type")
            }
        }

        override fun onBindViewHolder(holder: RawHolder, position: Int) {
            holder.bind(data[position].second)
        }
    }
}