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
*
* Дата изменения: 23.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавления логики работы с ClockViewModel и добавление отдельного холдера
* для пересечения.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.icu.util.TimeZone
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.*
import android.widget.Toast
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
import com.daiwerystudio.chronos.databinding.FragmentDayScheduleBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionSectionBinding
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.union.UnionItemAnimator
import java.time.format.FormatStyle

class DayScheduleFragment : Fragment() {
    private val viewModel: DayScheduleViewModel
            by lazy { ViewModelProvider(this).get(DayScheduleViewModel::class.java) }
    private lateinit var binding: FragmentDayScheduleBinding
    private var animationClock: ObjectAnimator? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.daySchedule.value =
            Pair(arguments?.getString("scheduleID")!!, arguments?.getInt("dayIndex")!!)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDayScheduleBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = UnionItemAnimator()
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    if (binding.fab.visibility == View.VISIBLE) binding.fab.hide()
                    else binding.fab.show()
            }
        })

        viewModel.mActionsSchedule.observe(viewLifecycleOwner, { setLoadingView() })
        viewModel.sections.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).setData(it)
        })
        viewModel.actionDrawables.observe(viewLifecycleOwner, {
            binding.clock.setActionDrawables(it)
        })

        binding.fab.setOnClickListener {
            val actionSchedule = ActionSchedule(scheduleID=viewModel.daySchedule.value!!.first,
            dayIndex=viewModel.daySchedule.value!!.second)

            val sections = viewModel.sections.value!!
            if (sections.isNotEmpty()) actionSchedule.startTime =
                (sections.last().data.last().first.endTime+30*60*1000)%(24*60*60*1000)
            else actionSchedule.startTime = 0
            actionSchedule.endTime = (actionSchedule.startTime+90*60*1000)%(24*60*60*1000)

            val dialog = ActionScheduleDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("actionSchedule", actionSchedule)
                putBoolean("isCreated", true)
                putString("parentID", viewModel.daySchedule.value!!.first)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
        }

        binding.clock.setOnTouchListener { _, _ ->
            if (animationClock != null) {
                animationClock?.cancel()
                animationClock = null
            }
            false
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val currentTime = System.currentTimeMillis()
        val time = (currentTime+TimeZone.getDefault().getOffset(currentTime))%(24*60*60*1000)-60*60*1000
        val ratio = time/(24*60*60*1000f)
        val scrollY = (binding.clock.getChildAt(0).height*ratio).toInt()
        animationClock = ObjectAnimator.ofInt(binding.clock, "scrollY",  scrollY).setDuration(1000)
        animationClock?.start()
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


    private inner class Adapter(var sections: List<DayScheduleViewModel.Section>):
        RecyclerView.Adapter<SectionHolder>(){

        fun setData(newData: List<DayScheduleViewModel.Section>){
            val diffUtilCallback = SectionDiffUtil(sections, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            sections = newData
            diffResult.dispatchUpdatesTo(this)

            if (sections.isEmpty()) setEmptyView() else setNullView()
        }

        override fun getItemCount() = sections.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionHolder{
            return SectionHolder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_action_section,
                    parent, false))
        }

        override fun onBindViewHolder(holder: SectionHolder, position: Int) {
            holder.bind(sections[position].data)
        }

        override fun onBindViewHolder(holder: SectionHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isNotEmpty()) holder.sendPayload(payloads.last())
            else onBindViewHolder(holder, position)
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

        fun sendPayload(payload: Any){
            binding.recyclerView.adapter?.notifyItemRangeChanged(0, data.size, payload)
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
            if (itemCount == 1) {
                holder.itemView.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                holder.binding.linearLayout.layoutParams.width = 0
            }
            else {
                holder.itemView.layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                holder.binding.linearLayout.layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            }
        }

        /* Если холдер есть в selectedItems, то мы с ним ничего не делаем.
        Если же нет, то изменям прозрачность. */
        override fun onBindViewHolder(holder: ActionHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isNotEmpty())
                if (selectedItems.isNotEmpty())
                    if (data[position].first.id in selectedItems) holder.itemView.alpha = 1f
                    else holder.itemView.alpha = 0.5f
                else holder.itemView.alpha = 1f
            else super.onBindViewHolder(holder, position, payloads)
        }

    }

    private inner class ActionHolder(val binding: ItemRecyclerViewActionBinding):
        RecyclerView.ViewHolder(binding.root){
        private lateinit var actionSchedule: ActionSchedule
        private var actionType: ActionType? = null

        init {
            itemView.setOnClickListener {
                if (actionMode == null) {
                    // Восстанавливаем анимацию клика на холдер.
                    itemView.isClickable = true

                    val dialog = ActionScheduleDialog()
                    dialog.arguments = Bundle().apply {
                        putSerializable("actionSchedule", actionSchedule)
                        putBoolean("isCreated", false)
                        putString("parentID", viewModel.daySchedule.value!!.first)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "ActionScheduleDialog")
                } else {
                    // Убираем анимацию клика на холдер.
                    itemView.isClickable = false
                    changeItem(actionSchedule.id)
                }
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(actionSchedule.id)
                true
            }
        }

        fun bind(item: Pair<ActionSchedule, ActionType?>) {
            this.actionSchedule = item.first
            this.actionType = item.second

            binding.time.text = (formatTime(
                actionSchedule.startTime,
                FormatStyle.SHORT,
                FORMAT_TIME,
                false,
                is24HourFormat(requireContext())
            ) +
                    " - " + formatTime(
                actionSchedule.endTime,
                FormatStyle.SHORT,
                FORMAT_TIME,
                false,
                is24HourFormat(requireContext())
            ))
            if (actionType == null) {
                binding.actionType = ActionType(id="", color=Color.BLACK, name="???")
                binding.invalid.visibility = View.VISIBLE
            } else {
                binding.invalid.visibility = View.GONE
                binding.actionType = actionType
            }
        }
    }

    // Будем хранить позиции выбранных холдеров.
    private val selectedItems: MutableList<String> = mutableListOf()
    private var actionMode: ActionMode? = null

    private fun startActionMode(){
        actionMode = requireActivity().startActionMode(callback)
        actionMode?.title = "0"
    }

    private fun changeItem(id: String){
        val index = selectedItems.indexOf(id)
        if (index == -1) selectedItems.add(id)
        else selectedItems.removeAt(index)

        binding.recyclerView.adapter?.notifyItemRangeChanged(0, viewModel.sections.value!!.size, true)
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
                                // после этот массив удалится, а дфункция выполняется
                                // в отдельном потоке.
                                viewModel.deleteActionsSchedule(selectedItems.map { it })
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
                binding.recyclerView.adapter?.notifyItemRangeChanged(0, viewModel.sections.value!!.size, false)
                binding.fab.show()
            }
        }
    }

    private class SectionDiffUtil(private val oldList: List<DayScheduleViewModel.Section>,
                                  private val newList: List<DayScheduleViewModel.Section>): DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
 }