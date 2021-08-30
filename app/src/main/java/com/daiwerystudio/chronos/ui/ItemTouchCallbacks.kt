/*
* Дата создания: 27.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Класс с определением функций для взмаха холдера влево.
 */
open class ItemTouchSwipeCallback(dragDirs: Int) :
    ItemTouchHelper.SimpleCallback(dragDirs, ItemTouchHelper.LEFT) {

    /*  Функции ниже означают, что функция onSwiped никогда не будет вызвана.  */
    override fun getSwipeEscapeVelocity(defaultValue: Float) = Float.MAX_VALUE
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = Float.MAX_VALUE

    /**
     * Находится ли холдер в взмахнутом состоянии. Определяется в onChildDraw: вышли ли
     * грацины холедра за установленнную черту.
     */
    private var mIsSwiped: Boolean = false

    /**
     * Позиция активного холдера в адаптере. Передается в интерфейс.
     */
    private var mSwipePosition: Int = 0

    /**
     * Так как mIsSwiped определяем в onChildDraw, которая запускается и при обратной анимации,
     * нам нужна переменная, в которой будет хранится, нужно ли спрашивать пользователя о удалении.
     * Она устанавливается в onSelectedChanged, то есть в тот момент, когда пользователь
     * закончил взаимодействовать с холдером и когда холдер еще не возвращается обратно.
     */
    private var mIsActiveDelete: Boolean = false

    /**
     * Иконка, которую рисует onChildDraw. Устанавливается во фрагменте.
     */
    var icon: Drawable? = null

    /**
     * Задний фон, который рисует onChildDraw. Устанавливается во фрагменте.
     */
    var background: Drawable? = null


    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        var dx = dX
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dx < -viewHolder.itemView.width/4f) {
                dx = -viewHolder.itemView.width/4f
                mIsSwiped = true
            } else mIsSwiped = false

            if (dx < 0) {
                val itemView = viewHolder.itemView
                background?.setBounds(itemView.left+viewHolder.itemView.width/10,
                    itemView.top, itemView.right, itemView.bottom)

                icon?.also {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - it.intrinsicWidth
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }
            } else {
                icon?.setBounds(0, 0, 0, 0)
                background?.setBounds(0, 0, 0, 0)
            }
        }
        background?.draw(c)
        icon?.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dx, dY, actionState, isCurrentlyActive)
    }

    /**
     * Запускается, когда меняется состояние холдера. То есть когда пользователь начинает
     * или заканчивает взаимодействовать с холдером.
     */
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder != null) mSwipePosition = viewHolder.absoluteAdapterPosition
        if (mIsSwiped) mIsActiveDelete = true

        super.onSelectedChanged(viewHolder, actionState)
    }

    /**
     * Запускается после окончания взаимодействия пользователя с холдером и окончания анимации
     * возвращения.
     */
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (mIsActiveDelete) {
            mSwipeItemListener?.swipeItem(mSwipePosition)
            mIsActiveDelete = false
        }
    }

    /* Интерфейс, вызываемый при событии взмахивания.  */
    private var mSwipeItemListener: SwipeItemListener? = null
    fun interface SwipeItemListener{
        fun swipeItem(position: Int)
    }
    fun setSwipeItemListener(swipeItemListener: SwipeItemListener){
        mSwipeItemListener = swipeItemListener
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean = false
}

/**
 * Класс с определением функций для drag event холдера.
 */
class ItemTouchDragCallback(dragDirs: Int) : ItemTouchSwipeCallback(dragDirs) {
    /**
     * Холдер, который перемещаем.
     */
    private var dragFromViewHolder: RecyclerView.ViewHolder? = null
    /**
     * Холдер, на который перместили.
     */
    private var dragToViewHolder: RecyclerView.ViewHolder? = null

    /**
     * Запускается, когда пользователь переместил холдер на другой холдер.
     */
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        setDragToViewHolder(target)
        return true
    }

    /**
     * Запускается, когда меняется состояние холдера. То есть когда пользователь начинает
     * или заканчивает взаимодействовать с холдером.
     */
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> setDragFromViewHolder(viewHolder!!)
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                if (dragFromViewHolder != null) {
                    // Если холдер никуда не переместили, то ставим dragToPosition равной -1.
                    // Это значит, что пользователь хочет переместить union вверх по иерархии.
                    mDragItemListener?.dragItem(dragFromViewHolder!!.absoluteAdapterPosition,
                        dragToViewHolder?.absoluteAdapterPosition ?: -1)

                    resetViewHolders()
                }
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    /**
     * Выполнятеся, когда хочет переместить холдер.
     */
    private fun setDragFromViewHolder(viewHolder: RecyclerView.ViewHolder){
        dragFromViewHolder = viewHolder
    }

    /**
     * Выполняется, когда пользователь переместил холдер на другой холдер.
     */
    private fun setDragToViewHolder(viewHolder: RecyclerView.ViewHolder){
        dragToViewHolder?.also { it.itemView.alpha = 1f }
        dragToViewHolder = viewHolder.also { it.itemView.alpha = 0.7f }
    }

    /**
     * Reset всех холдеров.
     */
    private fun resetViewHolders(){
        dragFromViewHolder?.also { it.itemView.alpha = 1f }
        dragToViewHolder?.also { it.itemView.alpha = 1f }
        dragFromViewHolder = null
        dragToViewHolder = null
    }

    /* Интерфейс, вызываемый при событии drag. */
    private var mDragItemListener: DragItemListener? = null
    fun interface DragItemListener{
        fun dragItem(dragFromPosition: Int, dragToPosition: Int)
    }
    fun setDragItemListener(dragItemListener: DragItemListener){
        mDragItemListener = dragItemListener
    }
}