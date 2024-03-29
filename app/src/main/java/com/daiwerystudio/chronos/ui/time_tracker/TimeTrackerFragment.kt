/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 24.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавления логики работы с ClockViewModel и добавление отдельного холдера
* для пересечения.
*/

package com.daiwerystudio.chronos.ui.time_tracker

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.databinding.FragmentTimeTrackerBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionSectionBinding
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.day.DayFabMenu
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.time_tracker.TimeTrackerFabMenu.Companion.TYPE_ACTION
import com.daiwerystudio.chronos.ui.time_tracker.TimeTrackerFabMenu.Companion.TYPE_ACTION_TRACKER
import com.daiwerystudio.chronos.ui.union.UnionItemAnimator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class TimeTrackerFragment : Fragment() {
    private val viewModel: TimeTrackerViewModel
        by lazy { ViewModelProvider(this).get(TimeTrackerViewModel::class.java) }
    private lateinit var binding: FragmentTimeTrackerBinding
    private var animationClock: ObjectAnimator? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.day.value == null)
            viewModel.day.value = (System.currentTimeMillis()+viewModel.local)/(1000*60*60*24)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentTimeTrackerBinding.inflate(inflater, container, false)
        binding.motionLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = UnionItemAnimator()
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (binding.calendarView.visibility == View.GONE)
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                        if (binding.fab.state != DayFabMenu.STATE_INVISIBLE) binding.fab.hide()
                        else binding.fab.show()
            }
        })

        viewModel.day.observe(viewLifecycleOwner, {
            binding.calendarView.date = viewModel.day.value!!*24*60*60*1000L
            binding.toolBar.title = LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        })
        viewModel.mActions.observe(viewLifecycleOwner, { setLoadingView() })
        viewModel.sections.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).setData(it)
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

        binding.calendarView.visibility = View.GONE
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.day.value = LocalDate.of(year, month+1, dayOfMonth).toEpochDay()
        }

        binding.fab.setOnMenuItemClickListener {
            when (it) {
                TYPE_ACTION -> {
                    val action = Action()
                    val day = (System.currentTimeMillis()+viewModel.local)/(1000*60*60*24)
                    action.startTime += (viewModel.day.value!!-day)*24*60*60*1000
                    action.endTime += (viewModel.day.value!!-day)*24*60*60*1000

                    val dialog = ActionDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("action", action)
                        putBoolean("isCreated", true)
                    }
                    dialog.show(this.requireActivity().supportFragmentManager, "ActionDialog")
                }

                TYPE_ACTION_TRACKER -> {
                    val dialog = ActionTrackerDialog()
                    dialog.show(this.requireActivity().supportFragmentManager, "ActionTrackerDialog")
                }
                else -> throw IllegalArgumentException("Invalid type")
            }
        }

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

        if (!viewModel.isAnimated) {
            val time = (System.currentTimeMillis()+viewModel.local)%(24*60*60*1000)-60*60*1000
            val ratio = time/(24*60*60*1000f)
            val scrollY = (binding.clock.getChildAt(0).height*ratio).toInt()
            animationClock = ObjectAnimator.ofInt(binding.clock, "scrollY",  scrollY).setDuration(1000)
            animationClock?.start()
            viewModel.isAnimated = true
        }
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


    private inner class Adapter(var sections: List<TimeTrackerViewModel.Section>):
        RecyclerView.Adapter<SectionHolder>(){

        fun setData(newData: List<TimeTrackerViewModel.Section>){
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
        private lateinit var data: List<Pair<Action, ActionType?>>

        init {
            binding.recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = SectionAdapter(emptyList())
            }
            binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollHorizontally(1))
                        ObjectAnimator.ofFloat(binding.imageView11,
                            "alpha",  0f).setDuration(300).start()
                    else {
                        if (data.size == 1) ObjectAnimator.ofFloat(binding.imageView11,
                            "alpha",  0f).setDuration(300).start()
                        else ObjectAnimator.ofFloat(binding.imageView11,
                            "alpha",  1f).setDuration(300).start()
                    }
                }
            })
        }

        fun bind(data: List<Pair<Action, ActionType?>>){
            this.data = data.map { it.copy() }
            (binding.recyclerView.adapter as SectionAdapter).updateData(data)

            if (data.size == 1) ObjectAnimator.ofFloat(binding.imageView11,
                "alpha",  0f).setDuration(300).start()
            else ObjectAnimator.ofFloat(binding.imageView11,
                "alpha",  1f).setDuration(300).start()
        }

        fun sendPayload(payload: Any){
            binding.recyclerView.adapter?.notifyItemRangeChanged(0, data.size, payload)
        }
    }

    private inner class SectionAdapter(var data: List<Pair<Action, ActionType?>>):
        RecyclerView.Adapter<ActionHolder>(){

        fun updateData(newData: List<Pair<Action, ActionType?>>){
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
                holder.binding.textView.layoutParams.width = 0
                holder.binding.textView.maxWidth = 0

                val constraintSet = ConstraintSet()
                constraintSet.clone(holder.binding.constraintLayout)
                constraintSet.connect(R.id.textView, ConstraintSet.END,
                    R.id.constraintLayout, ConstraintSet.END)
                constraintSet.applyTo(holder.binding.constraintLayout)
            }
            else {
                holder.itemView.layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                holder.binding.textView.layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                holder.binding.textView.maxWidth = binding.recyclerView.width*10/25

                val constraintSet = ConstraintSet()
                constraintSet.clone(holder.binding.constraintLayout)
                constraintSet.clear(R.id.textView, ConstraintSet.END)
                constraintSet.applyTo(holder.binding.constraintLayout)
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
        private lateinit var action: Action
        private var actionType: ActionType? = null

        init {
            itemView.setOnClickListener {
                if (actionMode == null) {
                    // Восстанавливаем анимацию клика на холдер.
                    itemView.isClickable = true

                    val dialog = ActionDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("action", action)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "ActionDialog")
                } else {
                    // Убираем анимацию клика на холдер.
                    itemView.isClickable = false
                    changeItem(action.id)
                }
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(action.id)
                true
            }
        }

        fun bind(item: Pair<Action, ActionType?>) {
            this.action = item.first
            this.actionType = item.second

            binding.time.text = (formatTime(action.startTime, FormatStyle.SHORT, FORMAT_TIME,
                true, is24HourFormat(requireContext())) + " - " + formatTime(action.endTime,
                FormatStyle.SHORT, FORMAT_TIME, true, is24HourFormat(requireContext())
            ))
            if (actionType == null) {
                binding.actionType = ActionType(id="", color=Color.BLACK, name="???")
                binding.invalid.visibility = View.VISIBLE
                binding.colorLine.visibility = View.INVISIBLE
                binding.colorLine1.visibility = View.INVISIBLE
                binding.colorSteps.visibility = View.INVISIBLE
            } else {
                binding.invalid.visibility = View.GONE
                binding.actionType = actionType
                binding.color.setColorFilter(actionType!!.color)
                binding.colorLine.setColorFilter(actionType!!.color)
                binding.colorLine1.setColorFilter(actionType!!.color)
                binding.colorSteps.setColorFilter(actionType!!.color)
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
                                // после этот массив удалится, а функция выполняется
                                // в отдельном потоке.
                                viewModel.deleteActions(selectedItems.map { it })
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

    private class SectionDiffUtil(private val oldList: List<TimeTrackerViewModel.Section>,
                                  private val newList: List<TimeTrackerViewModel.Section>): DiffUtil.Callback() {
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