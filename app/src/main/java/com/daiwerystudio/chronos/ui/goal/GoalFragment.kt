/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
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
import com.daiwerystudio.chronos.databinding.FragmentGoalBinding
import com.daiwerystudio.chronos.databinding.ItemProgressGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewNotAchievedGoalBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator
import java.util.*

/**
 * Класс фрагмента, который видит пользователь при нажатии на любую цель. Основная логика
 * такая же, как и в остальный фрагментах. Но есть несколько очень важдных особенностей.
 * Во-первых, в Recycler View показываются два типа holder-ов: выполненная и невыполненная цели.
 * Во-вторых, вверху фргамента есть "виджет", который показывает прогресс выполнения цели. Он
 * представляет из себя еще один RecyclerView, имеющий holder-ы специального вида.
 *
 * Возможная модификация: создать отдельный виджет, как наследник от View, для визуализации
 * прогресса цели, а не использовать RecyclerView. Основная причина этому, это то, что так
 * будет правильнее. А допольнительная: маленький зазор между элементами.
 * @see GoalViewModel
 */
class GoalFragment : Fragment() {
    /**
     * ViewModel.
     */
    private val viewModel: GoalViewModel
    by lazy { ViewModelProvider(this).get(GoalViewModel::class.java) }
    /**
     * Привязка данных.
     */
    private lateinit var binding: FragmentGoalBinding

