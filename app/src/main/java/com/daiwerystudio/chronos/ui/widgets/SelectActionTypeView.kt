/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 28.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: теперь mActionTypes имеет тип List<Pair<String, ActionType>>, а не List<ActionType>.
* Это связано с добавлением Union. Небольшой рефакторинг.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.ui.union.CustomDiffUtil

/**
 * Виджет, позволяющий выбрать тип действия. Представляет из себя горизонтальные
 * RecyclerView, вложенный в один вертикальный RecyclerView. Взаимосвязь между компонентами
 * осуществляется с помощью интерфейсов.
 */
class SelectActionTypeView(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {
    /**
     * Типы действий, из которых нужно выбрать. Представляет из себя массив из соединненых
     * id родителя и типа действийя.
     */
    private var mActionTypes: List<Pair<String, ActionType>> = emptyList()

    /**
     * Массив с id типов действий. В каждой строчке показываются дети от родителя с id
     * из этого массива.
     */
    private var mIDs: MutableList<String> = mutableListOf("")
    var selectedActionType: ActionType? = null
        private set
    private var color: ImageView
    private var name: TextView
    private var isAll: CheckBox
    private var mRecyclerView: RecyclerView


    init {
        inflate(context, R.layout.layout_select_action_type, this)

        color = findViewById(R.id.imageView)
        color.visibility = View.INVISIBLE
        name = findViewById(R.id.textView1)
        isAll = findViewById(R.id.isAll)
        isAll.setOnClickListener { mOnEditIsAllListener?.editIsALl(isAll.isChecked) }

        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(mIDs)
        }
    }

    /* Интерфейс для события выбора типа действия. */
    private var mOnSelectListener: OnSelectListener? = null
    fun interface OnSelectListener{
        fun onSelect(actionType: ActionType)
    }
    fun setOnSelectListener(onSelectListener: OnSelectListener) {
        mOnSelectListener = onSelectListener
    }

    /* Интерфейс для события изменения isAll. */
    private var mOnEditIsAllListener: OnEditIsAllListener? = null
    fun interface OnEditIsAllListener{
        fun editIsALl(isAll: Boolean)
    }
    fun setOnEditIsAllListener(onEditIsAllListener: OnEditIsAllListener){
        mOnEditIsAllListener = onEditIsAllListener
    }

    /**
     * Устанавливает типы действий, из которых нужно выбрать пользователю.
     */
    fun setData(actionTypes: List<Pair<String, ActionType>>){
        mActionTypes = actionTypes
        // Устанавливаем mIDs в начальное значение.
        mIDs = mutableListOf("")
        (mRecyclerView.adapter as Adapter).setData(mIDs)
        // После уведовляем, что могли изменится данные.
        mRecyclerView.adapter?.notifyItemRangeChanged(0, mIDs.size)
    }

    /**
     * Устанавливает выбранный тип действия.
     */
    fun setSelectedActionType(actionType: ActionType){
        selectedActionType = actionType

        color.visibility = View.VISIBLE
        color.setColorFilter(actionType.color)
        name.text = actionType.name
    }


    /*  После выбора типа действия, нужно удалить все строки после него и добавить новую строку. */
    private fun updateIDs(position: Int, actionType: ActionType){
        val size = mIDs.size
        for (i in position+1 until size){
            mIDs.removeAt(position+1)
        }
        mIDs.add(actionType.id)

        (mRecyclerView.adapter as Adapter).setData(mIDs)
        mRecyclerView.scrollToPosition(position+1)

        setSelectedActionType(actionType)
        mOnSelectListener?.onSelect(actionType)
    }


    /**
     * Адаптер для вертикального RecyclerView.
     */
    private inner class Adapter(var ids: List<String>): RecyclerView.Adapter<Holder>(){
        fun setData(newData: List<String>){
            val diffUtilCallback = DiffUtilString(ids, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            // Необходимо скопировать значения, чтобы значения не менялись сами до этого момента.
            ids = newData.map{ it }
            diffResult.dispatchUpdatesTo(this)
        }

        override fun getItemCount() = ids.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val holder = Holder(LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_line_select_action_type, parent, false))
            holder.setOnSelectListener{ position, actionType ->
                updateIDs(position, actionType)
            }
            return holder
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(mActionTypes, ids[position])
        }
    }

    /**
     * Внутри этого holder-а находится горизонтальный RecyclerView.
     */
    private class Holder(view: View): RecyclerView.ViewHolder(view){
        private var recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        private var emptyView: View = view.findViewById(R.id.empty_view)

        init{
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = ActionTypeAdapter(emptyList())
            }
            (recyclerView.adapter as ActionTypeAdapter).setOnSelectListener{
                mOnSelectListener?.onSelect(absoluteAdapterPosition, it)
            }
        }

        fun bind(rawActionTypes: List<Pair<String, ActionType>>, id: String) {
            var actionTypes = rawActionTypes.filter { it.first == id }.map { it.second }
            actionTypes = actionTypes.sortedBy { it.name }

            if (actionTypes.isEmpty()) emptyView.visibility = View.VISIBLE
            else emptyView.visibility = View.GONE

            (this.recyclerView.adapter as ActionTypeAdapter).setData(actionTypes)
        }

        /* Интерфейс для события выбора типа действия. */
        private var mOnSelectListener: OnSelectListener? = null
        fun interface OnSelectListener{
            fun onSelect(position: Int, actionType: ActionType)
        }
        fun setOnSelectListener(onSelectListener: OnSelectListener){
            mOnSelectListener = onSelectListener
        }
    }

    /**
     * Адаптер для горизонтального RecyclerView.
     */
    private class ActionTypeAdapter(var actionTypes: List<ActionType>):
        RecyclerView.Adapter<ActionTypeHolder>() {
        /**
         * Позиция последнего выбранного типа действия в этой строке.
         */
        private var mSelectPosition: Int = -1

        fun setData(newData: List<ActionType>) {
            val diffUtilCallback = CustomDiffUtil(actionTypes, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            mSelectPosition = -1
            actionTypes = newData.map{ it.copy() }
            diffResult.dispatchUpdatesTo(this)
        }

        override fun getItemCount() = actionTypes.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionTypeHolder {
            val holder = ActionTypeHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_item_select_action_type, parent, false))
            holder.setOnSelectListener{position, actionType ->
                val oldSelect = mSelectPosition
                mSelectPosition = position

                notifyItemChanged(mSelectPosition)
                notifyItemChanged(oldSelect)

                mOnSelectListener?.select(actionType)
            }
            return holder
        }

        override fun onBindViewHolder(holder: ActionTypeHolder, position: Int) {
            holder.bind(actionTypes[position])

            if (position == mSelectPosition) holder.setVisibilityFrame(View.VISIBLE)
            else holder.setVisibilityFrame(View.INVISIBLE)
        }

        /* Интерфейс для события выбора типа действия. */
        private var mOnSelectListener: OnSelectListener? = null
        fun interface OnSelectListener{
            fun select(actionType: ActionType)
        }
        fun setOnSelectListener(onSelectListener: OnSelectListener){
            mOnSelectListener = onSelectListener
        }
    }


    private class ActionTypeHolder(view: View): RecyclerView.ViewHolder(view){
        private lateinit var actionType: ActionType
        private val color: ImageView  = view.findViewById(R.id.imageView)
        private val name: TextView = view.findViewById(R.id.textView1)
        private val frame: ImageView = view.findViewById(R.id.frame)
//        private val countChild: TextView = view.findViewById(R.id.textView3)

        init {
            itemView.setOnClickListener{
                mOnSelectListener?.select(absoluteAdapterPosition, actionType)
            }
        }

        fun bind(actionType: ActionType) {
            this.actionType = actionType

            color.setColorFilter(actionType.color)
            name.text = actionType.name

//                val count = mActionTypes.count{ it.first == actionType.id }
//                if (count != 0){
//                    countChild.visibility = View.VISIBLE
//                    countChild.text = (resources.getString(R.string.inside_)+" "+count.toString())
//                } else countChild.visibility = View.GONE
        }

        fun setVisibilityFrame(visibility: Int){
            frame.visibility = visibility
        }

        /* Интерфейс для события выбора типа действия. */
        private var mOnSelectListener: OnSelectListener? = null
        fun interface OnSelectListener{
            fun select(position: Int, actionType: ActionType)
        }
        fun setOnSelectListener(onSelectListener: OnSelectListener){
            mOnSelectListener = onSelectListener
        }
    }


    private class DiffUtilString(private val oldList: List<String>,
                                 private val newList: List<String>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }
}