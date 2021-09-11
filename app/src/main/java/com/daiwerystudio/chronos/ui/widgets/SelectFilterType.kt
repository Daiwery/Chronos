/*
* Дата создания: 26.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*

/**
 * Класс, с помощью которого можно выбрать typeShowing. Следит за тем, какое view сейчас выбрано
 * и автоматически удаляет прошлое выбранное view.
 */
class SelectFilterType(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {
    private val activeAlpha: Float = 1f
    private val notActiveAlpha: Float = 0.5f
    private var selectedView: ImageView? = null
    private val folderView: ImageView
    private val noteView: ImageView
    private val reminderView: ImageView
    private val goalView: ImageView
    private val scheduleView: ImageView
    private val actionTypeView: ImageView

    init {
        inflate(context, R.layout.layout_select_filter_type, this)
        isHorizontalScrollBarEnabled = false

        folderView = findViewById(R.id.folder)
        folderView.setOnClickListener { select(it as ImageView) }
        folderView.alpha = notActiveAlpha

        noteView = findViewById(R.id.note)
        noteView.setOnClickListener { select(it as ImageView) }
        noteView.alpha = notActiveAlpha

        reminderView = findViewById(R.id.reminder)
        reminderView.setOnClickListener { select(it as ImageView) }
        reminderView.alpha = notActiveAlpha

        goalView = findViewById(R.id.goal)
        goalView.setOnClickListener { select(it as ImageView) }
        goalView.alpha = notActiveAlpha

        scheduleView = findViewById(R.id.schedule)
        scheduleView.setOnClickListener { select(it as ImageView) }
        scheduleView.alpha = notActiveAlpha

        actionTypeView = findViewById(R.id.action_type)
        actionTypeView.setOnClickListener { select(it as ImageView) }
        actionTypeView.alpha = notActiveAlpha
    }

    private fun select(view: ImageView){
        // В selectView ссылка на выбранный view.
        selectedView?.alpha = notActiveAlpha
        // Если выбрали тоже самое, то ставим null.
        if (selectedView == view) selectedView = null
        else {
            selectedView = view
            selectedView?.alpha = activeAlpha
        }

        when (selectedView) {
            null -> mSelectTypeShowingListener?.selectTypeShowing(null)
            folderView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_FOLDER)
            noteView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_NOTE)
            reminderView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_REMINDER)
            goalView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_GOAL)
            scheduleView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_SCHEDULE)
            actionTypeView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_ACTION_TYPE)
        }
    }

    fun setTypeShowing(typeShowing: Int?){
        when (typeShowing){
            TYPE_ACTION_TYPE -> select(actionTypeView)
            TYPE_GOAL -> select(goalView)
            TYPE_SCHEDULE -> select(scheduleView)
            TYPE_NOTE -> select(noteView)
            TYPE_REMINDER -> select(reminderView)
            TYPE_FOLDER -> select(folderView)
        }
    }

    /*  Интерфейс, который сообщает о выбранном типе показа.  */
    private var mSelectTypeShowingListener: SelectTypeShowingListener? = null
    fun interface SelectTypeShowingListener{
        fun selectTypeShowing(typeShowing: Int?)
    }
    fun setSelectTypeShowingListener(selectTypeShowingListener: SelectTypeShowingListener){
        mSelectTypeShowingListener = selectTypeShowingListener
    }

}