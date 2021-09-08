/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 23.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавление логики взаимодействия с DaySchedule.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.daiwerystudio.chronos.database.TYPE_DAY_SCHEDULE_ABSOLUTE
import com.daiwerystudio.chronos.database.TYPE_DAY_SCHEDULE_RELATIVE
import com.daiwerystudio.chronos.databinding.FragmentDayScheduleBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionScheduleBinding
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.union.CustomDiffUtil
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.daiwerystudio.chronos.ui.widgets.ScheduleClockView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.format.FormatStyle


class DayScheduleFragment : Fragment() {
    private val viewModel: DayScheduleViewModel
            by lazy { ViewModelProvider(this).get(DayScheduleViewModel::class.java) }
    private lateinit var binding: FragmentDayScheduleBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.dayScheduleID.value = arguments?.getString("dayScheduleID")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDayScheduleBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.daySchedule.observe(viewLifecycleOwner, {
            if (it.type != TYPE_DAY_SCHEDULE_RELATIVE) {
                binding.setStartDayTime.visibility = View.GONE
                binding.clock.setStartTime(0)
            }
            else {
                binding.setStartDayTime.visibility = View.VISIBLE
                binding.clock.setStartTime(it.startDayTime)
            }
        })

        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).setData(it)
            binding.loadingClock.visibility = View.VISIBLE
            if (viewModel.daySchedule.value!!.type != TYPE_DAY_SCHEDULE_RELATIVE)
                binding.clock.setActionsSchedule(it, null)
            else  binding.clock.setActionsSchedule(it, viewModel.daySchedule.value!!.startDayTime)
        })

        binding.setStartDayTime.setOnClickListener {
            val startDayTime = viewModel.daySchedule.value!!.startDayTime.toInt()
            val hour = startDayTime/(1000*60*60)
            val minute = (startDayTime-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                viewModel.daySchedule.value!!.startDayTime = (dialog.hour*60+dialog.minute)*60L*1000
                viewModel.updateDaySchedule()
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.setType.setOnClickListener {
            val type = if (viewModel.daySchedule.value!!.type == TYPE_DAY_SCHEDULE_RELATIVE) TYPE_DAY_SCHEDULE_ABSOLUTE
            else TYPE_DAY_SCHEDULE_RELATIVE
            viewModel.setTypeDaySchedule(type)
        }

        binding.fab.setOnClickListener {
            val actionSchedule = ActionSchedule(dayID=viewModel.dayScheduleID.value!!,
                indexList=viewModel.actionsSchedule.value!!.size)

            when (viewModel.daySchedule.value!!.type) {
                TYPE_DAY_SCHEDULE_RELATIVE -> {
                    actionSchedule.startAfter = 30*60*1000L
                    actionSchedule.duration = 90*60*1000L
                }
                TYPE_DAY_SCHEDULE_ABSOLUTE -> {
                    val actionsSchedule = viewModel.actionsSchedule.value!!

                    if (actionsSchedule.isNotEmpty()) actionSchedule.startTime =
                        (actionsSchedule[actionsSchedule.size-1].endTime+30*60*1000)%(25*60*60*1000)
                    else actionSchedule.startTime = 0

                    actionSchedule.endTime = (actionSchedule.startTime+90*60*1000)%(25*60*60*1000)
                }
                else -> throw IllegalStateException("Invalid type")
            }


            val dialog = ActionScheduleDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("actionSchedule", actionSchedule)
                putInt("type", viewModel.daySchedule.value!!.type)
                putBoolean("isCreated", true)
                putString("parentID", viewModel.daySchedule.value!!.scheduleID)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
        }

        binding.clock.setFinishedListener { binding.loadingClock.visibility = View.GONE }
        binding.clock.setCorruptedListener(object : ScheduleClockView.CorruptedListener {
            override fun addCorrupt(id: String) {
                val position = viewModel.actionsSchedule.value!!.indexOfFirst { it.id == id }
                if (position != -1){
                    viewModel.actionsSchedule.value!![position].isCorrupted = true
                    // Сразу обновим адаптер.
                    (binding.recyclerView.adapter as Adapter).setData(viewModel.actionsSchedule.value!!)
                    // После обновления это не уходит в бесконечный цикл, так как
                    // есть DiffUtil в ScheduleView.
                    viewModel.updateActionSchedule(viewModel.actionsSchedule.value!![position])
                }

                if (!viewModel.daySchedule.value!!.isCorrupted) {
                    viewModel.daySchedule.value!!.isCorrupted = true
                    viewModel.updateDaySchedule()
                }
            }

            override fun deleteCorrupt(id: String, countCorrupted: Int) {
                val position = viewModel.actionsSchedule.value!!.indexOfFirst { it.id == id }
                if (position != -1){
                    viewModel.actionsSchedule.value!![position].isCorrupted = false
                    // Сразу обновим адаптер.
                    (binding.recyclerView.adapter as Adapter).setData(viewModel.actionsSchedule.value!!)
                    // После обновления это не уходит в бесконечный цикл, так как
                    // есть DiffUtil в ScheduleView.
                    viewModel.updateActionSchedule(viewModel.actionsSchedule.value!![position])
                }

                if (countCorrupted == 0){
                    viewModel.daySchedule.value!!.isCorrupted = false
                    viewModel.updateDaySchedule()
                }
            }
        })

        return binding.root
    }

    override fun onPause() {
        super.onPause()

        // При выходе из фрагмента нам нужно сохранить значение startTime и endTime,
        // так как они будут использоваться в составлении расписании на день.
        viewModel.updateActionsSchedule()
    }

    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    private inner class Holder(private val binding: ItemRecyclerViewActionScheduleBinding):
        RecyclerView.ViewHolder(binding.root){
        private lateinit var actionSchedule: ActionSchedule

        init {
            itemView.setOnClickListener {
                val dialog = ActionScheduleDialog()
                dialog.arguments = Bundle().apply {
                    putSerializable("actionSchedule", actionSchedule)
                    putInt("type", viewModel.daySchedule.value!!.type)
                    putBoolean("isCreated", false)
                    putString("parentID", viewModel.daySchedule.value!!.scheduleID)
                }
                dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
            }
        }

        fun bind(actionSchedule: ActionSchedule) {
            this.actionSchedule = actionSchedule
            binding.actionSchedule = actionSchedule
            binding.start.text = formatTime(actionSchedule.startTime, false, FormatStyle.SHORT, FORMAT_TIME)
            binding.end.text = formatTime(actionSchedule.endTime, false, FormatStyle.SHORT, FORMAT_TIME)

            val actionType = viewModel.getActionType(actionSchedule.actionTypeId)
            actionType.observe(viewLifecycleOwner, {
                if (it == null) {
                    binding.actionType = ActionType(id="", color=0, name="???")
                    binding.invalid.visibility = View.VISIBLE
                } else {
                    binding.invalid.visibility = View.GONE
                    binding.actionType = it
                }
            })
        }
    }

    private inner class Adapter(var actionsSchedule: List<ActionSchedule>): RecyclerView.Adapter<Holder>(){
        fun setData(newData: List<ActionSchedule>){
             val diffUtilCallback = CustomDiffUtil(actionsSchedule, newData)
             val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

             actionsSchedule = newData.map { it.copy() }
             diffResult.dispatchUpdatesTo(this)

             if (actionsSchedule.isEmpty()) setEmptyView()
             else setNullView()
        }

        override fun getItemCount() = actionsSchedule.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
            return Holder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_action_schedule,
                    parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(actionsSchedule[position])
        }
    }


    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT){

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            return false
        }


        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                viewModel.deleteActionSchedule(viewModel.actionsSchedule.value!![viewHolder.absoluteAdapterPosition])
        }

        /**
         * Иконка, которую рисует onChildDraw.
         */
        var icon: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_delete_24_white)

        /**
         * Задний фон, который рисует onChildDraw.
         */
        var background: Drawable? = ColorDrawable(Color.parseColor("#CA0000"))

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                 dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                if (dX < 0) {
                    val itemView = viewHolder.itemView
                    background?.setBounds(
                        itemView.left + viewHolder.itemView.width/100,
                        itemView.top, itemView.right, itemView.bottom
                    )

                    icon?.also {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                } else {
                    icon?.setBounds(0, 0, 0, 0)
                    background?.setBounds(0, 0, 0, 0)
                }
            }
            background?.draw(c)
            icon?.draw(c)
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }
 }