/*
* Дата создания: 07.10.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.time_tracker

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
import com.google.android.material.floatingactionbutton.FloatingActionButton


class TimeTrackerFabMenu(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs){
    private val open: FloatingActionButton
    private val close: FloatingActionButton
    private val createActionTracker: FloatingActionButton
    private val actionTrackerTextView: TextView
    private val createAction: FloatingActionButton
    private val actionTextView: TextView
    private val lightening: ImageView
    var state: Int = STATE_CLOSED
        private set

    init {
        inflate(context, R.layout.layout_time_tracker_fab_menu, this)

        open = findViewById(R.id.open)
        open.setOnClickListener { transitionToOpen() }

        close = findViewById(R.id.close)
        close.setOnClickListener { transitionToClose() }

        createActionTracker = findViewById(R.id.create_action_tracker)
        actionTrackerTextView = findViewById(R.id.actionTrackerTextView)
        createActionTracker.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_ACTION_TRACKER)
            transitionToClose()
        }

        createAction = findViewById(R.id.create_action)
        actionTextView = findViewById(R.id.actionTextView)
        createAction.setOnClickListener {
            mOnMenuItemClickListener?.onMenuItemClick(TYPE_ACTION)
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
        createActionTracker.visibility = View.VISIBLE
        createAction.visibility = View.VISIBLE
        lightening.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(lightening, "alpha",  0.9f).setDuration(300).start()
        ObjectAnimator.ofFloat(close, "rotation",  0f).setDuration(300).start()
        animateFAB(createAction, 1f, 0f)
        animateTextView(actionTextView, 1f)
        animateFAB(createActionTracker, 1f, 0f)
        animateTextView(actionTrackerTextView, 1f)
    }

    private fun transitionToClose(invisible: Boolean = false){
        if (invisible) {
            state = STATE_INVISIBLE
            open.visibility = View.VISIBLE
        } else state = STATE_CLOSED

        animateFAB(createAction, 0f, 1f*createAction.height)
        animateTextView(actionTextView, 0f)
        animateFAB(createActionTracker, 0f, 2f*createAction.height)
        animateTextView(actionTrackerTextView, 0f)
        ObjectAnimator.ofFloat(lightening, "alpha",  0f).setDuration(300).start()
        val animation = ObjectAnimator.ofFloat(close, "rotation",  255f).setDuration(300)
        animation.addListener(object : Animator.AnimatorListener{
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                lightening.visibility = View.INVISIBLE
                close.visibility = View.INVISIBLE
                createActionTracker.visibility = View.INVISIBLE
                createAction.visibility = View.INVISIBLE

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
        const val TYPE_ACTION = 0
        const val TYPE_ACTION_TRACKER = 1

        const val STATE_OPENED = 0
        const val STATE_CLOSED = 1
        const val STATE_INVISIBLE = 0
    }

}