/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

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
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

/**
  * Класс фрагмента, показывающий один день в расписании. Практически идентичен другим фрагментам.
  * Используется собственный виджет ScheduleClockView из ClockViewGroup для визуализации расписания.
  * Также есть отдельный экран загрузки для Clock. Управляется из этого фрагмента.
  *
  * Возможная модификация: перенести переменные schedule и dayIndex в ViewModel.
  *
  * Возможная модификация: перенести действия со ViewModel в функции после onCreateView. Вероятно,
  * из-за этого фрагмент ScheduleFragment долго грузится.
  * @see DayScheduleViewModel
  */
 class DayScheduleFragment : Fragment() {
     /**
      * ViewModel.
      */
     private val viewModel: DayScheduleViewModel
     by lazy { ViewModelProvider(this).get(DayScheduleViewModel::class.java) }
     /**
      * Привязка данных.
      */
     private lateinit var binding: FragmentDayScheduleBinding
     /**
      * Расписание, по которому нужно искать действия.
      */
     private lateinit var schedule: Schedule
     /**
      * Индекс дня, по которому нужно искать действия.
      */
     private var dayIndex: Int = 0

     /**
      * Выполняется перед созданием UI.
      */
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)

         schedule = arguments?.getSerializable("schedule") as Schedule
         dayIndex = arguments?.getInt("dayIndex") as Int

         viewModel.getActionsSchedule(schedule, dayIndex)
     }

     /**
      * Создание UI.
      */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
         binding = FragmentDayScheduleBinding.inflate(inflater, container, false)
         val view = binding.root

         binding.clock.setFinishedListener { binding.loadingClock.visibility = View.GONE }
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

         binding.clock.setStartTime(schedule.defaultStartDayTime)
         binding.set.setOnClickListener {
             val hour = schedule.defaultStartDayTime.toInt()/3600
             val minute =  (schedule.defaultStartDayTime.toInt()-hour*3600)/60

             val dialog = MaterialTimePicker.Builder()
                 .setTimeFormat(TimeFormat.CLOCK_24H)
                 .setHour(hour)
                 .setMinute(minute)
                 .setTitleText("")
                 .build()

             dialog.addOnPositiveButtonClickListener {
                 binding.loadingClock.visibility = View.VISIBLE

                 schedule.defaultStartDayTime = (dialog.hour * 60 + dialog.minute) * 60L
                 viewModel.updateSchedule(schedule)

                 if (schedule.type == TYPE_SCHEDULE_ABSOLUTE)
                     updateStartEndTimes(viewModel.actionsSchedule.value!!)
                 binding.clock.setStartTime(schedule.defaultStartDayTime)

                 binding.clock.setActionsSchedule(viewModel.actionsSchedule.value!!, false)
             }
             dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
         }


         binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
         }
         itemTouchHelper.attachToRecyclerView(binding.recyclerView)


         viewModel.actionsSchedule.observe(viewLifecycleOwner, {
            updateStartEndTimes(it)

            // Если данные не изменились, то заного рисовать не нужно.
            // Это необходимо не только при перевороте устройства, а еще когда пользователь
            // работает с соседней вкладкой в ViewPager2. Ибо при изменении базы данных
            // об этом будут знать и соседние фрагменты.
            if ((binding.recyclerView.adapter as Adapter).actionsSchedule != it){
                binding.loadingClock.visibility = View.VISIBLE
                binding.clock.setActionsSchedule(it, false)
            }

            // Нельзя создавать новый адаптер, так как используется DiffUtil
            // для нахождения оптимизированных изменений данных.
            (binding.recyclerView.adapter as Adapter).setData(it)
        })


         binding.fab.setOnClickListener{
            val actionSchedule = ActionSchedule(scheduleID=schedule.id, dayIndex=dayIndex,
                indexList=viewModel.actionsSchedule.value?.size!!)

            when (schedule.type){
                TYPE_SCHEDULE_RELATIVE -> {
                    actionSchedule.startAfter = 30*60
                    actionSchedule.duration = 90*60
                }
                TYPE_SCHEDULE_ABSOLUTE -> {
                    val actionsSchedule = viewModel.actionsSchedule.value!!

                    if (actionsSchedule.isNotEmpty()) actionSchedule.startTime =
                        (actionsSchedule[actionsSchedule.size-1].endTime+30*60)
                    else actionSchedule.startTime = 0

                    actionSchedule.endTime = (actionSchedule.startTime+90*60)
                }
                else -> throw IllegalStateException("Invalid type")
            }


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

     /**
      * Обновляет значения startTime и emdTime у actionsSchedule. Используется тот факт, что
      * массивы в функцию передаются с помощью ссылок. Тем самым изменяется и исходный массив.
      */
     private fun updateStartEndTimes(actionsSchedule: List<ActionSchedule>){
         if (schedule.type == TYPE_SCHEDULE_RELATIVE){
             actionsSchedule.forEachIndexed { i, actionSchedule ->
                 var start = actionSchedule.startAfter
                 start += if (i != 0) actionsSchedule[i-1].endTime
                 else schedule.defaultStartDayTime

                 actionsSchedule[i].startTime = start
                 actionsSchedule[i].endTime = start+actionSchedule.duration

                 // В абсолютном расписании невозможно указать время больше, чем 24 часа.
                 // Но в относительном можно. Но тут все по-другому. Все, что больше 24 часов,
                 // должно находится в промежутке между 00:00 и defaultDayStartTime, так как мы
                 // далем круг. Если не влезает, то ошибка.

                 // Если все-таки перешли, то ставим специальный код,
                 // чтобы TimeView смог обработать ошибку.
                 if (actionsSchedule[i].startTime-24*60*60 >= schedule.defaultStartDayTime) {
                     actionsSchedule[i].startTime = -100
                 }
                 if (actionsSchedule[i].endTime-24*60*60 >= schedule.defaultStartDayTime) {
                     actionsSchedule[i].endTime = -100
                 }
             }
         }
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
    private inner class Holder(private val binding: ItemRecyclerViewActionScheduleBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
         /**
          * Действие, которое показывает holder. Необходимо для передачи информации в ActionScheduleDialog.
          */
        private lateinit var actionSchedule: ActionSchedule

         /**
          * Инициализация холдера. Установка onClickListener на сам холдер.
          */
        init {
            itemView.setOnClickListener(this)
        }

         /**
          * Установка содержимого holder-а.
          */
        fun bind(actionSchedule: ActionSchedule) {
            this.actionSchedule = actionSchedule
             binding.actionSchedule = actionSchedule

            val actionType = viewModel.getActionType(actionSchedule.actionTypeId)
            actionType.observe(viewLifecycleOwner, { binding.actionType = it })
        }

         /**
          * Устанавливает ошибку ан холдер. Вызвается в адаптере с помощью payload.
          */
         fun setError(error: Boolean){
            if (error) binding.error.visibility = View.VISIBLE
            else binding.error.visibility = View.GONE
        }

         /**
          * Вызывается при нажатии на холдер. Создает диалог для изменения action schedule.
          */
        override fun onClick(v: View) {
            val dialog = ActionScheduleDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("actionSchedule", actionSchedule)
                putInt("type", schedule.type)
                putBoolean("isCreated", false)
            }
            dialog.show(activity?.supportFragmentManager!!, "ActionScheduleDialog")
        }
    }

     /**
      * Практически такой же адаптер, как и в других фрагментах. За тем исключение, что может
      * обрабатывать payload полученную из интерфеса OnCorruptListener у виджета clock.
      * Это используется, если нужно показать или убрать ошибку у action schedule.
      */
    private inner class Adapter(var actionsSchedule: List<ActionSchedule>): RecyclerView.Adapter<Holder>(){
         /**
          * Нужна для сохранения последней позиции holder-а, который увидил пользователь.
          * Используется для анимации.
          */
        private var lastPosition = -1

         /**
          * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
          */
        fun setData(newData: List<ActionSchedule>){
            val diffUtilCallback = DiffUtilCallback(actionsSchedule, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            actionsSchedule = newData
            diffResult.dispatchUpdatesTo(this)

            if (actionsSchedule.isEmpty())setEmptyView()
            else setNullView()
        }

         /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
         override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
            return Holder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_action_schedule,
                    parent, false))
        }

         override fun getItemCount() = actionsSchedule.size

         /**
          * Старший брат onBindViewHolder. Обратывает payload, получаемую из интерфейса
          * OnCorruptListener у виджета clock. Это используется, если нужно показать
          * или убрать ошибку у action schedule.
          */
         override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) onBindViewHolder(holder, position)
            else holder.setError(payloads[payloads.size-1] as Boolean)
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

     /**
      * Класс для объявления функций класса DiffUtil.Callback. См. оф. документацию.
      *
      * Возможная модификация: необходимо вынести этот класс в файл RecyclerView, так как
      * он повторяется почти по всех RecyclerView. Но из-за того, что в каждом RecyclerView
      * данные разных типов, это сделать проблематично. (Но ведь возможно!)
      */
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

     /**
      * Переопределение класа CustomItemTouchCallback из файла RecyclerViewAnimation.
      * Перемещения вверх или вниз разрешены, взмахи влево или вправо разрешены.
      */
    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        CustomItemTouchCallback(requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
         /**
          * Адаптер RecyclerView в этом фрагменте. Нужен в функции onClickNegativeButton, чтобы
          * уведомить адаптер, что произошла отмена удаления и нужно вернуть holder на место.
          */
        private val mAdapter = binding.recyclerView.adapter!!

         /**
          * Выполняется, когда пользователь перемещает элемент вверх или вниз. Перемещает сами
          * holder-ы, меняет местами соотсветствующие цели в массиве, обновляет индексы и
          * уведомляет обо всем этом двум адаптерам.
          */
        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            viewModel.actionsSchedule.value!![from].indexList = to
            viewModel.actionsSchedule.value!![to].indexList = from

            Collections.swap(viewModel.actionsSchedule.value!!, from, to)
            recyclerView.adapter?.notifyItemMoved(from, to)

            return true
        }

         /**
          * Выполняется при нажатии на кнопку "Yes". Удаляет выбранный элемент из базы данных
          * со всем деревом.
          */
        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder) {
            viewModel.deleteActionSchedule(viewModel.actionsSchedule.value!![viewHolder.adapterPosition])
        }

         /**
          * Выполняется при нажатии на кнопку "No". Уведомляет адаптер, что произошла отмена удаления
          * и нужно выбранный элемент вернуть на место.
          */
        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }

        }

        ItemTouchHelper(simpleItemTouchCallback)
    }

     /**
      * Выполняет, когда фрагмент перестает работать. Сохраняет все indexList,
      * требуемые пользователем.
      */
     override fun onPause() {
         viewModel.updateListActionTimetable(viewModel.actionsSchedule.value!!)

         super.onPause()
     }


 }