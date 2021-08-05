package com.daiwerystudio.chronos.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.ui.actiontype.ActionTypeDialog
import java.util.*


class SelectActionTypeView(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {
    // Array ids parent action types
    private var ids: MutableList<String> = mutableListOf("")
    // UI
    private var color: ImageView
    private var name: TextView
    private var recyclerView: RecyclerView
    // Database
    private val actionTypeRepository = ActionTypeRepository.get()
    private var selectActionType: ActionType? = null


    init {
        inflate(context, R.layout.layout_select_action_type, this)

        color = findViewById(R.id.color)
        color.visibility = View.INVISIBLE
        name = findViewById(R.id.name)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(ids)
        }
    }


    // Interface
    private var mOnSelectListener: OnSelectListener? = null
    interface OnSelectListener{
        fun onSelect(actionType: ActionType)
    }
    fun setOnSelectListener(onSelectListener: OnSelectListener?) {
        this.mOnSelectListener = onSelectListener
    }

    private var mOnClickAddListener: OnClickAddListener? = null
    interface OnClickAddListener{
        fun onClickAdd(id: String)
    }
    fun setOnClickAddListener(onClickAddListener: OnClickAddListener?) {
        this.mOnClickAddListener = onClickAddListener
    }




    fun setSelectActionType(id: String){
        // Set data
        val liveData = actionTypeRepository.getActionType(id)
        liveData.observeForever { actionType ->
            if (actionType == null) throw IllegalStateException("Invalid id")

            if (selectActionType == null) selectActionType = actionType
            color.visibility = View.VISIBLE
            color.setColorFilter(selectActionType!!.color)
            name.text = selectActionType!!.name
        }

    }



    private fun updateIds(position: Int, actionType: ActionType){
        // Сперва удаляем все, что после выбранной строки
        val size = ids.size
        for (i in position+1 until size){
            // Delete id
            ids.removeAt(position+1)
        }
        // Add new id
        ids.add(actionType.id)

        // Update select action type
        color.visibility = View.VISIBLE
        color.setColorFilter(actionType.color)
        name.text = actionType.name
        selectActionType = actionType

        // Update recyclerView
        (recyclerView.adapter as Adapter).setData(ids)
        recyclerView.scrollToPosition(position+1)

        // Notify
        mOnSelectListener?.onSelect(actionType)
    }


    // Ну не шарю я в ООП, ну и что
    private class DiffUtilString(private val oldList: List<String>,
                                   private val newList: List<String>): DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return true
        }
    }
    private class DiffUtilActionType(private val oldList: List<ActionType>,
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



    // Суть в том, что этот recyclerView будет состоять из горизонтальных recyclerView, содержащих action types
    private inner class Adapter(var ids: List<String>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun getItemCount() = ids.size

        fun setData(newData: List<String>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilString(ids, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update and copy data
            ids = newData.map{ it }
            // Notify
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
            return Holder(LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_line_select_action_type, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as Holder).bind(ids[position])
        }

        fun select(position: Int, actionType: ActionType){
            // Отправляем выбранный элемент дальше
            updateIds(position, actionType)
        }


        private inner class Holder(view: View): RecyclerView.ViewHolder(view){
            private var recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
            private var emptyView: View = view.findViewById(R.id.empty_view)
            private var loadingView: View = view.findViewById(R.id.loading_view)
            private var add: ImageView = view.findViewById(R.id.add)

            fun bind(id: String) {
                // Set add
                add.setOnClickListener {
                    mOnClickAddListener?.onClickAdd(id)
                }
                // Initialization recyclerView
                this.recyclerView.apply {
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = ActionTypeAdapter(emptyList())
                    itemAnimator = ItemAnimator()
                }
                // Set data
                val liveData = actionTypeRepository.getActionTypesFromParent(id)
                liveData.observeForever { actionTypes ->
                    (this.recyclerView.adapter as ActionTypeAdapter).setData(actionTypes)
                }
            }

            fun select(actionType: ActionType){
                // Отправляем выбранный элемент дальше
                this@Adapter.select(adapterPosition, actionType)
            }


            // Infinity inner...
            // Sorry, man
            private inner class ActionTypeAdapter(var actionTypes: List<ActionType>):
                RecyclerView.Adapter<RecyclerView.ViewHolder>(){

                fun setData(newData: List<ActionType>){
                    // Находим, что изменилось
                    val diffUtilCallback = DiffUtilActionType(actionTypes, newData)
                    val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
                    // Update data
                    actionTypes = newData
                    // Notify
                    diffResult.dispatchUpdatesTo(this)

                    if (actionTypes.isEmpty()) emptyView.visibility = View.VISIBLE
                    else emptyView.visibility = View.GONE
                    loadingView.visibility = View.GONE
                }


                // This magic logic
                private var selectPosition: Int = -1
                fun select(position: Int, actionType: ActionType){
                    val oldSelect = selectPosition
                    selectPosition = position
                    this.notifyItemChanged(oldSelect)
                    this.notifyItemChanged(selectPosition)

                    // Отправляем выбранный элемент дальше
                    this@Holder.select(actionType)
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionTypeHolder {
                    return ActionTypeHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_item_select_action_type, parent, false))
                }

                override fun getItemCount() = actionTypes.size

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder as ActionTypeHolder).bind(actionTypes[position])
                    // Frame
                    if (position == selectPosition) {
                        holder.setVisibilityFrame(View.VISIBLE)
                    } else {
                        holder.setVisibilityFrame(View.INVISIBLE)
                    }
                }


                private inner class ActionTypeHolder(view: View):
                    RecyclerView.ViewHolder(view), OnClickListener {
                    private lateinit var actionType: ActionType
                    private var color: ImageView  = view.findViewById(R.id.color)
                    private var name: TextView = view.findViewById(R.id.name)
                    private var frame: ImageView = view.findViewById(R.id.frame)
                    private var countChild: TextView = view.findViewById(R.id.count_child)

                    init {
                        itemView.setOnClickListener(this)
                    }

                    fun bind(actionType: ActionType) {
                        this.actionType = actionType
                        color.setColorFilter(actionType.color)
                        name.text = actionType.name
                        val live = actionTypeRepository.getCountChild(actionType.id)
                        live.observeForever { count ->
                            if (count != 0){
                                countChild.visibility = View.VISIBLE
                                countChild.text = (resources.getString(R.string.count_)+" "+count.toString())
                            } else {
                                countChild.visibility = View.GONE
                            }
                        }
                    }

                    fun setVisibilityFrame(visibility: Int){
                        frame.visibility = visibility
                    }

                    // Это все ради обработки нажания в классах выше
                    override fun onClick(v: View) {
                        this@ActionTypeAdapter.select(adapterPosition, this.actionType)
                    }
                }
            }
        }

    }
}