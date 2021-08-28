/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.day

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.databinding.FragmentDayBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * Основная логика такая же, как и в остальных объектах. Но имеет несколько особенностей:
 * Содержится внутри бесконечного ViewPager2; используется таймер, чтобы проверить, какое сейчас
 * нужно делать действие (в ClockViewGroup); сохраняет в SharedPreferences
 * текущее действие, которое выполняет пользователь, и по таймеру меняет соответсвующий UI.
 */
class DayFragment: Fragment() {
    /**
     * ViewModel.
     */
    private val viewModel: DayViewModel
    by lazy { ViewModelProvider(this).get(DayViewModel::class.java) }

    /**
     * Привязка данных.
     */
    private lateinit var binding: FragmentDayBinding

    /**
     * Переменная для связи с локальным файлом настроек. Нужен для хранения времени пробуждения
     * и для доступа к текущему действию.
     */
    private lateinit var preferences: SharedPreferences

    /**
     * id типа действия, которое нужно сейчас выполнять в обертке MutableLiveData.
     * Его id меняется в таймере.
     */
    private val idMustActionType: MutableLiveData<String?> = MutableLiveData()

    /**
     * Тип действия, который нужно выполнить. Связан с idMustActionType.
     * Это все нужно, чтобы была подписка на изменения базы данных.
     */
    private val mustActionType: LiveData<ActionType> =
        Transformations.switchMap(idMustActionType) {
            when (it){
                null -> {
                    val actionType = MutableLiveData<ActionType>()
                    actionType.value = ActionType(id="", name=resources.getString(R.string.error))

                    actionType
                }
                "" -> {
                    val actionType = MutableLiveData<ActionType>()
                    actionType.value = ActionType(id="", name=resources.getString(R.string.nothing))

                    actionType
                }
                else -> viewModel.getActionType(it)
            }
        }


    /**
     * Время начала выполняющегося действия.
     */
    private var doingStartTime: Long = (System.currentTimeMillis())/1000
    /**
     * id типа действия, которое сейчас выполняет пользователь в обертке MutableLiveData.
     */
    private val idDoingActionType: MutableLiveData<String> = MutableLiveData()

    /**
     * Тип действия, которое выполняет пользователль. Связан с idDoingActionType.
     * Это все нужно, чтобы была подписка на изменения базы данных.
     */
    private val doingActionType: LiveData<ActionType> =
        Transformations.switchMap(idDoingActionType) {
            when (it){
                "" -> {
                    val actionType = MutableLiveData<ActionType>()
                    actionType.value = ActionType(id="", name=resources.getString(R.string.nothing))

                    actionType
                }
                else -> {
                    viewModel.getActionType(it)
                }
            }
        }

    /**
     * Timer. Каждую секунду извлекает обновляет UI для выполняющегося действия,
     * если оно не null.
     */
    private val mTimer: Handler = Handler(Looper.getMainLooper())

    /**
     * Runnable объект для таймера.
     */
    private val runnableTimer = RunnableTimer()

