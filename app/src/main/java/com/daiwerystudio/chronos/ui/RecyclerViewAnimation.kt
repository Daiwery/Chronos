/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R

/*
 * В файле написаны два класса, являющиемся расширениями от ItemAnimation
 *  и ItemTouchHelper.SimpleCallback соответственно. Эти два класса используются во всех
 * Recycler View, по этой причине они были вынесены в отдельный файл.
 */

/**
 * Необходим для анимации Holder-ов в Recycler View. В суперклассе уже существует стандартная
 * анимация, по этой переопределяется только анимация при добавлении Holder-а в RecyclerView.
 * При окончании анимации необохдимо вызывать dispatchAnimationFinished(holder).
 */
class ItemAnimator: DefaultItemAnimator(){
    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        val itemView = holder!!.itemView

        val listener = object : Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                dispatchAnimationFinished(holder)
            }
        }

        val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.anim_add_item)
        animation.setAnimationListener(listener)
        itemView.startAnimation(animation)

        return true
    }
}

/**
 * Наследуется от класса ItemTouchHelper.SimpleCallback, который помогает обабатывать такие
 * действия пользователя, как взмах влево или вправо и перемещение элемента вверх или вниз.
 * Является отрытым классом, потому что для его использования необходимо переопределить 3 функции:
 * это onMove - то, что происходит при перемещнии Holder-a вверх или вниз, onClickPositiveButton
 * и onClickNegativeButton - то, что происходит при нажатии на кнопки в AlertDialog, который
 * возникает при взмахе влево или вправо. Главная функция, из-за которой данный класс был
 * вынесен в отдельный файл, - это onChildDraw. Она рисует красный фон и иконку мусорки за Holder-ом.
 * @param context Context. Необходим для прорисовки Alert Dialog.
 * @param dragDirs разрешенные направления перемещения.
 * @param swipeDirs разрешенные направления взмаха.
 */
open class CustomItemTouchCallback(var context: Context, dragDirs: Int, swipeDirs: Int):
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs){

    /**
     * Выполняется при перемещении Holder-а вверх или вниз. По умолчанию ничего не делает.
     * Для использользования необоходимо переопределить.
     */
    override fun onMove(recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder): Boolean{
        return false
    }

    /**
     * Выполняется при взмахе влево ил вправо. Показывает AlertDialog с надписью "Are you sure?"
     * и двумя кнопками "Yes" и "No", при нажатии на которых вызывает функции onClickPositiveButton
     * и onClickNegativeButton соответственно.
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        AlertDialog.Builder(context, R.style.App_AlertDialog)
            .setTitle(R.string.are_you_sure)
            .setPositiveButton(R.string.yes) { _, _ ->
                onClickPositiveButton(viewHolder)
            }
            .setNegativeButton(R.string.no){ _, _ ->
                onClickNegativeButton(viewHolder)
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /**
     * Вызывается из onSwiped при нажатии на кнопку "Yes" в AlertDialog. По умолчанию
     * ничего не делает, необходимо переопределить для использования.
     */
    open fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder){
    }

    /**
     * Вызывается из onSwiped при нажатии на кнопку "No" в AlertDialog. По умолчанию
     * ничего не делает, необходимо переопределить для использования.
     */
    open fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder){
    }

    /**
     * Иконка, которая рисует onChildDraw. По умолчанию R.drawable.ic_baseline_delete_24.
     */
    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)

    /**
     * Задний фон, который рисует onChildDraw. Представляет из себя ColorDrawable с цветом #930000.
     */
    private val background = ColorDrawable(Color.parseColor("#930000"))

    /**
     * Рисует иконку и фон за Holder-ом, который пользователем взмахнул влево или вправо.
     * Код предложен на https://medium.com/@zackcosborn/step-by-step-recyclerview-swipe-to-delete-and-undo-7bbae1fce27e .
     * За исключение нескольких модификаций, дабавленных мною, (написаны комментариями в коде).
     */
    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView
        val backgroundCornerOffset = 20

        val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight

        when {
            dX > 0 -> {
                // iconRight и iconLeft поменялись местами.
                val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                val iconLeft = itemView.left + iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                background.setBounds(itemView.left, itemView.top,
                    itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom)
            }
            dX < 0 -> {
                val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset,
                    itemView.top, itemView.right, itemView.bottom)
            }
            else -> {
                // Была добавления строчка ниже, как необходимость исчезновения иконки при
                // вертикальных перещениях Holder-а.
                icon.setBounds(0, 0, 0, 0)
                background.setBounds(0, 0, 0, 0)
            }
        }

        background.draw(c)
        icon.draw(c)
    }

}
