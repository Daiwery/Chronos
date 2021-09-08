/*
* Дата создания: 26.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*

/**
 * Класс, с помощью которого можно выбрать typeShowing. Следит за тем, какое view сейчас выбрано
 * и автоматически удаляет прошлое выбранное view.
 */
class SelectTypeShowing(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {
    private var selectedView: ImageView? = null
    private val folderView: ImageView
    private val noteView: ImageView
    private val reminderView: ImageView
    private val goalView: ImageView
    private val scheduleView: ImageView
    private val actionTypeView: ImageView

    init {
        inflate(context, R.layout.layout_select_type_showing, this)
        isHorizontalScrollBarEnabled = false

        folderView = findViewById(R.id.folder)
        folderView.setOnClickListener { select(it as ImageView) }
        folderView.setColorFilter(ContextCompat.getColor(context, R.color.black))

        noteView = findViewById(R.id.note)
        noteView.setOnClickListener { select(it as ImageView) }
        noteView.setColorFilter(ContextCompat.getColor(context, R.color.black))

        reminderView = findViewById(R.id.reminder)
        reminderView.setOnClickListener { select(it as ImageView) }
        reminderView.setColorFilter(ContextCompat.getColor(context, R.color.black))

        goalView = findViewById(R.id.goal)
        goalView.setOnClickListener { select(it as ImageView) }
        goalView.setColorFilter(ContextCompat.getColor(context, R.color.black))

        scheduleView = findViewById(R.id.schedule)
        scheduleView.setOnClickListener { select(it as ImageView) }
        scheduleView.setColorFilter(ContextCompat.getColor(context, R.color.black))

        actionTypeView = findViewById(R.id.action_type)
        actionTypeView.setOnClickListener { select(it as ImageView) }
        actionTypeView.setColorFilter(ContextCompat.getColor(context, R.color.black))
    }

    private fun select(view: ImageView){
        // В selectView ссылка на выбранный view.
        selectedView?.setColorFilter(ContextCompat.getColor(context, R.color.black))
        // Если выбрали тоже самое, то ставим null.
        if (selectedView == view) selectedView = null
        else {
            selectedView = view
            selectedView?.setColorFilter(ContextCompat.getColor(context, R.color.purple_500))
        }

        when (selectedView) {
            null -> mSelectTypeShowingListener?.selectTypeShowing(-1)
            folderView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_FOLDER)
            noteView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_NOTE)
            reminderView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_REMINDER)
            goalView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_GOAL)
            scheduleView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_SCHEDULE)
            actionTypeView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_ACTION_TYPE)
        }
    }

    fun setTypeShowing(typeShowing: Int){
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
        fun selectTypeShowing(typeShowing: Int)
    }
    fun setSelectTypeShowingListener(selectTypeShowingListener: SelectTypeShowingListener){
        mSelectTypeShowingListener = selectTypeShowingListener
    }

}