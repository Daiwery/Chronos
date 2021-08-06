/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.action_type

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.databinding.FragmentChildActionTypeBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionTypeBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator

/**
 * Является практически полной копией ActionTypeFragment. За тем исключение, что не является
 * фрагментом первого уровня (поэтому есть стрелка "назад"), имеет свою ViewModel,
 * показывает дочерние типы действий и имеет возможность изменить родительный тип действия.
 * @see ActionTypeFragment
 * @see ChildActionTypeViewModel
 * @see ActionTypeDialog
 */
class ChildActionTypeFragment: Fragment() {
    /**
     * ViewModel.
     */
    private val viewModel: ChildActionTypeViewModel
            by lazy { ViewModelProvider(this).get(ChildActionTypeViewModel::class.java) }
    /**
     * Привязка данных.
     */
    private lateinit var binding: FragmentChildActionTypeBinding

    /**
     * Выполняется перед созданием UI. Здесь фрагмент получает idParent и передает его в ViewModel.
     * Это делается для того, чтобы при перевороте устройства данные заного не извлекались.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Это необходимо для созданию меню по макету.
        setHasOptionsMenu(true)

        // Если idParent не передался фрагменту, код вылетит с ошибкой.
        viewModel.idParent = arguments?.getString("idParent")
    }

    /**
     * Создание всего UI в фрагменте. Настройка RecyclerView, подписка на данные из viewModel
     * и настройка fab, при нажатии на которую возникает диалог создания типа действия.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChildActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        viewModel.parent.observe(viewLifecycleOwner, {
            (activity as AppCompatActivity).supportActionBar?.title = it.name
        })
        viewModel.actionTypes.observe(viewLifecycleOwner, {
            // Нельзя создавать новый адаптер, так как используется DiffUtil
            // для нахождения оптимизированных изменений данных.
            (binding.recyclerView.adapter as Adapter).setData(it)
        })


        binding.fab.setOnClickListener {
            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply {
                putSerializable("actionType", ActionType(parent = viewModel.parent.value!!.id))
                putBoolean("isCreated", true)
            }
            dialog.show(this.requireActivity().supportFragmentManager, "ActionTypeDialog")
        }

        return view
    }

    /**
     * Устанавливает видимым layout_empty, а layout_loading невидимым.
     */
    private fun setEmptyView() {
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    /**
     * Устанавливает layout_empty и layout_loading невидимыми.
     */
    private fun setNullView() {
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
     * Выполняется при нажатии на элемент в меню. В зависимости нажатого элемента, либо позволяет
     * изменить родительский тип действия, либо удалить его и его дерево.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                val dialog = ActionTypeDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("actionType", viewModel.parent.value!!)
                    putBoolean("isCreated", false)
                }
                dialog.show(this.requireActivity().supportFragmentManager, "ActionTypeDialog")

                return true
            }
            R.id.delete -> {
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteActionTypeWithChild(viewModel.parent.value!!)
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
     * Класс Holder-а для RecyclerView. Строится по item_recycler_view_action_type.
     * После нажатия на иконку edit появляется диалог с возможностью изменения
     * выбранного типа действия. В binding этого макета есть две переменные: actionType и
     * countChild. Первая нужна для установки цвета и имени. Вторая для установки дополнительных
     * данных при countChild != 0.
     */
    private inner class Holder(private val binding: ItemRecyclerViewActionTypeBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        /**
         * Тип действия, который показывает holder. Необходима для передачи информации диалогу,
         * какой тип действия нужно изменить, и ChildActionTypeFragment, от какого родителя
         * показывать дочерние типы действий.
         */
        private lateinit var actionType: ActionType

        /**
         * Инициализация холдера. Установка onClickListener на edit и на сам холдер.
         */
        init {
            itemView.setOnClickListener(this)

            binding.edit.setOnClickListener{
                val dialog = ActionTypeDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("actionType", actionType)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
            }
        }

        /**
         * Установка содержимого holder-а.
         */
        fun bind(actionType: ActionType) {
            this.actionType = actionType
            binding.actionType = actionType

            val countChild = viewModel.getCountChild(actionType.id)
            countChild.observe(viewLifecycleOwner, {
                binding.countChild = it
            })
        }

        /**
         * Вызывается при нажатии на холдер. Перемещает пользователя в новый ChildActionTypeFragment.
         */
        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", actionType.id)
            }
            v.findNavController().navigate(R.id.action_navigation_child_action_type_self, bundle)
        }
    }

    /**
     * Адаптер для RecyclerView. Обычный адаптер, за исключением того, что анимирует появление
     * holder-ов при их самом первом появлении на экране, и использованием DiffUtil для вычисления
     * изменений и их последующих визуализаций (появление, перемещние или удаление).
     * Также уведомляет пользователя, если RecyclerView пустой, посредством setEmptyView().
     */
    private inner class Adapter(var actionTypes: List<ActionType>): RecyclerView.Adapter<Holder>(){
        /**
         * Нужна для сохранения последней позиции holder-а, который увидил пользователь.
         * Используется для анимации.
         */
        private var lastPosition = -1

        /**
         * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
         */
        fun setData(newData: List<ActionType>){
            val diffUtilCallback = CustomDiffUtil(actionTypes, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            actionTypes = newData
            diffResult.dispatchUpdatesTo(this)

            if (actionTypes.isEmpty()) setEmptyView()
            else setNullView()
        }

        /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_action_type,
                parent, false))
        }

        override fun getItemCount() = actionTypes.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(actionTypes[position])

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
    private class CustomDiffUtil(private val oldList: List<ActionType>,
                           private val newList: List<ActionType>): DiffUtil.Callback() {

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
            viewModel.deleteActionTypeWithChild(viewModel.actionTypes.value!![viewHolder.adapterPosition])
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