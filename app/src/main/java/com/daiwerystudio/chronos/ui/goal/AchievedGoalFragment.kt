/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.databinding.FragmentAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewAchievedGoalBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator

/**
 * Класс фрагмента, показывающий завершенные цели. Практически идентичен фрагменту
 * ActionTypeFragment, за тем исключение, что состоит только из RecyclerView.
 * @see AchievedGoalViewModel
 */
class AchievedGoalFragment : Fragment() {
    /**
     * ViewModel.
     */
    private val viewModel: AchievedGoalViewModel
    by lazy { ViewModelProvider(this).get(AchievedGoalViewModel::class.java) }
    /**
     * Привязка данных.
     */
    private lateinit var binding: FragmentAchievedGoalBinding

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentAchievedGoalBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        viewModel.goals.observe(viewLifecycleOwner, {
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
     * Класс holder-а для RecyclerView. Никакие особенности не имеет.
     */
    private inner class Holder(private val binding: ItemRecyclerViewAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        /**
         * Цель, которую показывает holder. Необходима для передачи информации в GoalFragment.
         */
        private lateinit var goal: Goal

        /**
         * Инициализация холдера. Установка onClickListener на сам холдер.
         */
        init {
            itemView.setOnClickListener(this)
        }

        /**
         * Установка содержимого holder-а.
         */
        fun bind(goal: Goal) {
            this.goal = goal
            binding.goalName.text = goal.name
        }

        /**
         * Вызывается при нажатии на холдер. Перемещает пользователя в GoalFragment.
         */
        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
            v.findNavController().navigate(R.id.action_navigation_preview_goal_to_navigation_goal, bundle)
        }
    }

    /**
     * Адаптер для RecyclerView. Обычный адаптер, за исключением того, что анимирует появление
     * holder-ов при их самом первом появлении на экране, и использованием DiffUtil для вычисления
     * изменений и их последующих визуализаций (появление, перемещние или удаление).
     * Также уведомляет пользователя, если RecyclerView пустой, посредством setEmptyView().
     */
    private inner class Adapter(var goals: List<Goal>): RecyclerView.Adapter<Holder>(){
        /**
         * Нужна для сохранения последней позиции holder-а, который увидил пользователь.
         * Используется для анимации.
         */
        private var lastPosition = -1

        /**
         * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
         */
        fun setData(newData: List<Goal>){
            val diffUtilCallback = DiffUtilCallback(goals, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            goals = newData
            diffResult.dispatchUpdatesTo(this)

            if (goals.isEmpty()) setEmptyView()
            else setNullView()
        }

        /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_achieved_goal,
                parent, false))
        }

        override fun getItemCount() = goals.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(goals[position])

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
    private class DiffUtilCallback(private val oldList: List<Goal>,
                                   private val newList: List<Goal>): DiffUtil.Callback() {

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
            viewModel.deleteGoalWithChild(viewModel.goals.value!![viewHolder.adapterPosition])
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