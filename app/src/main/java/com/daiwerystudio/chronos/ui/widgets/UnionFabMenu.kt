/*
* Дата создания: 20.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 18.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: теперь это соединение нескольких fab с анимацией появление из одного fab.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.google.android.material.floatingactionbutton.FloatingActionButton


class UnionFabMenu(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs){
    private val open: FloatingActionButton
    private val close: FloatingActionButton
    private var createFolder: FloatingActionButton
    private val folderTextView: TextView
    private val createNote: FloatingActionButton
    private val noteTextView: TextView
    private val createReminder: FloatingActionButton
    private val reminderTextView: TextView
    private val createGoal: FloatingActionButton
    private val goalTextView: TextView
    private val createSchedule: FloatingActionButton
    private val scheduleTextView: TextView
    private val createActionType: FloatingActionButton
    private val actionTypeTextView: TextView
    private val lightening: ImageView
    var state: Int = STATE_CLOSED
        private set

    init {
        inflate(context, R.layout.layout_union_fab_menu, this)

        open = findViewById(R.id.open)
        open.setOnClickListener { transitionToOpen() }

        close = findViewById(R.id.close)
        close.setOnClickListener { transitionToClose() }

        createFolder = findViewById(R.id.create_folder)
        folderTextView = findViewById(R.id.folderTextView)
        createFolder.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_FOLDER)
            transitionToClose()
        }

        createNote = findViewById(R.id.create_note)
        noteTextView = findViewById(R.id.noteTextView)
        createNote.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_NOTE)
            transitionToClose()
        }

        createReminder = findViewById(R.id.create_reminder)
        reminderTextView = findViewById(R.id.reminderTextView)
        createReminder.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_REMINDER)
            transitionToClose()
        }

        createGoal = findViewById(R.id.create_goal)
        goalTextView = findViewById(R.id.goalTextView)
        createGoal.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_GOAL)
            transitionToClose()
        }

        createSchedule = findViewById(R.id.create_schedule)
        scheduleTextView = findViewById(R.id.scheduleTextView)
        createSchedule.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_SCHEDULE)
            transitionToClose()

        }

        createActionType = findViewById(R.id.create_action_type)
        actionTypeTextView = findViewById(R.id.actionTypeTextView)
        createActionType.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_ACTION_TYPE)
            transitionToClose()
        }

        lightening = findViewById(R.id.imageView6)
        lightening.setOnClickListener { transitionToClose() }
    }

    private fun transitionToOpen(){
        state = STATE_OPENED

        close.rotation = 225f
        close.visibility = View.VISIBLE
        open.visibility = View.INVISIBLE
        createActionType.visibility = View.VISIBLE
        createSchedule.visibility = View.VISIBLE
        createGoal.visibility = View.VISIBLE
        createReminder.visibility = View.VISIBLE
        createNote.visibility = View.VISIBLE
        createFolder.visibility = View.VISIBLE
        lightening.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(lightening, "alpha",  0.9f).setDuration(300).start()
        ObjectAnimator.ofFloat(close, "rotation",  0f).setDuration(300).start()
        animateFAB(createActionType, 1f, 0f)
        animateTextView(actionTypeTextView, 1f)
        animateFAB(createSchedule, 1f, 0f)
        animateTextView(scheduleTextView, 1f)
        animateFAB(createGoal, 1f, 0f)
        animateTextView(goalTextView, 1f)
        animateFAB(createReminder, 1f, 0f)
        animateTextView(reminderTextView, 1f)
        animateFAB(createNote, 1f, 0f)
        animateTextView(noteTextView, 1f)
        animateFAB(createFolder, 1f, 0f)
        animateTextView(folderTextView, 1f)
    }

    private fun transitionToClose(invisible: Boolean = false){
        if (invisible) {
            state = STATE_INVISIBLE
            open.visibility = View.VISIBLE
        } else state = STATE_CLOSED

        animateFAB(createActionType, 0f, 1f*createGoal.height)
        animateTextView(actionTypeTextView, 0f)
        animateFAB(createSchedule, 0f, 2f*createGoal.height)
        animateTextView(scheduleTextView, 0f)
        animateFAB(createGoal, 0f, 3f*createGoal.height)
        animateTextView(goalTextView, 0f)
        animateFAB(createReminder, 0f, 4f*createGoal.height)
        animateTextView(reminderTextView, 0f)
        animateFAB(createNote, 0f, 5f*createGoal.height)
        animateTextView(noteTextView, 0f)
        animateFAB(createFolder, 0f, 6f*createGoal.height)
        animateTextView(folderTextView, 0f)
        ObjectAnimator.ofFloat(lightening, "alpha",  0f).setDuration(300).start()
        val animation = ObjectAnimator.ofFloat(close, "rotation",  255f).setDuration(300)
        animation.addListener(object : Animator.AnimatorListener{
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                lightening.visibility = View.INVISIBLE
                close.visibility = View.INVISIBLE
                createActionType.visibility = View.INVISIBLE
                createSchedule.visibility = View.INVISIBLE
                createGoal.visibility = View.INVISIBLE
                createReminder.visibility = View.INVISIBLE
                createNote.visibility = View.INVISIBLE
                createFolder.visibility = View.INVISIBLE
                lightening.visibility = View.INVISIBLE

                if (invisible) open.hide()
                else open.visibility = View.VISIBLE
            }
        })
        animation.start()
    }

    private fun animateFAB(view: View, alpha: Float, translationY: Float){
        ObjectAnimator.ofFloat(view, "translationY",  translationY).setDuration(300)
            .apply { interpolator = Interpolator { if (it < 0.8f) it/0.8f else 1f } }.start()
        ObjectAnimator.ofFloat(view, "alpha",  alpha).setDuration(300).start()
    }

    private fun animateTextView(view: View, alpha: Float){
        ObjectAnimator.ofFloat(view, "alpha",  alpha).setDuration(300).start()
    }

    fun close() {
        transitionToClose()
    }

    fun hide(){
        if (state == STATE_OPENED) transitionToClose(true)
        else {
            state = STATE_INVISIBLE
            open.hide()
        }
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
        const val STATE_OPENED = 0
        const val STATE_CLOSED = 1
        const val STATE_INVISIBLE = 0
    }

}
