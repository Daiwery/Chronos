/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 23.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавление логики взаимодействия с DaySchedule.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.TYPE_DAY_SCHEDULE_ABSOLUTE
import com.daiwerystudio.chronos.database.TYPE_DAY_SCHEDULE_RELATIVE
import com.daiwerystudio.chronos.databinding.FragmentDayScheduleBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionScheduleBinding
import com.daiwerystudio.chronos.ui.union.CustomDiffUtil
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat


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
//        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.daySchedule.observe(viewLifecycleOwner, {
            if (it.type != TYPE_DAY_SCHEDULE_RELATIVE) binding.setStartDayTime.visibility = View.GONE
            else binding.setStartDayTime.visibility = View.VISIBLE
        })

        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).setData(it)
//            binding.loadingClock.visibility = View.VISIBLE
//            binding.clock.setActionsSchedule(it, startDayTime)
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
            viewModel.daySchedule.value!!.type =
                if (viewModel.daySchedule.value!!.type == TYPE_DAY_SCHEDULE_RELATIVE) TYPE_DAY_SCHEDULE_ABSOLUTE
                else TYPE_DAY_SCHEDULE_RELATIVE
            viewModel.updateDaySchedule()
        }

        binding.fab.setOnClickListener {
            val actionSchedule = ActionSchedule(dayID=viewModel.dayScheduleID.value!!,
                indexList=viewModel.actionsSchedule.value!!.size)

            when (viewModel.daySchedule.value!!.type) {
                TYPE_DAY_SCHEDULE_RELATIVE -> {
                    actionSchedule.startAfter = 30*60*1000
                    actionSchedule.duration = 90*60*1000
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

        //        binding.clock.setFinishedListener { binding.loadingClock.visibility = View.GONE }
//        binding.clock.setCorruptedListener(object : ScheduleClockView.CorruptedListener {
//            override fun addCorrupt(id: String) {
//                val position = viewModel.actionsSchedule.value!!.indexOfFirst { it.id == id }
//                // После обновления это не уходит в бесконечный цикл, так как
//                // есть DiffUtil в ScheduleView.
//                if (position != -1){
//                    viewModel.actionsSchedule.value!![position].isCorrupted = true
//                    viewModel.updateActionSchedule(viewModel.actionsSchedule.value!![position])
//                }
//
//                if (!schedule.isCorrupted) {
//                    schedule.isCorrupted = true
//                    viewModel.updateSchedule(schedule)
//                }
//            }
//
//            override fun deleteCorrupt(id: String, countCorrupted: Int) {
//                val position = viewModel.actionsSchedule.value!!.indexOfFirst { it.id == id }
//                // После обновления это не уходит в бесконечный цикл, так как
//                // есть DiffUtil в ScheduleView.
//                if (position != -1){
//                    viewModel.actionsSchedule.value!![position].isCorrupted = false
//                    viewModel.updateActionSchedule(viewModel.actionsSchedule.value!![position])
//                }
//
//                if (countCorrupted == 0){
//                    schedule.isCorrupted = false
//                    viewModel.updateSchedule(schedule)
//                }
//            }
//        })

        return binding.root
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

             val actionType = viewModel.getActionType(actionSchedule.actionTypeId)
             actionType.observe(viewLifecycleOwner, {
                 if (it == null) {
                     binding.actionType = ActionType(id="", color=0, name="???")
                     binding.invalid.visibility = View.VISIBLE
                 }
                 else {
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
 }