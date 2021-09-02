/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 31.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменения, связанные с изменениями в DayViewModel.
*/

package com.daiwerystudio.chronos.ui.day

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.databinding.FragmentDayBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionBinding
import com.daiwerystudio.chronos.ui.union.CustomDiffUtil
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class DayFragment: Fragment() {
    private val viewModel: DayViewModel
        by lazy { ViewModelProvider(this).get(DayViewModel::class.java) }
    private lateinit var binding: FragmentDayBinding

    /*  ID действия, которое сейчас нужно выполнять.  */
    private val idMustActionType: MutableLiveData<String?> = MutableLiveData()
    private val mustActionType: LiveData<ActionType> =
        Transformations.switchMap(idMustActionType) {
            when (it){
                null -> {
                    val actionType = MutableLiveData<ActionType>()
                    actionType.value = ActionType(id="", name="")

                    actionType
                }
                "" -> {
                    val actionType = MutableLiveData<ActionType>()
                    actionType.value = ActionType(id="", name="")

                    actionType
                }
                else -> viewModel.getActionType(it)
            }
        }

    /* То действие, которое сейчас выполняется, хранится на устройстве.
     * Это нужно для того, чтобы если пользователь вышел из приложение,
     * действие все это время выполнялось.
     */
    private lateinit var preferences: SharedPreferences
    private var doingStartTime: Long = (System.currentTimeMillis())/1000
    private val idDoingActionType: MutableLiveData<String> = MutableLiveData()
    private val doingActionType: LiveData<ActionType> =
        Transformations.switchMap(idDoingActionType) {
            when (it){
                "" -> {
                    val actionType = MutableLiveData<ActionType>()
                    actionType.value = ActionType(id="", name="")

                    actionType
                }
                else -> viewModel.getActionType(it)
            }
        }

    /* Timer. Каждую секунду извлекает обновляет UI для выполняющегося действия,
     * если оно не null.
     */
    private val mTimer: Handler = Handler(Looper.getMainLooper())
    private val runnableTimer = RunnableTimer()
    private inner class RunnableTimer: Runnable{
        override fun run() {
            val time = (System.currentTimeMillis())/1000
            //if (idDoingActionType.value!! != "") binding.doingTime = time-doingStartTime
            mTimer.postDelayed(this, 1000L)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.day.value == null)
            viewModel.day.value = (System.currentTimeMillis()+viewModel.local)/(1000*60*60*24)

        // Если значение есть, то действие выполняется, но работа приложения было прервано.
        preferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        idDoingActionType.value = preferences.getString("doingActionTypeID", "")
        doingStartTime = preferences.getLong("doingStartTime", (System.currentTimeMillis())/1000)
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

            val position = it-(System.currentTimeMillis()+viewModel.local)/(1000*60*60*24)
            if (position == 0L) setPresent()
            if (position < 0L) setPast()
            if (position > 0L) setFuture()
        })


        binding.toolBar.setOnClickListener {
            val dialog = MaterialDatePicker.Builder.datePicker()
                .setSelection(viewModel.day.value!!*24*60*60*1000).build()

            dialog.addOnPositiveButtonClickListener {
                viewModel.day.value = it/(24*60*60*1000)
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

//        // Если мы в настоящем.
//        if (position == 0L) {
//            mustActionType.observe(viewLifecycleOwner, {
//                binding.mustActionType = it
//
//                if (it.id == "") binding.colorMustActionType.visibility = View.INVISIBLE
//                else binding.colorMustActionType.visibility = View.VISIBLE
//            })
//            binding.clock.setMustActionTypeListener {
//                idMustActionType.value = it
//            }
//
//            doingActionType.observe(viewLifecycleOwner, {
//                binding.doingActionType = it
//
//                if (it.id == "") {
//                    binding.startStop.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)
//                    binding.startTime.visibility = View.GONE
//                    binding.colorDoingActionType.visibility = View.INVISIBLE
//                } else {
//                    binding.startStop.setImageResource(R.drawable.ic_baseline_stop_circle_24)
//                    binding.startTime.visibility = View.VISIBLE
//                    binding.colorDoingActionType.visibility = View.VISIBLE
//                }
//            })
//
//            // Запускаем таймер.
//            mTimer.post(runnableTimer)
//        }

//        binding.startStop.setOnClickListener{
//            val id = idDoingActionType.value!!
//            if (id == ""){
//                val dialog = DoingActionDialog()
//
//                dialog.arguments = Bundle().apply{
//                    putSerializable("actionTypeID", idMustActionType.value!!)
//                }
//
//                dialog.setAddDoingActionTypeListener{actionTypeID, startTime ->
//                    doingStartTime = startTime
//                    idDoingActionType.value = actionTypeID
//
//                    // Сохраняем выполняющее действие.
//                    val editor = preferences.edit()
//                    editor.putLong("doingStartTime", startTime).apply()
//                    editor.putString("doingActionTypeID", actionTypeID).apply()
//                }
//
//                dialog.show(this.requireActivity().supportFragmentManager, "ActionDialog")
//            } else {
//                val action = Action().apply{
//                    startTime = doingStartTime
//                    endTime = (System.currentTimeMillis())/1000
//                    actionTypeId = idDoingActionType.value!!
//                }
//                viewModel.addAction(action)
//
//                // "" - означает, что ничего не выполняется.
//                idDoingActionType.value = ""
//                val editor = preferences.edit()
//                editor.putString("doingActionTypeID", "").apply()
//            }
//        }

        binding.clock.setFinishedListener{ binding.loadingClock.visibility = View.GONE }
        binding.clock.setClickSectionListener{}
        binding.clock.setCorruptedListener{}

        viewModel.actions.observe(viewLifecycleOwner,  {
            binding.clock.setActions(it, viewModel.local, viewModel.day.value!!)
            (binding.recyclerView.adapter as Adapter).setData(it)
        })

        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            binding.loadingClock.visibility = View.VISIBLE
            binding.clock.setActionsSchedule(it)
        })

        binding.fab.setOnClickListener{
            val dialog = ActionDialog()

            val action = Action()
            // Переводим действие в день, выбранный пользователь, если мы не в настоящем.
            val time = System.currentTimeMillis()
            val day = ((time+TimeZone.getDefault().getOffset(time))/(1000*60*60*24)).toInt()
            action.startTime += (viewModel.day.value!!-day)*24*60*60*1000
            action.endTime += (viewModel.day.value!!-day)*24*60*60*1000

            dialog.arguments = Bundle().apply{
                putSerializable("action", action)
                putBoolean("isCreated", true)
            }
            // dialog.show(this.requireActivity().supportFragmentManager, "ActionDialog")
        }

        return binding.root
    }

    private fun setFuture(){
        binding.clock.setFuture()
    }

    private fun setPast(){
        binding.clock.setPast()
    }

    private fun setPresent(){
        binding.clock.setPresent()
    }

    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    private inner class Holder(private val binding: ItemRecyclerViewActionBinding):
        RecyclerView.ViewHolder(binding.root) {
        private lateinit var action: Action

        init {
            itemView.setOnClickListener {
                val dialog = ActionDialog()

                val day = System.currentTimeMillis()/(1000*60*60*24)
                action.startTime += (viewModel.day.value!!-day)*24*60*60
                action.endTime += (viewModel.day.value!!-day)*24*60*60

                dialog.arguments = Bundle().apply{
                    putSerializable("action", action)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "ActionDialog")
            }
        }

        fun bind(action: Action) {
            this.action = action
            binding.action = action

            val actionType = viewModel.getActionType(action.actionTypeId)
            actionType.observe(viewLifecycleOwner, {
                if (it == null) {
                    binding.actionType = ActionType(id=UUID.randomUUID().toString(), color=0, name="???")
                    binding.invalid.visibility = View.VISIBLE
                }
                else {
                    binding.invalid.visibility = View.GONE
                    binding.actionType = it
                }
            })
        }
    }

    private inner class Adapter(var actions: List<Action>): RecyclerView.Adapter<Holder>(){
        fun setData(newData: List<Action>){
            val diffUtilCallback = CustomDiffUtil(actions, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            actions = newData
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

    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        ItemTouchHelper.SimpleCallback(-1, ItemTouchHelper.LEFT){

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            return false
        }


        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            viewModel.deleteAction(viewModel.actions.value!![viewHolder.absoluteAdapterPosition])
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