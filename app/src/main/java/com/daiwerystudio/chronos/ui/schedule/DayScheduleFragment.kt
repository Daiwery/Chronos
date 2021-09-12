/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 23.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавление логики взаимодействия с DaySchedule.
*
* Дата изменения: 11.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: удаление таблицы дня в расписании. Теперь только один тип.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
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
import com.daiwerystudio.chronos.databinding.FragmentDayScheduleBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionScheduleBinding
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.union.UnionSimpleCallback
import java.time.format.FormatStyle

class DayScheduleFragment : Fragment() {
    private val viewModel: DayScheduleViewModel
            by lazy { ViewModelProvider(this).get(DayScheduleViewModel::class.java) }
    private lateinit var binding: FragmentDayScheduleBinding
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
                        viewModel.deleteActionSchedule(viewModel.actionsSchedule.value!![position])
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

        viewModel.daySchedule.value =
            Pair(arguments?.getString("scheduleID")!!, arguments?.getInt("dayIndex")!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDayScheduleBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            // itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) binding.fab.show()
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) binding.fab.hide()
            }
        })

        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).setData(it)
            binding.loadingClock.visibility = View.VISIBLE
            binding.clock.setActionsSchedule(it)
        })

        binding.fab.setOnClickListener {
            val actionSchedule = ActionSchedule(scheduleID=viewModel.daySchedule.value!!.first,
            dayIndex=viewModel.daySchedule.value!!.second)

            val actionsSchedule = viewModel.actionsSchedule.value!!
            if (actionsSchedule.isNotEmpty()) actionSchedule.startTime =
                (actionsSchedule[actionsSchedule.size-1].endTime+30*60*1000)%(25*60*60*1000)
            else actionSchedule.startTime = 0
            actionSchedule.endTime = (actionSchedule.startTime+90*60*1000)%(25*60*60*1000)

            val dialog = ActionScheduleDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("actionSchedule", actionSchedule)
                putBoolean("isCreated", true)
                putString("parentID", viewModel.daySchedule.value!!.first)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
        }

        binding.clock.setFinishedListener { binding.loadingClock.visibility = View.GONE }

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
                    putBoolean("isCreated", false)
                    putString("parentID", viewModel.daySchedule.value!!.first)
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
             val diffUtilCallback = ActionScheduleDiffUtil(actionsSchedule, newData)
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

    class ActionScheduleDiffUtil(private val oldList: List<ActionSchedule>,
                                 private val newList: List<ActionSchedule>): DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
 }