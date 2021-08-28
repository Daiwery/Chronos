/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.widgets

import android.content.Context
import android.util.AttributeSet
import com.daiwerystudio.chronos.R

/**
 * Класс, использующийся для хранения способа показа времени и даты. Используется в BindingAdapters.
 */
class TimeTextView(context: Context, attrs: AttributeSet):
    androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    var timeLocal: Int
    var timeStyle: Int
    var timeType: Int

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.TimeTextView,
            0, 0).apply {
            try {
                timeLocal = getInteger(R.styleable.TimeTextView_timeLocal, 0)
                timeStyle = getInteger(R.styleable.TimeTextView_timeStyle , 0)
                timeType = getInteger(R.styleable.TimeTextView_timeType , 0)
            } finally {
                recycle()
            }
        }
    }
}