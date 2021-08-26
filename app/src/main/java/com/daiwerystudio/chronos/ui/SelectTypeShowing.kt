/*
* Дата создания: 26.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*

/**
 * Класс, с помощью которого можно выбрать typeShowing. Следит за тем, какое view сейчас выбрано
 * и автоматически удаляет прошлое выбранное view.
 */
class SelectTypeShowing(context: Context, attrs: AttributeSet): HorizontalScrollView(context, attrs) {
    private var selectView: View
    private val allView: View
    private val noteView: View
    private val reminderView: View
    private val goalView: View
    private val scheduleView: View
    private val actionTypeView: View

    init {
        inflate(context, R.layout.layout_select_type_showing, this)
        isHorizontalScrollBarEnabled = false

        allView = findViewById(R.id.all)
        allView.setOnClickListener { select(it) }

        noteView = findViewById(R.id.note)
        noteView.setOnClickListener { select(it) }

        reminderView = findViewById(R.id.reminder)
        reminderView.setOnClickListener { select(it) }

        goalView = findViewById(R.id.goal)
        goalView.setOnClickListener { select(it) }

        scheduleView = findViewById(R.id.schedule)
        scheduleView.setOnClickListener { select(it) }

        actionTypeView = findViewById(R.id.action_type)
        actionTypeView.setOnClickListener { select(it) }

        // Устанавливаем начальный выбор, как "все", без вызова функции.
        selectView = allView
        selectView.isSelected = true
    }

    private fun select(view: View){
        // В selectView ссылка на выбранный view.
        selectView.isSelected = false

        view.isSelected = true
        selectView = view

        when (selectView) {
            allView -> mSelectTypeShowingListener?.selectTypeShowing(-1)
            noteView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_NOTE)
            reminderView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_REMINDER)
            goalView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_GOAL)
            scheduleView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_SCHEDULE)
            actionTypeView -> mSelectTypeShowingListener?.selectTypeShowing(TYPE_ACTION_TYPE)
        }
    }

    fun setTypeShowing(typeShowing: Int){
        selectView.isSelected = false
        when (typeShowing){
            -1 -> selectView = allView
            TYPE_ACTION_TYPE -> selectView = actionTypeView
            TYPE_GOAL -> selectView = goalView
            TYPE_SCHEDULE -> selectView = scheduleView
            TYPE_NOTE -> selectView = noteView
            TYPE_REMINDER -> selectView = reminderView
        }
        selectView.isSelected = true
    }

    private var mSelectTypeShowingListener: SelectTypeShowingListener? = null
    fun interface SelectTypeShowingListener{
        fun selectTypeShowing(typeShowing: Int)
    }
    fun setSelectTypeShowingListener(selectTypeShowingListener: SelectTypeShowingListener){
        mSelectTypeShowingListener = selectTypeShowingListener
    }

}