    /**
     * Выполняется перед созданием UI. Здесь фрагмент получает idParent и передает его в ViewModel.
     * Это делается для того, чтобы при перевороте устройства данные заного не извлекались.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Это необходимо для созданию меню по макету.
        setHasOptionsMenu(true)

        viewModel.idParent = arguments?.getString("idParent")
    }

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentGoalBinding.inflate(inflater, container, false)
        val view = binding.root

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.goal = it
            (activity as AppCompatActivity).supportActionBar?.title = it.name
        })


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        binding.progressGoal.apply{
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = ProgressGoalAdapter(emptyList())
        }


        viewModel.goals.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).setData(it)
            (binding.progressGoal.adapter as ProgressGoalAdapter).setData(it)
        })


        binding.fab.setOnClickListener{
            val dialog = GoalDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("goal", Goal(parent=viewModel.parent.value!!.id,
                    indexList=viewModel.goals.value!!.size))
                putBoolean("isCreated", true)
            }
            dialog.show(this.requireActivity().supportFragmentManager, "GoalTypeDialog")
        }


        binding.isAchieved.setOnClickListener {
            viewModel.updateListGoals(viewModel.goals.value!!)
            if ((it as CheckBox).isChecked){
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.setAchievedGoalWithChild(viewModel.parent.value!!)
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                        it.isChecked = false
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            } else {
                val goal = viewModel.parent.value!!
                goal.isAchieved = false
                viewModel.updateGoal(goal)
            }
        }


        binding.goalNote.addTextChangedListener {
            val goal = viewModel.parent.value!!
            goal.note = binding.goalNote.text.toString()
            viewModel.updateGoal(goal)
        }

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
     * Уставнавливает меню и заполняет его по menu.edit_delete_menu.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_delete_menu, menu)
    }

    /**
     * Выполняется при нажатии на элемент в меню. В зависимости нажатого элемента либо позволяет
     * изменить родительский тип действия, либо удалить его и его дерево.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                viewModel.updateListGoals(viewModel.goals.value!!)

                val dialog = GoalDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("goal", viewModel.parent.value!!)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")

                return true
            }
            R.id.delete -> {
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteGoalWithChild(viewModel.parent.value!!)
                        requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                    }
                    .setNegativeButton(R.string.no){ _, _ -> }
                    .setCancelable(false)
                    .create()
                    .show()

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Копия holder-а из AchievedGoalFragment. Испльзуется для визуализации выполненных целей.
     */
    private inner class AchievedGoalHolder(private val binding: ItemRecyclerViewAchievedGoalBinding):
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
         * Вызывается при нажатии на холдер. Перемещает пользователя в новый GoalFragment.
         */
        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
            v.findNavController().navigate(R.id.action_navigation_goal_self, bundle)
        }
    }

    /**
     * Копия holder-а из NotAchievedGoalFragment. Испльзуется для визуализации невыполненных целей.
     */
    private inner class NotAchievedGoalHolder(private val binding: ItemRecyclerViewNotAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        /**
         * Цель, которую показывает holder. Необходима для передачи информации в GoalFragment.
         */
        private lateinit var goal: Goal

        /**
         * Инициализация холдера. Установка onClickListener на checkBox, edit и на сам холдер.
         */
        init {
            itemView.setOnClickListener(this)

            binding.edit.setOnClickListener{
                viewModel.updateListGoals(viewModel.goals.value!!)
                val dialog =GoalDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("goal", goal)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
            }

            binding.checkBox.setOnClickListener {
                viewModel.updateListGoals(viewModel.goals.value!!)
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.setAchievedGoalWithChild(goal)
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                        (it as CheckBox).isChecked = false
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
        }

        /**
         * Установка содержимого holder-а.
         */
        fun bind(goal: Goal) {
            this.goal = goal
            binding.goal = goal

            val percent = viewModel.getPercentAchieved(goal.id)
            percent.observe(viewLifecycleOwner, {
                if (it != null) binding.progressBar.progress = it
            })
        }

        /**
         * Вызывается при нажатии на холдер. Перемещает пользователя в новый GoalFragment.
         */
        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
            v.findNavController().navigate(R.id.action_navigation_goal_self, bundle)
        }
    }


    /**
     * Адаптер для RecyclerView. Обычный адаптер, за исключением того, что анимирует появление
     * holder-ов при их самом первом появлении на экране, использованием DiffUtil для вычисления
     * изменений и их последующих визуализаций (появление, перемещние или удаление) и тем, что
     * показываем два ивда holder-а.
     * Также уведомляет пользователя, если RecyclerView пустой, посредством setEmptyView().
     */
    private inner class Adapter(var goals: List<Goal>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
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

        /**
         * С помощью этой функции адаптер определяет, какой тип холдера нужно создать
         * или использовать.
         */
        override fun getItemViewType(position: Int): Int {
            return if (goals[position].isAchieved) TYPE_ACHIEVED_GOAL
            else TYPE_NOT_ACHIEVED_GOAL
        }

        /*  Ниже представлены стандартные функции адаптера, с той лишь разницей,
        что обрабатывают разные типа holder-а.  См. оф. документацию. */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
            return when (viewType){
                TYPE_ACHIEVED_GOAL -> AchievedGoalHolder(
                    DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_achieved_goal,
                    parent,
                    false))

                TYPE_NOT_ACHIEVED_GOAL -> NotAchievedGoalHolder(
                    DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_not_achieved_goal,
                    parent,
                    false))
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun getItemCount() = goals.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)){
                TYPE_ACHIEVED_GOAL -> (holder as AchievedGoalHolder).bind(goals[position])
                TYPE_NOT_ACHIEVED_GOAL -> (holder as NotAchievedGoalHolder).bind(goals[position])
                else -> throw IllegalArgumentException("Invalid view type")
            }

            if (holder.adapterPosition > lastPosition){
                lastPosition = holder.adapterPosition

                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_add_item)
                holder.itemView.startAnimation(animation)
            }
        }
    }

    companion object {
        private const val TYPE_ACHIEVED_GOAL = 1
        private const val TYPE_NOT_ACHIEVED_GOAL = 2
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

            viewModel.goals.value!![from].indexList = to
            viewModel.goals.value!![to].indexList = from

            Collections.swap(viewModel.goals.value!!, from, to)

            mAdapter.notifyItemMoved(from, to)

            // Вызывается изменение элемента, а не их перемещение. Это из-за особенностей
            // ProgressGoal у последних элементов.
            // Намек на создание отдельного виджета.
            binding.progressGoal.adapter!!.notifyItemChanged(from)
            binding.progressGoal.adapter!!.notifyItemChanged(to)

            return true
        }

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

    /**
     * Выполняет, когда фрагмент перестает работать. Сохраняет все indexList,
     * требуемые пользователем.
     */
    override fun onPause() {
        super.onPause()
        viewModel.updateListGoals(viewModel.goals.value!!)
    }

    /**
     * Это такой же DiffUtil, как все остальные, но с той модификацией, что для последнего
     * и предпоследнего элемента в зависимости от размером массивов выдает false. Это нужно,
     * чтобы эти элементы обновлись, так как у последнего элемента отсутствует правый мост.
     */
    private class CallbackProgressBar(private val oldList: List<Goal>,
                                   private val newList: List<Goal>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return if (newListSize > oldListSize && newPosition==newListSize-2) false
            else if (newListSize < oldListSize && newPosition==newListSize-1) false
            else oldList[oldPosition] == newList[newPosition]
        }
    }

    /**
     * Holder для визуализации прогресса целей. Макет состоит из TextView и двух Drawable,
     * которые могут менять свой цвет в зависимости от состояния.
     */
    private inner class ProgressGoalHolder(private val binding: ItemProgressGoalBinding):
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
        fun bind(goal: Goal, last: Boolean) {
            this.goal = goal
            binding.goal = goal

            if (last) binding.bridge.visibility = View.GONE
            else binding.bridge.visibility = View.VISIBLE
        }

        /**
         * Вызывается при нажатии на холдер. Перемещает пользователя в GoalFragment.
         */
        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
            v.findNavController().navigate(R.id.action_navigation_goal_self, bundle)
        }
    }

    /**
     * Адаптер для ProgressGal. Обычный адаптер, за исключением того, что анимирует появление
     * holder-ов при их самом первом появлении на экране, и использованием DiffUtil для вычисления
     * изменений и их последующих визуализаций (появление, перемещние или удаление).
     */
    private inner class ProgressGoalAdapter(var goals: List<Goal>): RecyclerView.Adapter<ProgressGoalHolder>(){
        /**
         * Нужна для сохранения последней позиции holder-а, который увидил пользователь.
         * Используется для анимации.
         */
        private var lastPosition = -1

        /**
         * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
         */
        fun setData(newData: List<Goal>){
            val diffUtilCallback = CallbackProgressBar(goals, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            goals = newData
            diffResult.dispatchUpdatesTo(this)
        }

        /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressGoalHolder{
            return ProgressGoalHolder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_progress_goal,
                    parent,
                    false))
        }

        override fun getItemCount() = goals.size

        override fun onBindViewHolder(holder: ProgressGoalHolder, position: Int) {
            holder.bind(goals[position], position==itemCount-1)

            // Animation
            if (holder.adapterPosition > lastPosition){
                lastPosition = holder.adapterPosition

                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
                holder.itemView.startAnimation(animation)
            }
        }
    }
}