/*
* Дата создания: 20.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 18.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: теперь это соединение нескольких fab с анимацией появление из одного fab.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.google.android.material.floatingactionbutton.FloatingActionButton


class UnionFabMenu(context: Context, attrs: AttributeSet) : MotionLayout(context, attrs){
    private val motionLayout: MotionLayout
    private val open: FloatingActionButton
    private val close: FloatingActionButton
    private var createFolder: FloatingActionButton
    private val createNote: FloatingActionButton
    private val createReminder: FloatingActionButton
    private val createGoal: FloatingActionButton
    private val createSchedule: FloatingActionButton
    private val createActionType: FloatingActionButton
    private val lightening: ImageView

    init {
        inflate(context, R.layout.layout_union_fab_menu, this)
        // По какой-то причине использование this.transitionToEnd() не работает.
        motionLayout = findViewById(R.id.motionLayout)

        open = findViewById(R.id.open)
        open.setOnClickListener { motionLayout.transitionToEnd() }

        close = findViewById(R.id.close)
        close.setOnClickListener { motionLayout.transitionToStart() }

        createFolder = findViewById(R.id.create_folder)
        createFolder.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_FOLDER)
            motionLayout.transitionToStart()
        }

        createNote = findViewById(R.id.create_note)
        createNote.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_NOTE)
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

        createSchedule = findViewById(R.id.create_schedule)
        createSchedule.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_SCHEDULE)
            motionLayout.transitionToStart()

        }

        createActionType = findViewById(R.id.create_action_type)
        createActionType.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_ACTION_TYPE)
            motionLayout.transitionToStart()
        }

        lightening = findViewById(R.id.imageView6)
        lightening.setOnClickListener { motionLayout.transitionToStart() }
    }

    fun hide(){
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

}
