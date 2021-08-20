/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 20.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: модификация без особых изменений логики.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.database.DaySchedule
import com.daiwerystudio.chronos.databinding.FragmentDayScheduleBinding


class DayScheduleFragment : Fragment() {
    private val viewModel: DayScheduleViewModel
            by lazy { ViewModelProvider(this).get(DayScheduleViewModel::class.java) }
    private lateinit var binding: FragmentDayScheduleBinding
    private lateinit var daySchedule: DaySchedule


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        daySchedule = arguments?.getSerializable("daySchedule") as DaySchedule
        viewModel.getActionsSchedule(daySchedule)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDayScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

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


//        if (schedule.type != TYPE_SCHEDULE_RELATIVE) {
//            binding.clock.setStartTime(0)
//            binding.set.visibility = View.GONE
//        } else {
//            binding.clock.setStartTime(startDayTime)
//            binding.set.visibility = View.VISIBLE
//        }


//        binding.set.setOnClickListener {
//            val hour = startDayTime.toInt() / 3600
//            val minute = (startDayTime.toInt() - hour * 3600) / 60
//
//            val dialog = MaterialTimePicker.Builder()
//                .setTimeFormat(TimeFormat.CLOCK_24H)
//                .setHour(hour)
//                .setMinute(minute)
//                .setTitleText("")
//                .build()
//
//            dialog.addOnPositiveButtonClickListener {
//                binding.loadingClock.visibility = View.VISIBLE
//
//                startDayTime = (dialog.hour * 60 + dialog.minute)*60L
//                val editor = preferences.edit()
//                editor.putLong("startDayTime", startDayTime).apply()
//
//                binding.clock.setStartTime(startDayTime)
//                updateUI(viewModel.actionsSchedule.value!!)
//            }
//            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
//        }

//
//        binding.recyclerView.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = Adapter(emptyList())
//            //itemAnimator = ItemAnimator()
//        }
//        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
//
//
//        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
//            updateUI(it)
//        })


//        binding.fab.setOnClickListener {
//            val actionSchedule = ActionSchedule(
//                scheduleID = schedule.id, dayIndex = dayIndex,
//                indexList = viewModel.actionsSchedule.value?.size!!
//            )
//
//            when (schedule.type) {
//                TYPE_SCHEDULE_RELATIVE -> {
//                    actionSchedule.startAfter = 30 * 60
//                    actionSchedule.duration = 90 * 60
//                }
//                TYPE_SCHEDULE_ABSOLUTE -> {
//                    val actionsSchedule = viewModel.actionsSchedule.value!!
//
//                    if (actionsSchedule.isNotEmpty()) actionSchedule.startTime =
//                        (actionsSchedule[actionsSchedule.size - 1].endTime + 30 * 60)
//                    else actionSchedule.startTime = 0
//
//                    actionSchedule.endTime = (actionSchedule.startTime + 90 * 60)
//                }
//                else -> throw IllegalStateException("Invalid type")
//            }
//
//
//            val dialog = ActionScheduleDialog()
//            dialog.arguments = Bundle().apply {
//                putSerializable("actionSchedule", actionSchedule)
//                putInt("type", schedule.type)
//                putBoolean("isCreated", true)
//            }
//            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
//        }

        return view
    }

//    private fun updateUI(it: List<ActionSchedule>){
//        binding.loadingClock.visibility = View.VISIBLE
//
//        updateStartEndTimes(it)
//
//        val startDayTime = if (schedule.type == TYPE_SCHEDULE_RELATIVE) startDayTime else null
//        binding.clock.setActionsSchedule(it, startDayTime)
//
//        // Нельзя создавать новый адаптер, так как используется DiffUtil
//        // для нахождения оптимизированных изменений данных.
//        (binding.recyclerView.adapter as Adapter).setData(it)
//     }

//     private fun updateStartEndTimes(actionsSchedule: List<ActionSchedule>){
//         if (schedule.type == TYPE_SCHEDULE_RELATIVE){
//             actionsSchedule.forEachIndexed { i, actionSchedule ->
//                 var start = actionSchedule.startAfter
//                 start += if (i != 0) actionsSchedule[i-1].endTime
//                 else startDayTime
//
//                 actionsSchedule[i].startTime = start
//                 actionsSchedule[i].endTime = start+actionSchedule.duration
//             }
//         }
//     }

