/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.time_tracker

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
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.databinding.FragmentTimeTrackerBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionBinding
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.union.UnionDiffUtil
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.daiwerystudio.chronos.ui.union.UnionSimpleCallback
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class TimeTrackerFragment : Fragment() {
    private val viewModel: TimeTrackerViewModel
        by lazy { ViewModelProvider(this).get(TimeTrackerViewModel::class.java) }
    private lateinit var binding: FragmentTimeTrackerBinding
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
                        viewModel.deleteAction(viewModel.actions.value!![position])
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
        binding = FragmentTimeTrackerBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (binding.fab.isShown && dy > 0) binding.fab.hide()
                if (!binding.fab.isShown && dy < 0) binding.fab.show()
            }
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) binding.fab.show()
            }
        })

        viewModel.day.observe(viewLifecycleOwner, {
            binding.toolBar.title =
                LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        })

        viewModel.actions.observe(viewLifecycleOwner, {
            binding.loadingClock.visibility = View.VISIBLE
            (binding.recyclerView.adapter as Adapter).updateData(it)
            binding.clock.setActions(it, viewModel.local, viewModel.day.value!!)
        })

        binding.toolBar.setOnClickListener {
            if (binding.motionLayout.progress > 0.5) binding.motionLayout.transitionToStart()
            else binding.motionLayout.transitionToEnd()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.day.value = LocalDate.of(year, month+1, dayOfMonth).toEpochDay()
        }

        binding.clock.setFinishedListener{ binding.loadingClock.visibility = View.GONE }

        binding.fab.setOnClickListener{
            val dialog = ActionDialog()

            // При создании действия время ставится текущее.
            // Если мы создаем в прошлом или в будущем, мы должны изменить время.
            val action = Action()
            val day = (System.currentTimeMillis()+viewModel.local)/(1000*60*60*24)
            action.startTime += (viewModel.day.value!!-day)*24*60*60*1000
            action.endTime += (viewModel.day.value!!-day)*24*60*60*1000

            dialog.arguments = Bundle().apply{
                putSerializable("action", action)
                putBoolean("isCreated", true)
            }
            dialog.show(this.requireActivity().supportFragmentManager, "ActionDialog")
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


    inner class Holder(val binding: ItemRecyclerViewActionBinding) :
        RecyclerView.ViewHolder(binding.root){

        private lateinit var action: Action

        init {
            itemView.setOnClickListener {
                val dialog = ActionDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("action", action)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "ActionDialog")
            }
        }

        fun bind(action: Action){
            this.action = action
            binding.action = action
            binding.start.text = formatTime(action.startTime, true, FormatStyle.SHORT, FORMAT_TIME)
            binding.end.text = formatTime(action.endTime, true, FormatStyle.SHORT, FORMAT_TIME)

            val actionType = viewModel.getActionType(action.actionTypeId)
            actionType.observe(viewLifecycleOwner, {
                if (it == null) {
                    binding.actionType = ActionType(id=UUID.randomUUID().toString(), color=0, name="???")
                    binding.invalid.visibility = View.VISIBLE
                } else {
                    binding.invalid.visibility = View.GONE
                    binding.actionType = it
                }
            })
        }
    }

    private inner class Adapter(var actions: List<Action>): RecyclerView.Adapter<Holder>(){
        fun updateData(newData: List<Action>){
            val diffUtilCallback = UnionDiffUtil(actions, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            actions = newData.map{ it.copy() }
            diffResult.dispatchUpdatesTo(this)

            if (actions.isEmpty()) setEmptyView()
            else setNullView()
        }

        override fun getItemCount() = actions.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_action,
                parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(actions[position])
        }
    }
}