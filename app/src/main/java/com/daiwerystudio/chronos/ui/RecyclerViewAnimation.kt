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



class ItemAnimator: DefaultItemAnimator(){
    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        val itemView = holder!!.itemView

        // Listener
        val listener = object : Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                dispatchAnimationFinished(holder)
            }
        }

        // Animation
        val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.anim_add_item)
        animation.setAnimationListener(listener)
        itemView.startAnimation(animation)

        return true
    }
}



open class CustomItemTouchCallback(var context: Context, dragDirs: Int, swipeDirs: Int):
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs){


    override fun onMove(recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder): Boolean{
        return false
    }

    // onSwiped вызывает две переопределяемые функции
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Dialog
        AlertDialog.Builder(context, R.style.App_AlertDialog)
            .setTitle(R.string.are_you_sure)
            .setPositiveButton(R.string.yes) { _, _ ->
                onClickPositiveButton(viewHolder, direction)
            }
            .setNegativeButton(R.string.no){ _, _ ->
                onClickNegativeButton(viewHolder, direction)
            }
            .setCancelable(false)
            .create()
            .show()
    }
    open fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder, direction: Int){
    }
    open fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder, direction: Int){
    }


    // Все ради этого, кстати
    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)
    private val background = ColorDrawable(Color.parseColor("#930000"))
    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView
        val backgroundCornerOffset = 20

        val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight


        // Swipe left, right and unSwiped
        when {
            dX > 0 -> {
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
                background.setBounds(0, 0, 0, 0)
            }
        }

        // Draw
        background.draw(c)
        icon.draw(c)
    }

}