    /**
     * Выполняется перед созданием UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.day = arguments?.getInt("day") as Int

        (activity as AppCompatActivity).supportActionBar?.title =
            LocalDate.ofEpochDay(viewModel.day*1L).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

        preferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        viewModel.startDayTime.value = preferences.getLong("startDayTime", 6*60*60L)

        // Если значение есть, то действие выполняется, но работа приложения
        // было прервано.
        idDoingActionType.value = preferences.getString("doingActionTypeID", "")
        doingStartTime = preferences.getLong("doingStartTime", (System.currentTimeMillis())/1000)
    }

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentDayBinding.inflate(inflater, container, false)
        val view = binding.root

        val position = viewModel.day-(System.currentTimeMillis()+viewModel.local
                +viewModel.startDayTime.value!!)/(1000*60*60*24)
        if (position == 0L) setPresent()
        if (position < 0L) setPast()
        if (position > 0L) setFuture()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            // itemAnimator = ItemAnimator()
        }
        // itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        // Если мы в настоящем.
        if (position == 0L) {
            mustActionType.observe(viewLifecycleOwner, {
                binding.mustActionType = it

                if (it.id == "") binding.colorMustActionType.visibility = View.INVISIBLE
                else binding.colorMustActionType.visibility = View.VISIBLE
            })
            binding.clock.setMustActionTypeListener {
                idMustActionType.value = it
            }

            doingActionType.observe(viewLifecycleOwner, {
                binding.doingActionType = it

                if (it.id == "") {
                    binding.startStop.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)
                    binding.startTime.visibility = View.GONE
                    binding.colorDoingActionType.visibility = View.INVISIBLE
                } else {
                    binding.startStop.setImageResource(R.drawable.ic_baseline_stop_circle_24)
                    binding.startTime.visibility = View.VISIBLE
                    binding.colorDoingActionType.visibility = View.VISIBLE
                }
            })

            // Запускаем таймер.
            mTimer.post(runnableTimer)
        }


        binding.startStop.setOnClickListener{
            val id = idDoingActionType.value!!
            if (id == ""){
                val dialog = DoingActionDialog()

                dialog.arguments = Bundle().apply{
                    putSerializable("actionTypeID", idMustActionType.value!!)
                }

                dialog.setAddDoingActionTypeListener{actionTypeID, startTime ->
                    doingStartTime = startTime
                    idDoingActionType.value = actionTypeID

                    // Сохраняем выполняющее действие.
                    val editor = preferences.edit()
                    editor.putLong("doingStartTime", startTime).apply()
                    editor.putString("doingActionTypeID", actionTypeID).apply()
                }

                dialog.show(this.requireActivity().supportFragmentManager, "ActionDialog")
            } else {
                val action = Action().apply{
                    startTime = doingStartTime
                    endTime = (System.currentTimeMillis())/1000
                    actionTypeId = idDoingActionType.value!!
                }
                viewModel.addAction(action)

                // "" - означает, что ничего не выполняется.
                idDoingActionType.value = ""
                val editor = preferences.edit()
                editor.putString("doingActionTypeID", "").apply()
            }
        }


        binding.clock.setFinishedListener{
            binding.loadingClock.visibility = View.GONE
        }
        binding.clock.setClickListener{}
        binding.clock.setCorruptedListener{
            binding.countCorrupted = it
        }


        viewModel.actions.observe(viewLifecycleOwner,  {
            // Изменения actions могут быть вызваны изменением startDayTime.
            // Чтобы не было гонки потоков, устанавливаем значение здесь.
            binding.clock.setStartTime(viewModel.startDayTime.value!!)

            binding.clock.setActions(it, viewModel.local, viewModel.day)

            (binding.recyclerView.adapter as Adapter).setData(it)
        })

        viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            binding.loadingClock.visibility = View.VISIBLE

            // Изменения actions могут быть вызваны изменением startDayTime.
            // Чтобы не было гонки потоков, устанавливаем значение здесь.
            binding.clock.setStartTime(viewModel.startDayTime.value!!)

            binding.clock.setActionsSchedule(it, viewModel.startDayTime.value!!)
        })


        binding.fab.setOnClickListener{
            val dialog = ActionDialog()

            val action = Action()
            val time = System.currentTimeMillis()
            val day = ((time+ TimeZone.getDefault().getOffset(time))/(1000*60*60*24)).toInt()
            action.startTime += (viewModel.day-day)*24*60*60
            action.endTime += (viewModel.day-day)*24*60*60

            dialog.arguments = Bundle().apply{
                putSerializable("action", action)
                putBoolean("isCreated", true)
            }
            dialog.show(this.requireActivity().supportFragmentManager, "ActionDialog")
        }

        binding.set.setOnClickListener {
            val hour = viewModel.startDayTime.value!!.toInt()/3600
            val minute = (viewModel.startDayTime.value!!.toInt()-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                viewModel.startDayTime.value = (dialog.hour * 60 + dialog.minute)*60L

                val editor = preferences.edit()
                editor.putLong("startDayTime", viewModel.startDayTime.value!!).apply()
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        return view
    }

    /**
     * Выполняется, когда фрагмент снова становится видимым.
     */
    override fun onResume() {
        super.onResume()

        // Если пользователь изменил startDayTime в расписании, то мы должны об этом знать.
        viewModel.startDayTime.postValue(preferences.getLong("startDayTime", 6*60*60L))

        (activity as AppCompatActivity).supportActionBar?.title =
            LocalDate.ofEpochDay(viewModel.day*1L).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    /**
     * Устанавливаем видимость для виджетов связанных с действиями,
     * которое нужно выполнить и которое выполняется.
     */
    private fun setMustAndDoingUIVisibility(visibility: Int){
        binding.textView18.visibility = visibility
        binding.colorMustActionType.visibility = visibility
        binding.textView1.visibility = visibility
        binding.textView17.visibility = visibility
        binding.colorDoingActionType.visibility = visibility
        binding.textView19.visibility = visibility
        binding.startTime.visibility = visibility
        binding.startStop.visibility = visibility
    }

    /**
     * Устанавливает видимость компонентов UI, если мы в будущем.
     */
    private fun setFuture(){
        binding.clock.setFuture()
        setMustAndDoingUIVisibility(View.GONE)
    }

    /**
     * Устанавливает видимость компонентов UI, если мы в прошлом.
     */
    private fun setPast(){
        binding.clock.setPast()
        setMustAndDoingUIVisibility(View.GONE)
    }

    /**
     * Устанавливает видимость компонентов UI, если мы в настощем.
     */
    private fun setPresent(){
        binding.clock.setPresent()
        setMustAndDoingUIVisibility(View.VISIBLE)
    }

    /**
     * Устанавливает layout_empty и layout_loading невидимыми.
     */
    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    /**
     * Уставнавливает меню и заполняет его по menu.edit_delete_menu.
     */
    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }

