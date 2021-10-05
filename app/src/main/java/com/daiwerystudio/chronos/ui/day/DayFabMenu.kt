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
    private var state: Int = STATE_CLOSED

    init {
        inflate(context, R.layout.layout_day_fab_menu, this)
        // По какой-то причине использование this.transitionToEnd() не работает.
        motionLayout = findViewById(R.id.motionLayout)

        open = findViewById(R.id.open)
        open.setOnClickListener {
            state = STATE_OPENED
            motionLayout.transitionToEnd()
        }

        close = findViewById(R.id.close)
        close.setOnClickListener {
            state = STATE_CLOSED
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

    override fun isFocused(): Boolean = state == STATE_OPENED
    val isVisible: Boolean
        get() = state != STATE_INVISIBLE

    override fun clearFocus() {
        state = STATE_CLOSED
        motionLayout.transitionToStart()
    }

    fun hide(){
        state = STATE_INVISIBLE
        motionLayout.transitionToStart()
        open.hide()
    }

    fun show(){
        state = STATE_CLOSED
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

        const val STATE_OPENED = 0
        const val STATE_CLOSED = 1
        const val STATE_INVISIBLE = 0
    }

}