//     private fun setEmptyView(){
//         binding.loadingView.visibility = View.GONE
//         binding.emptyView.visibility = View.VISIBLE
//     }
//
//     private fun setNullView(){
//         binding.loadingView.visibility = View.GONE
//         binding.emptyView.visibility = View.GONE
//     }


//    private inner class Holder(private val binding: ItemRecyclerViewActionScheduleBinding):
//        RecyclerView.ViewHolder(binding.root){
//        private lateinit var actionSchedule: ActionSchedule
//
//        init {
//            itemView.setOnClickListener(this)
//        }

//        fun bind(actionSchedule: ActionSchedule) {
//             this.actionSchedule = actionSchedule
//             binding.actionSchedule = actionSchedule
//
//             val actionType = viewModel.getActionType(actionSchedule.actionTypeId)
//             actionType.observe(viewLifecycleOwner, {
//                 if (it == null) {
//                     binding.actionType = ActionType(id=UUID.randomUUID().toString(), color=0, name="???")
//                     binding.invalid.visibility = View.VISIBLE
//                 }
//                 else {
//                     binding.invalid.visibility = View.GONE
//                     binding.actionType = it
//                 }
//             })
//        }

//        override fun onClick(v: View) {
//            val dialog = ActionScheduleDialog()
//            dialog.arguments = Bundle().apply {
//                putSerializable("actionSchedule", actionSchedule)
//                putInt("type", schedule.type)
//                putBoolean("isCreated", false)
//            }
//            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
//        }
//    }

//    private inner class Adapter(var actionsSchedule: List<ActionSchedule>): RecyclerView.Adapter<Holder>(){
//        private var lastPosition = -1
//
//        fun setData(newData: List<ActionSchedule>){
//             val diffUtilCallback = DiffUtilCallback(actionsSchedule, newData)
//             val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
//
//             // Copy data.
//             actionsSchedule = newData.map { it.copy() }
//             diffResult.dispatchUpdatesTo(this)
//
//             if (actionsSchedule.isEmpty())setEmptyView()
//             else setNullView()
//        }
//
//         /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
//         override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
//            return Holder(DataBindingUtil.inflate(layoutInflater,
//                    R.layout.item_recycler_view_action_schedule,
//                    parent, false))
//        }
//
//         override fun getItemCount() = actionsSchedule.size
//
//        override fun onBindViewHolder(holder: Holder, position: Int) {
//            holder.bind(actionsSchedule[position])
//
//            // Animation
//            if (holder.adapterPosition > lastPosition){
//                lastPosition = holder.adapterPosition
//
//                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_add_item)
//                holder.itemView.startAnimation(animation)
//            }
//        }
//    }
//
//     private class DiffUtilCallback(private val oldList: List<ActionSchedule>,
//                                    private val newList: List<ActionSchedule>): DiffUtil.Callback() {
//
//         override fun getOldListSize() = oldList.size
//
//         override fun getNewListSize() = newList.size
//
//         override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
//             return oldList[oldPosition].id == newList[newPosition].id
//         }
//
//         override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
//             return oldList[oldPosition] == newList[newPosition]
//         }
//     }
//
//    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
//        CustomItemTouchCallback(requireContext(),
//            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
//            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
//        private val mAdapter = binding.recyclerView.adapter!!
//
//        override fun onMove(recyclerView: RecyclerView,
//                            viewHolder: RecyclerView.ViewHolder,
//                            target: RecyclerView.ViewHolder): Boolean {
//            val from = viewHolder.adapterPosition
//            val to = target.adapterPosition
//
//            viewModel.actionsSchedule.value!![from].indexList = to
//            viewModel.actionsSchedule.value!![to].indexList = from
//
//            Collections.swap(viewModel.actionsSchedule.value!!, from, to)
//            recyclerView.adapter?.notifyItemMoved(from, to)
//
//            return true
//        }
//
//        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder) {
//            viewModel.deleteActionSchedule(viewModel.actionsSchedule.value!![viewHolder.adapterPosition])
//        }
//
//        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder) {
//            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
//        }
//
//        }
//
//        ItemTouchHelper(simpleItemTouchCallback)
//    }
//
//     override fun onPause() {
//         viewModel.updateListActionSchedule(viewModel.actionsSchedule.value!!)
//
//         super.onPause()
//     }


 }