    /**
     * Класс Holder-а для RecyclerView. Особенностей не имеет. За исключеним того, что при нажатии
     * на холдер, появляется диалог с его изменением.
     */
    private inner class Holder(private val binding: ItemRecyclerViewActionBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        /**
         * Действие, которое показывает holder. Необходимо для передачи информации в ActionDialog.
         */
        private lateinit var action: Action

        /**
         * Инициализация холдера. Установка onClickListener на сам холдер.
         */
        init {
            itemView.setOnClickListener(this)
        }

        /**
         * Установка содержимого holder-а.
         */
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

        /**
         * Вызывается при нажатии на холдер. Создает диалог для изменения action schedule.
         */
        override fun onClick(v: View) {
            val dialog = ActionDialog()

            val day = System.currentTimeMillis()/(1000*60*60*24)
            action.startTime += (viewModel.day-day)*24*60*60
            action.endTime += (viewModel.day-day)*24*60*60

            dialog.arguments = Bundle().apply{
                putSerializable("action", action)
                putBoolean("isCreated", false)
            }
            dialog.show(requireActivity().supportFragmentManager, "ActionDialog")
        }
    }

    /**
     * Такой же адаптер, как и в других фрагментах.
     */
    private inner class Adapter(var actions: List<Action>): RecyclerView.Adapter<Holder>(){
        /**
         * Нужна для сохранения последней позиции holder-а, который увидил пользователь.
         * Используется для анимации.
         */
        private var lastPosition = -1

        /**
         * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
         */
        fun setData(newData: List<Action>){
            val diffUtilCallback = DiffUtilCallback(actions, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            actions = newData
            diffResult.dispatchUpdatesTo(this)

            if (actions.isEmpty()) setEmptyView()
            else setNullView()
        }

        /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_action,
                parent, false))
        }

        override fun getItemCount() = actions.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(actions[position])

            // Animation
            if (holder.adapterPosition > lastPosition){
                lastPosition = holder.adapterPosition

                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_add_item)
                holder.itemView.startAnimation(animation)
            }
        }
    }

    /**
     * Класс для объявления функций класса DiffUtil.Callback. См. оф. документацию.
     *
     * Возможная модификация: необходимо вынести этот класс в файл RecyclerView, так как
     * он повторяется почти по всех RecyclerView. Но из-за того, что в каждом RecyclerView
     * данные разных типов, это сделать проблематично. (Но ведь возможно!)
     */
    private class DiffUtilCallback(private val oldList: List<Action>,
                                   private val newList: List<Action>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }


    /**
     * Runnable объект для таймера.
     */
    private inner class RunnableTimer: Runnable{
        override fun run() {
            val time = (System.currentTimeMillis())/1000
            if (idDoingActionType.value!! != "")
                binding.doingTime = time-doingStartTime

            mTimer.postDelayed(this, 1000L)
        }

    }

}