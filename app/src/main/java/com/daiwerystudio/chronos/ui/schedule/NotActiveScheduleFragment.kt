/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Switch
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.databinding.FragmentNotActiveScheduleBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewNotActiveScheduleBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator

/**
 * Класс фрагмента, показывающий неактивные расписания. Практически идентичен фрагменту
 * AchievedGoalFragment, за тем исключение, что имеет активный switch.
 * @see NotActiveScheduleViewModel
 */
class NotActiveScheduleFragment : Fragment() {
    /**
     * ViewModel.
     */
    private val viewModel: NotActiveScheduleViewModel
    by lazy { ViewModelProvider(this).get(NotActiveScheduleViewModel::class.java) }
    /**
     * Привязка данных.
     */
    private lateinit var binding: FragmentNotActiveScheduleBinding

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentNotActiveScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        viewModel.schedules.observe(viewLifecycleOwner, {
            // Нельзя создавать новый адаптер, так как используется DiffUtil
            // для нахождения изменений данных.
            (binding.recyclerView.adapter as Adapter).setData(it)
        })

        return view
    }

    /**
     * Устанавливает видимым layout_empty, а layout_loading невидимым.
     */
    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    /**
     * Устанавливает layout_empty и layout_loading невидимыми.
     */
    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }

    /**
     * Класс holder-а для RecyclerView. Такой же, как и остальные холдеры.
     * Но есть важная особенность: для показа типа расписания использует string array,
     * в качестве индекса использует значение schedule.type
     */
    private inner class Holder(private val binding: ItemRecyclerViewNotActiveScheduleBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        /**
         * Расписание, которое показывает holder. Необходимо для передачи информации в ScheduleFragment.
         */
        private lateinit var schedule: Schedule

        /**
         * Инициализация холдера. Установка onClickListener на switch на сам холдер.
         */
        init {
            itemView.setOnClickListener(this)

            binding.activeSwitch.setOnClickListener {
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        schedule.isActive = true
                        viewModel.updateSchedule(schedule)
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                        (it as Switch).isChecked = false
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
        }

        /**
         * Установка содержимого holder-а.
         */
        fun bind(schedule: Schedule) {
            this.schedule = schedule
            binding.schedule = schedule

            // Очень странный баг. При нажатии на switch в паралелльном фрагменте
            // в этом фрагменте isChecked равно true.
            binding.activeSwitch.isChecked = false

            val array = resources.getStringArray(R.array.types_schedule)
            binding.type.text = array[schedule.type]
        }

        /**
         * Вызывается при нажатии на холдер. Перемещает пользователя в ScheduleFragment.
         */
        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("id", schedule.id)
            }
            v.findNavController().navigate(R.id.action_navigation_schedule_to_navigation_schedule, bundle)
        }
    }

    /**
     * Адаптер для RecyclerView. Обычный адаптер, за исключением того, что анимирует появление
     * holder-ов при их самом первом появлении на экране, и использованием DiffUtil для вычисления
     * изменений и их последующих визуализаций (появление, перемещние или удаление).
     * Также уведомляет пользователя, если RecyclerView пустой, посредством setEmptyView().
     */
    private inner class Adapter(var schedules: List<Schedule>): RecyclerView.Adapter<Holder>(){
        /**
         * Нужна для сохранения последней позиции holder-а, который увидил пользователь.
         * Используется для анимации.
         */
        private var lastPosition = -1

        /**
         * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
         */
        fun setData(newData: List<Schedule>){
            val diffUtilCallback = DiffUtilCallback(schedules, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            schedules = newData
            diffResult.dispatchUpdatesTo(this)

            if (schedules.isEmpty()) setEmptyView()
            else setNullView()
        }

        /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_not_active_schedule,
                parent, false))
        }

        override fun getItemCount() = schedules.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(schedules[position])

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
    private class DiffUtilCallback(private val oldList: List<Schedule>,
                                   private val newList: List<Schedule>): DiffUtil.Callback() {

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
     * Перемещения вверх или вниз запрещены, взмахи влево или вправо разрешены.
     */
    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        CustomItemTouchCallback(requireContext(),0,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
        /**
         * Адаптер RecyclerView в этом фрагменте. Нужен в функции onClickNegativeButton, чтобы
         * уведомить адаптер, что произошла отмена удаления и нужно вернуть holder на место.
         */
        private val mAdapter = binding.recyclerView.adapter!!

        /**
         * Выполняется при нажатии на кнопку "Yes". Удаляет выбранный элемент из базы данных
         * со всем деревом.
         */
        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder) {
            viewModel.deleteScheduleWithActions(viewModel.schedules.value!![viewHolder.adapterPosition])
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
}