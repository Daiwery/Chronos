 package com.daiwerystudio.chronos.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.TYPE_SCHEDULE_ABSOLUTE
import com.daiwerystudio.chronos.database.TYPE_SCHEDULE_RELATIVE
import com.daiwerystudio.chronos.databinding.FragmentDayScheduleBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionScheduleBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator
import com.daiwerystudio.chronos.ui.ScheduleClockView
import java.lang.IllegalStateException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*


 class DayScheduleFragment : Fragment() {
     // ViewModel
     private val viewModel: DayScheduleViewModel
     by lazy { ViewModelProvider(this).get(DayScheduleViewModel::class.java) }
     // Data Binding
     private lateinit var binding: FragmentDayScheduleBinding
     // Arguments
     private lateinit var schedule: Schedule
     private var dayIndex: Int = 0


     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)

         schedule = arguments?.getSerializable("schedule") as Schedule
         dayIndex = arguments?.getInt("dayIndex") as Int

         viewModel.getActionsSchedule(schedule, dayIndex)
     }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentDayScheduleBinding.inflate(inflater, container, false)
        val view = binding.root
        setLoadingView()


        /*  Setting clock  */
        binding.clock.setFinishedListener(object : ScheduleClockView.FinishedListener{
            override fun finish() {
                binding.loadingClock.visibility = View.GONE
            }
        })
        binding.clock.setCorruptedListener(object : ScheduleClockView.CorruptedListener{
            override fun addCorrupt(id: String) {
                val position = viewModel.actionsSchedule.value!!.indexOfFirst{ it.id == id }
                binding.recyclerView.adapter?.notifyItemChanged(position, true)
            }

            override fun deleteCorrupt(id: String) {
                val position = viewModel.actionsSchedule.value!!.indexOfFirst{ it.id == id }
                binding.recyclerView.adapter?.notifyItemChanged(position, false)
            }
        })

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        // Observation
        viewModel.actionsSchedule.observe(viewLifecycleOwner, { actionsSchedule ->
            setLoadingView()
            // Update
            viewModel.updateStartEndTimes(schedule, actionsSchedule)
            // Clock
            if ((binding.recyclerView.adapter as Adapter).actionsSchedule != actionsSchedule){
                binding.loadingClock.visibility = View.VISIBLE
                binding.clock.setActionsSchedule(actionsSchedule, false)
            }
            // RecyclerView
            (binding.recyclerView.adapter as Adapter).setData(actionsSchedule)

        })
        // Support move and swipe.
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        // Setting fab
        binding.fab.setOnClickListener{
            // ActionSchedule
            val actionSchedule = ActionSchedule(scheduleId=schedule.id, dayIndex=dayIndex,
                indexList=viewModel.actionsSchedule.value?.size!!)

            when (schedule.type){
                TYPE_SCHEDULE_RELATIVE -> {
                    actionSchedule.startAfter = 30*60
                    actionSchedule.duration = 90*60
                }
                TYPE_SCHEDULE_ABSOLUTE -> {
                    val actionsSchedule = viewModel.actionsSchedule.value!!
                    if (actionsSchedule.isNotEmpty()) actionSchedule.startTime = (actionsSchedule[actionsSchedule.size-1].endTime+30*60)
                    else actionSchedule.startTime = 0
                    actionSchedule.endTime = (actionSchedule.startTime+90*60)
                    if (actionSchedule.startTime > 24*60*60) actionSchedule.startTime=0
                    if (actionSchedule.endTime > 24*60*60) actionSchedule.endTime=0
                }
                else -> throw IllegalStateException("Invalid type")
            }

            // Dialog
            val dialog = ActionScheduleDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("actionSchedule", actionSchedule)
                putInt("type", schedule.type)
                putBoolean("isCreated", true)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
        }

        return view
    }


     private fun setLoadingView(){
         binding.loadingView.visibility = View.VISIBLE
         binding.emptyView.visibility = View.GONE
     }
     private fun setEmptyView(){
         binding.loadingView.visibility = View.GONE
         binding.emptyView.visibility = View.VISIBLE
     }
     private fun setNullView(){
         binding.loadingView.visibility = View.GONE
         binding.emptyView.visibility = View.GONE
     }



    // Support animation recyclerView
    private class DiffUtilCallback(private val oldList: List<ActionSchedule>,
                                   private val newList: List<ActionSchedule>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }


    private inner class Holder(private val binding: ItemRecyclerViewActionScheduleBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var actionSchedule: ActionSchedule

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(actionSchedule: ActionSchedule) {
            this.actionSchedule = actionSchedule

            /*  Set action type  */
            val liveActionType = viewModel.getActionType(actionSchedule.actionTypeId)
            liveActionType.observe(viewLifecycleOwner, { actionType ->
                binding.actionType = actionType
            })

            /*  Set text  */
            val startTime = actionSchedule.startTime
            val endTime = actionSchedule.endTime

            if (startTime > 24*60*60)  binding.start.text = "??:??"
            else binding.start.text = DateTimeFormatter.ofPattern("HH:mm").format(
                LocalTime.ofSecondOfDay(startTime))

            if (endTime > 24*60*60)  binding.end.text = "??:??"
            else binding.end.text = DateTimeFormatter.ofPattern("HH:mm").format(
                LocalTime.ofSecondOfDay(endTime))
        }

        fun setError(error: Boolean){
            if (error) binding.error.visibility = View.VISIBLE
            else binding.error.visibility = View.GONE
        }

        override fun onClick(v: View) {
            /*  Create dialog for edit  */
            val dialog = ActionScheduleDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("actionSchedule", actionSchedule)
                putInt("type", schedule.type)
                putBoolean("isCreated", false)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
        }
    }

    private inner class Adapter(var actionsSchedule: List<ActionSchedule>): RecyclerView.Adapter<Holder>(){
        override fun getItemCount() = actionsSchedule.size

        // Cringe Logic for animation
        private var lastPosition = -1

        fun setData(newData: List<ActionSchedule>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilCallback(actionsSchedule, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update data
            actionsSchedule = newData
            // Animation
            diffResult.dispatchUpdatesTo(this)

            // Show view
            if (actionsSchedule.isEmpty()){
                setEmptyView()
            } else {
                setNullView()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
            return Holder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_action_schedule,
                    parent, false))
        }


        override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) onBindViewHolder(holder, position)
            else {
                /*  Если указан payloads, то устанавливаем видимость error  */
                holder.setError(payloads[payloads.size-1] as Boolean)
            }
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(actionsSchedule[position])

            // Animation
            if (holder.adapterPosition > lastPosition){
                lastPosition = holder.adapterPosition

                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_add_item)
                holder.itemView.startAnimation(animation)
            }
        }
    }


    // Support move and swiped
    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        CustomItemTouchCallback(requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

        private val mAdapter = binding.recyclerView.adapter!!

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            // Yeah, symmetry
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            // Update index
            viewModel.actionsSchedule.value!![from].indexList = to
            viewModel.actionsSchedule.value!![to].indexList = from

            // Update recyclerView
            Collections.swap(viewModel.actionsSchedule.value!!, from, to)
            recyclerView.adapter?.notifyItemMoved(from, to)


            return true
        }

        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder,
                                           direction: Int) {
            viewModel.deleteActionSchedule(viewModel.actionsSchedule.value!![viewHolder.adapterPosition])
        }

        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder,
                                           direction: Int) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }


     override fun onPause() {
         viewModel.updateListActionTimetable(viewModel.actionsSchedule.value!!)

         super.onPause()
     }


 }