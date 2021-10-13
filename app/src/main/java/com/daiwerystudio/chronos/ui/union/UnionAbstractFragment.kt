/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.union

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.*
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.daiwerystudio.chronos.ui.folder.FolderDialog
import com.daiwerystudio.chronos.ui.goal.GoalDialog
import com.daiwerystudio.chronos.ui.reminder.ReminderDialog
import com.daiwerystudio.chronos.ui.schedule.ScheduleDialog
import java.util.*

/**
 * Абстрактный класс для union фрагмента.
 */
abstract class UnionAbstractFragment : Fragment() {
    abstract val viewModel: UnionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.information.setData(arguments?.getString("parentID") ?: "",
            null, null)
    }

    override fun onStop() {
        super.onStop()
        viewModel.updateUnions()
    }


    @SuppressLint("ClickableViewAccessibility")
    open inner class ActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding):
        ActionTypeAbstractHolder(binding, requireActivity().supportFragmentManager) {

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN)
                    itemTouchHelper.startDrag(this)
                false
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }
        }

        override fun onClicked() {
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val bundle = Bundle().apply {
                    putString("parentID", actionType.id)
                }
                itemView.transitionName = actionType.id
                val extras = FragmentNavigatorExtras(itemView to actionType.id)
                itemView.findNavController()
                    .navigate(R.id.action_global_navigation_union_action_type, bundle, null, extras)
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    open inner class GoalHolder(binding: ItemRecyclerViewGoalBinding) :
        GoalAbstractHolder(binding, requireActivity().supportFragmentManager){

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN)
                    itemTouchHelper.startDrag(this)
                false
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }
        }

        override fun onAchieved() {
            // Копируем, чтобы recyclerView смог засечь изменения.
            val mGoal = goal.copy()
            mGoal.isAchieved = binding.checkBox.isChecked
            if (binding.checkBox.isChecked) viewModel.setAchievedGoalWithChild(goal.id)
            else viewModel.updateGoal(mGoal)
        }

        override fun setPercentAchieved() {
            // Есть подозрение, что здесь утечка памяти.
            val existence = viewModel.existenceGoals(goal.id)
            existence.observe(viewLifecycleOwner, {
                if (it) {
                    binding.textView21.visibility = View.VISIBLE
                    binding.progressTextView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE

                    val percent = viewModel.getPercentAchieved(goal.id)
                    percent.observe(viewLifecycleOwner, { p ->
                        binding.progressBar.progress = p
                        binding.progressTextView.text = ("$p%")
                    })
                }
            })
        }

        override fun onClicked() {
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val bundle = Bundle().apply {
                    putString("parentID", goal.id)
                }
                itemView.transitionName = goal.id
                val extras = FragmentNavigatorExtras(itemView to goal.id)
                itemView.findNavController()
                    .navigate(R.id.action_global_navigation_union_goal, bundle, null, extras)
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    open inner class ScheduleHolder(binding: ItemRecyclerViewScheduleBinding):
        ScheduleAbstractHolder(binding, requireActivity().supportFragmentManager) {

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN)
                    itemTouchHelper.startDrag(this)
                false
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }
        }
        override fun onActive() {
            schedule.isActive = !schedule.isActive
            viewModel.updateSchedule(schedule)
        }

        override fun onClicked() {
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val bundle = Bundle().apply {
                    putString("parentID", schedule.id)
                }
                itemView.transitionName = schedule.id
                val extras = FragmentNavigatorExtras(itemView to schedule.id)
                itemView.findNavController()
                    .navigate(R.id.action_global_navigation_union_schedule, bundle, null, extras)
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    open inner class NoteHolder(binding: ItemRecyclerViewNoteBinding):
        NoteAbstractHolder(binding) {

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN)
                    itemTouchHelper.startDrag(this)
                false
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }
        }
        override fun onClicked() {
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val bundle = Bundle().apply {
                    putString("parentID", note.id)
                }
                itemView.transitionName = note.id
                val extras = FragmentNavigatorExtras(itemView to note.id)
                itemView.findNavController()
                    .navigate(R.id.action_global_navigation_union_note, bundle, null, extras)
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    open inner class ReminderHolder(binding: ItemRecyclerViewReminderBinding):
        ReminderAbstractHolder(binding, requireActivity().supportFragmentManager){

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN)
                    itemTouchHelper.startDrag(this)
                false
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }
        }

        override fun onClicked() {
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val dialog = ReminderDialog()
                dialog.arguments = Bundle().apply {
                    putSerializable("reminder", reminder)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "ReminderDialog")
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    open inner class FolderHolder(binding: ItemRecyclerViewFolderBinding):
        FolderAbstractHolder(binding, requireActivity().supportFragmentManager) {

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN)
                    itemTouchHelper.startDrag(this)
                false
            }
            itemView.setOnLongClickListener {
                startActionMode()
                changeItem(absoluteAdapterPosition)
                true
            }
        }

        override fun onClicked() {
            if (actionMode == null) {
                // Восстанавливаем анимацию клика на холдер.
                itemView.isClickable = true

                val bundle = Bundle().apply {
                    putString("parentID", folder.id)
                }
                itemView.transitionName = folder.id
                val extras = FragmentNavigatorExtras(itemView to folder.id)
                itemView.findNavController()
                    .navigate(R.id.action_global_navigation_union_folder, bundle, null, extras)
            } else {
                // Убираем анимацию клика на холдер.
                itemView.isClickable = false
                changeItem(absoluteAdapterPosition)
            }
        }
    }

    abstract fun setEmptyView()
    abstract fun setNullView()

    open inner class UnionAdapter: UnionAbstractAdapter(emptyList(), layoutInflater){
        override fun updateData(newData: List<Pair<Int, ID>>) {
            super.updateData(newData)

            if (data.isEmpty()) setEmptyView()
            else setNullView()
        }

        override fun createActionTypeHolder(binding: ItemRecyclerViewActionTypeBinding): RawHolder =
            ActionTypeHolder(binding)

        override fun createGoalHolder(binding: ItemRecyclerViewGoalBinding): RawHolder =
            GoalHolder(binding)

        override fun createScheduleHolder(binding: ItemRecyclerViewScheduleBinding): RawHolder =
            ScheduleHolder(binding)

        override fun createNoteHolder(binding: ItemRecyclerViewNoteBinding): RawHolder =
            NoteHolder(binding)

        override fun createReminderHolder(binding: ItemRecyclerViewReminderBinding): RawHolder =
            ReminderHolder(binding)

        override fun createFolderHolder(binding: ItemRecyclerViewFolderBinding): RawHolder =
            FolderHolder(binding)

        /* Если позиция холедра равна dragFromPosition или dragToPosition, или она есть в selectedItems,
        то мы с ним ничего не делаем. Если же нет, то изменям прозрачность.*/
        override fun onBindViewHolder(holder: RawHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isNotEmpty())
                if (payloads.last() is Boolean)
                    if (payloads.last() as Boolean)
                        if (dragFromPosition != -1 && dragToPosition != -1)
                            if (position == dragFromPosition || position == dragToPosition)
                                holder.itemView.alpha = 1f
                            else holder.itemView.alpha = 0.5f
                        else if (selectedItems.isNotEmpty())
                                if (position in selectedItems) holder.itemView.alpha = 1f
                                else holder.itemView.alpha = 0.5f
                            else holder.itemView.alpha = 1f
                    else holder.itemView.alpha = 1f
                else super.onBindViewHolder(holder, position, payloads)
            else super.onBindViewHolder(holder, position, payloads)
        }
    }


    fun createUnionItem(item: Int){
        when (item){
            TYPE_ACTION_TYPE -> {
                val id = UUID.randomUUID().toString()
                val actionType = ActionType(id=id)
                val union = Union(id=id, parent=viewModel.information.parentID,
                    indexList=viewModel.data.value!!.size, type=TYPE_ACTION_TYPE)

                val dialog = ActionTypeDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("actionType", actionType)
                    putSerializable("union", union)
                    putBoolean("isCreated", true)
                }
                dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
            }

            TYPE_GOAL -> {
                val id = UUID.randomUUID().toString()
                val goal = Goal(id=id)
                val union = Union(id=id, parent=viewModel.information.parentID,
                    indexList=viewModel.data.value!!.size, type=TYPE_GOAL)

                val dialog = GoalDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("goal", goal)
                    putSerializable("union", union)
                    putBoolean("isCreated", true)
                }
                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
            }

            TYPE_SCHEDULE -> {
                val id = UUID.randomUUID().toString()
                val schedule = Schedule(id=id, type=TYPE_SCHEDULE_PERIODIC)
                val union = Union(id=id, parent=viewModel.information.parentID,
                    indexList=viewModel.data.value!!.size, type=TYPE_SCHEDULE)

                val dialog = ScheduleDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("schedule", schedule)
                    putSerializable("union", union)
                    putBoolean("isCreated", true)
                }
                dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")
            }

            TYPE_NOTE -> {
                val id = UUID.randomUUID().toString()
                val note = Note(id=id)
                val union = Union(id=id, parent=viewModel.information.parentID,
                    indexList=viewModel.data.value!!.size, type=TYPE_NOTE)

                val bundle = Bundle().apply{
                    putSerializable("note", note)
                    putSerializable("union", union)
                }
                view?.findNavController()?.navigate(R.id.action_global_navigation_note, bundle)
            }

            TYPE_REMINDER -> {
                val id = UUID.randomUUID().toString()
                val reminder = Reminder(id=id)
                val union = Union(id=id, parent=viewModel.information.parentID,
                    indexList=viewModel.data.value!!.size, type=TYPE_REMINDER)

                val dialog = ReminderDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("reminder", reminder)
                    putSerializable("union", union)
                    putBoolean("isCreated", true)
                }
                dialog.show(requireActivity().supportFragmentManager, "ReminderDialog")
            }

            TYPE_FOLDER -> {
                val id = UUID.randomUUID().toString()
                val folder = Folder(id=id)
                val union = Union(id=id, parent=viewModel.information.parentID,
                    indexList=viewModel.data.value!!.size, type=TYPE_FOLDER)

                val dialog = FolderDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("folder", folder)
                    putSerializable("union", union)
                    putBoolean("isCreated", true)
                }
                dialog.show(requireActivity().supportFragmentManager, "FolderDialog")
            }
           else -> throw IllegalArgumentException("Invalid type")
        }
    }


    // ПРЕДУПРЕЖДЕНИЕ! Инициализация должна происходить после инициализации information в UnionViewModel.
    val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : UnionSimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or if (viewModel.information.parentID != "") ItemTouchHelper.RIGHT else 0){

            override fun getMovementFlags(recyclerView: RecyclerView,
                                          viewHolder: RecyclerView.ViewHolder): Int {
                return if (actionMode != null || viewModel.information.filterString != null ||
                    viewModel.information.filterType != null) 0
                else super.getMovementFlags(recyclerView, viewHolder)
            }
        }

        simpleItemTouchCallback.backgroundRight = ColorDrawable(Color.parseColor("#CA0000"))
        simpleItemTouchCallback.iconRight = ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_baseline_delete_24)?.apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        simpleItemTouchCallback.backgroundLeft = ColorDrawable(Color.parseColor("#0071D5"))
        simpleItemTouchCallback.iconLeft = ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_baseline_arrow_upward_24)?.apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }

        simpleItemTouchCallback.setSwipeItemListener(object : UnionSimpleCallback.SwipeListener{
            override fun swipeLeft(position: Int) {
                AlertDialog.Builder(context, R.style.Style_AlertDialog)
                    .setTitle(R.string.are_you_sure)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteUnionWithChild(viewModel.data.value!![position].second.id)
                        Toast.makeText(requireContext(), R.string.text_toast_delete, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.no){ _, _ -> }
                    .setCancelable(false).create().show()
            }

            override fun swipeRight(position: Int) {
                AlertDialog.Builder(context, R.style.Style_AlertDialog)
                    .setTitle(R.string.are_you_sure)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.moveUnionUp(position)
                        Toast.makeText(requireContext(), R.string.text_toast_move, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.no){ _, _ -> }
                    .setCancelable(false).create().show()
            }
        })
        simpleItemTouchCallback.setMoveListener{ fromPosition, toPosition ->
            viewModel.swap(fromPosition, toPosition)
        }
        simpleItemTouchCallback.setDragItemListener{ fromPosition, toPosition ->
            AlertDialog.Builder(context, R.style.Style_AlertDialog)
                .setTitle(R.string.are_you_sure)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.editParentUnion(fromPosition, toPosition)
                    Toast.makeText(requireContext(), R.string.text_toast_move, Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(R.string.no){ _, _ -> }
                .setCancelable(false).create().show()
        }
        simpleItemTouchCallback.setDraggindViewHolderSetter(object : UnionSimpleCallback.DraggindViewHolderSetter{
            override fun startDrag(dragFromViewHolder: RecyclerView.ViewHolder,
                                   dragToViewHolder: RecyclerView.ViewHolder) {
                dragFromPosition = dragFromViewHolder.absoluteAdapterPosition
                dragToPosition = dragToViewHolder.absoluteAdapterPosition
                notifyAdapterItemsChange(true)
            }

            override fun endDrag() {
                dragFromPosition = -1
                dragToPosition = -1
                notifyAdapterItemsChange(false)
            }
        })

        ItemTouchHelper(simpleItemTouchCallback)
    }

    /* Если позиция холедра равна dragFromPosition или dragToPosition, то мы с ним ничего не делаем.
     Если же нет, то изменям прозрачность. */
    private var dragFromPosition: Int = -1
    private var dragToPosition: Int = -1
    // Нужна функция, которая будет сообщать адаптеру, что нужно перерисовать все холдеры.
    abstract fun notifyAdapterItemsChange(payload: Boolean)


    // Будем хранить позиции выбранных холдеров.
    private val selectedItems: MutableList<Int> = mutableListOf()
    var actionMode: ActionMode? = null
        private set

    private fun startActionMode(){
        actionMode = requireActivity().startActionMode(callback)
        actionMode?.title = "0"
    }

    private fun changeItem(position: Int){
        val index = selectedItems.indexOf(position)
        if (index == -1) selectedItems.add(position)
        else selectedItems.removeAt(index)

        notifyAdapterItemsChange(true)
        actionMode?.title = selectedItems.size.toString()

        if (selectedItems.size == 0) actionMode?.finish()
    }

    abstract fun hideFab()
    abstract fun showFab()

    // ПРЕДУПРЕЖДЕНИЕ! Инициализация должна происходить после инициализации information в UnionViewModel.
    private val callback by lazy {
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.menu_action_bar, menu)
                hideFab()

                if (viewModel.information.parentID == "")
                    menu?.findItem(R.id.up)?.isVisible = false
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.up -> {
                        AlertDialog.Builder(context, R.style.Style_AlertDialog)
                            .setTitle(R.string.are_you_sure)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                // Нужно передать скопированное значение, из-за того, что
                                // после этот массив удалится, а действия внутри функции выполняются
                                // в отдельном потоке.
                                viewModel.moveUnionsUp(selectedItems.map { it })
                                Toast.makeText(requireContext(), R.string.text_toast_move, Toast.LENGTH_LONG).show()
                                actionMode?.finish()
                            }
                            .setNegativeButton(R.string.no){ _, _ ->
                                actionMode?.finish()
                            }
                            .setCancelable(false).create().show()
                        true
                    }
                    R.id.delete -> {
                        AlertDialog.Builder(context, R.style.Style_AlertDialog)
                            .setTitle(R.string.are_you_sure)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                viewModel.deleteUnionsWithChild(selectedItems)
                                Toast.makeText(requireContext(), R.string.text_toast_delete, Toast.LENGTH_LONG).show()
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
                notifyAdapterItemsChange(false)
                showFab()
            }
        }
    }
}