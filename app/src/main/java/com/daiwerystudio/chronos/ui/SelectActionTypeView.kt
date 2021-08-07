/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import android.content.Context
import android.util.AttributeSet
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
import java.util.*

/**
 * Класс виджета, позволяющего выбрать тип действия. Представляет из себя горизонтальные
 * RecyclerView, вложенный в один вертикальный RecyclerView. Написан на основе вложенных
 * классов: адаптер вложен в холдер.
 *
 * Возможная модификация: виджет ведет себя странно при быстром выборе типов действий. Возможно,
 * дело во вложенных классах. Как идея, можно переписать всю логику на основе интерфейсов.
 * Но в этом случае класс не сможет стать внутренним и не будет иметь доступа к необходимым
 * функциям.
 */
class SelectActionTypeView(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {
    /**
     * Массив с id типов действий. В каждой строчке показываются дети от родителя с id
     * из этого массива.
     */
    private var ids: MutableList<String> = mutableListOf("")

    /**
     * UI для выбранного типа действия.
     */
    private var color: ImageView
    private var name: TextView

    /**
     * Основной RecyclerView
     */
    private var recyclerView: RecyclerView

    /**
     * Доступ к базе данных
     */
    private val actionTypeRepository = ActionTypeRepository.get()

    /**
     * Выбранный тип действия.
     */
    private var selectActionType: ActionType? = null

    /**
     * Инициализация. Заполнение макета и настройка UI.
     */
    init {
        inflate(context, R.layout.layout_select_action_type, this)

        color = findViewById(R.id.imageView)
        color.visibility = View.INVISIBLE

        name = findViewById(R.id.textView1)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(ids)
        }
    }

    /**
     * Интерфейс для события выбора типа действия.
     */
    private var mOnSelectListener: OnSelectListener? = null
    fun interface OnSelectListener{
        fun onSelect(actionType: ActionType)
    }
    fun setOnSelectListener(onSelectListener: OnSelectListener?) {
        this.mOnSelectListener = onSelectListener
    }

    /**
     * Интерфейс для события события нажатие на кнопку "+".
     */
    private var mOnClickAddListener: OnClickAddListener? = null
    fun interface OnClickAddListener{
        fun onClickAdd(id: String)
    }
    fun setOnClickAddListener(onClickAddListener: OnClickAddListener?) {
        this.mOnClickAddListener = onClickAddListener
    }

    /**
     * Устанавливает заданный тип действия как выбранный.
     */
    fun setSelectActionType(id: String){
        val liveData = actionTypeRepository.getActionType(id)
        liveData.observeForever {
            if (it == null) throw IllegalStateException("Invalid id")

            // Так нужно сделать, чтобы UI для выбранного элемента не менялся
            // при изменении базы данных. Такое возможно, так как это подписка.
            if (selectActionType == null) {
                selectActionType = it
                setUI(selectActionType!!)
            }
        }
    }

    /**
     * Устанавливает UI для типа действия.
     */
    private fun setUI(actionType: ActionType){
        color.visibility = View.VISIBLE
        color.setColorFilter(actionType.color)
        name.text = actionType.name
    }

    /**
     * Обновляет массив с id после выбора типа действий. Сперва удаляет все id после выбранного
     * и добавляет выбранный id.
     */
    private fun updateIds(position: Int, actionType: ActionType){
        val size = ids.size
        for (i in position+1 until size){
            ids.removeAt(position+1)
        }
        ids.add(actionType.id)

        setUI(actionType)
        selectActionType = actionType

        (recyclerView.adapter as Adapter).setData(ids)
        recyclerView.scrollToPosition(position+1)

        mOnSelectListener?.onSelect(actionType)
    }

    /**
     * Внутри этого holder-а вложен адаптер для горизонтального RecyclerView, а в адаптер вложен
     * еще один holder. Это нужно для доступа адаптера и холдера к некоторым функциям.
     * Но основная цель: визуализировать рамку вокруг того холдера, которого выбрал пользователь.
     */
    private inner class Holder(view: View): RecyclerView.ViewHolder(view){
        /**
         * Горизонтальный RecyclerView.
         */
        private var recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        /**
         * UI.
         */
        private var emptyView: View = view.findViewById(R.id.empty_view)
        private var loadingView: View = view.findViewById(R.id.loading_view)
        private var add: ImageView = view.findViewById(R.id.add)

        /**
         * Заполнение холдера. Установка OnClickListener на кнопку "+", настройка RecyclerView
         * и получение для него данных.
         */
        fun bind(id: String) {
            add.setOnClickListener {
                mOnClickAddListener?.onClickAdd(id)
            }

            this.recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = ActionTypeAdapter(emptyList())
                itemAnimator = ItemAnimator()
            }

            val actionTypes = actionTypeRepository.getActionTypesFromParent(id)
            actionTypes.observeForever {
                (this.recyclerView.adapter as ActionTypeAdapter).setData(it)
            }
        }

        /**
         * Вызывается из адаптера, когда пользователь выбрал тип действия.
         */
        fun select(actionType: ActionType){
            updateIds(adapterPosition, actionType)
        }

        /**
         * Вложенный класс адаптера для горизонтального RecyclerView.
         */
        private inner class ActionTypeAdapter(var actionTypes: List<ActionType>):
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            /**
             * Хранит позицию выбранного холдера в данной строке (горизонтальном RecyclerView).
             */
            private var selectPosition: Int = -1

            /**
             * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
             */
            fun setData(newData: List<ActionType>) {
                val diffUtilCallback = DiffUtilActionType(actionTypes, newData)
                val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

                actionTypes = newData
                diffResult.dispatchUpdatesTo(this)

                if (actionTypes.isEmpty()) emptyView.visibility = View.VISIBLE
                else emptyView.visibility = View.GONE
                loadingView.visibility = View.GONE
            }

            /**
             * Вызывается их холдера, который выбрал пользователь. Нужна для установки рамки
             * только у выбранного холдера.
             */
            fun select(position: Int, actionType: ActionType){
                val oldSelect = selectPosition
                selectPosition = position

                this.notifyItemChanged(oldSelect)
                this.notifyItemChanged(selectPosition)

                this@Holder.select(actionType)
            }

            /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionTypeHolder {
                return ActionTypeHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_item_select_action_type, parent, false))
            }

            override fun getItemCount() = actionTypes.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder as ActionTypeHolder).bind(actionTypes[position])

                if (position == selectPosition) holder.setVisibilityFrame(View.VISIBLE)
                else holder.setVisibilityFrame(View.INVISIBLE)
            }

            /**
             * Вложенный класс холдера в адаптере. Это то, с чем в/д пользователь.
             * При нажатии на холдер он посылает сигнал адаптеру, что пользователь выбрал его.
             */
            private inner class ActionTypeHolder(view: View):
                RecyclerView.ViewHolder(view), OnClickListener {
                /**
                 * Тип действия, который показывет холдер.
                 */
                private lateinit var actionType: ActionType

                /**
                 * UI.
                 */
                private var color: ImageView  = view.findViewById(R.id.imageView)
                private var name: TextView = view.findViewById(R.id.textView1)
                private var frame: ImageView = view.findViewById(R.id.frame)
                private var countChild: TextView = view.findViewById(R.id.textView3)

                /**
                 * Инициализация. Ставится OnClickListener на холдер.
                 */
                init {
                    itemView.setOnClickListener(this)
                }

                /**
                 * Установка содержимого holder-а.
                 */
                fun bind(actionType: ActionType) {
                    this.actionType = actionType

                    color.setColorFilter(actionType.color)
                    name.text = actionType.name

                    val count = actionTypeRepository.getCountChild(actionType.id)
                    count.observeForever {
                        if (it != 0){
                            countChild.visibility = View.VISIBLE
                            countChild.text = (resources.getString(R.string.count_)+" "+it.toString())
                        } else countChild.visibility = View.GONE
                    }
                }

                /**
                 * Устанавливает видимость для рамки. Вызвается из адаптера.
                 */
                fun setVisibilityFrame(visibility: Int){
                    frame.visibility = visibility
                }

                /**
                 * Выполняет при нажатии на холдер. Посылает сигнал адаптеру, что его выбрали.
                 */
                override fun onClick(v: View) {
                    this@ActionTypeAdapter.select(adapterPosition, this.actionType)
                }
            }
        }
    }

    /**
     * Адаптер для главного RecyclerView. Никаких особенностей не имеет.
     */
    private inner class Adapter(var ids: List<String>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        /**
         * Установка новых данных для адаптера и вычисления изменений с помощью DiffUtil
         */
        fun setData(newData: List<String>){
            val diffUtilCallback = DiffUtilString(ids, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)

            // Copy data
            ids = newData.map{ it }
            diffResult.dispatchUpdatesTo(this)
        }


        /*  Ниже представлены стандартные функции адаптера.  См. оф. документацию. */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
            return Holder(LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_line_select_action_type, parent, false))
        }

        override fun getItemCount() = ids.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as Holder).bind(ids[position])
        }
    }

    /**
     * Класс для объявления функций класса DiffUtil.Callback. См. оф. документацию.
     *
     * Возможная модификация: необходимо вынести этот класс в файл RecyclerView, так как
     * он повторяется почти по всех RecyclerView. Но из-за того, что в каждом RecyclerView
     * данные разных типов, это сделать проблематично. (Но ведь возможно!)
     */
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

    /**
     * Класс для объявления функций класса DiffUtil.Callback. См. оф. документацию.
     *
     * Возможная модификация: необходимо вынести этот класс в файл RecyclerView, так как
     * он повторяется почти по всех RecyclerView. Но из-за того, что в каждом RecyclerView
     * данные разных типов, это сделать проблематично. (Но ведь возможно!)
     */
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
}