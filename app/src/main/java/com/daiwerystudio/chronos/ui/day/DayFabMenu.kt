/*
* Дата создания: 02.10.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.day

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import com.daiwerystudio.chronos.R
import com.google.android.material.floatingactionbutton.FloatingActionButton


class DayFabMenu(context: Context, attrs: AttributeSet) : MotionLayout(context, attrs){
    private val motionLayout: MotionLayout
    private val open: FloatingActionButton
    private val close: FloatingActionButton
    private val createReminder: FloatingActionButton
    private val createGoal: FloatingActionButton
    private val lightening: ImageView
    private var isOpened: Boolean = false

    init {
        inflate(context, R.layout.layout_day_fab_menu, this)
        // По какой-то причине использование this.transitionToEnd() не работает.
        motionLayout = findViewById(R.id.motionLayout)

        open = findViewById(R.id.open)
        open.setOnClickListener {
            isOpened = true
            motionLayout.transitionToEnd()
        }

        close = findViewById(R.id.close)
        close.setOnClickListener {
            isOpened = false
            motionLayout.transitionToStart()
        }

        createReminder = findViewById(R.id.create_reminder)
        createReminder.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_REMINDER)
            motionLayout.transitionToStart()
        }

        createGoal = findViewById(R.id.create_goal)
        createGoal.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_GOAL)
            motionLayout.transitionToStart()
        }

        lightening = findViewById(R.id.imageView6)
        lightening.setOnClickListener { motionLayout.transitionToStart() }
    }

    override fun isFocused(): Boolean = isOpened

    override fun clearFocus() {
        isOpened = false
        motionLayout.transitionToStart()
    }

    fun hide(){
        isOpened = false
        motionLayout.transitionToStart()
        open.hide()
    }

    fun show(){
        open.show()
    }


    private var mOnMenuItemClickListener: OnMenuItemClickListener? = null
    fun interface OnMenuItemClickListener{
        fun onMenuItemClick(item: Int)
    }
    fun setOnMenuItemClickListener(onMenuItemClickListener: OnMenuItemClickListener){
        mOnMenuItemClickListener = onMenuItemClickListener
    }

    companion object {
        const val TYPE_GOAL = 0
        const val TYPE_REMINDER = 1
    }

}