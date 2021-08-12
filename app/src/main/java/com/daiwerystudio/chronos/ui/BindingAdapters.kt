/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/*
 * В данном файле написаны все кастомные Binding Adapter, использующиеся в приложении.
 */

/**
 * Ставит цветовой фильтр на Image View. Цветовой фильтр обычный, поэтому
 * все пиксели картинки (кроме прозрачных) красятся в заданный увет.
 */
@BindingAdapter("android:colorFilter")
fun setColorFilter(imageView: ImageView, color: Int) {
    imageView.setColorFilter(color)
}

/**
 * Устанавливает видимость View в зависимости от булевой переменной.
 * Если true - VISIBLE, false - GONE.
 */
@BindingAdapter("android:booleanVisibility")
fun setBooleanVisibility(view: View, visibility: Boolean){
    if (visibility) view.visibility = View.VISIBLE
    else view.visibility = View.GONE
}

/**
 * Устанавливает isActivated ImageView. Используется в GoalFragment для ProgressGoal.
 * При переписывании этого виджета, данный BindingAdapter необходимо удалить.
 */
@BindingAdapter("android:activated")
fun setActivated(image: ImageView, activated: Boolean){
    image.isActivated = activated
}

/**
 * Устанавливает время в TextView.
 */
@BindingAdapter("android:textTime")
fun setTextTime(textView: TextView, time: Long){
    // Подробности в updateStartEndTimes в DayScheduleFragment
    if (time < 0)  textView.text = "???"
    else {
        val localTime = time+TimeZone.getDefault().getOffset(System.currentTimeMillis())
        textView.text = LocalTime.ofSecondOfDay(localTime%(24*60*60))
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
}

/**
 * Устанавливает время в TextView с секундами.
 */
@BindingAdapter("android:textTimeWithSeconds")
fun setTextTimeWithSeconds(textView: TextView, time: Long){
    // Подробности в updateStartEndTimes в DayScheduleFragment
    if (time < 0)  textView.text = "???"
    else {
        val localTime = time+TimeZone.getDefault().getOffset(System.currentTimeMillis())
        textView.text = LocalTime.ofSecondOfDay(localTime%(24*60*60))
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
    }
}

/**
 * Устанавливает локальное время в TextView.
 */
@BindingAdapter("android:textLocalTime")
fun setTextLocalTime(textView: TextView, time: Long){
    val localTime = time+TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000
    textView.text = LocalTime.ofSecondOfDay(localTime%(24*60*60))
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}

/**
 * Устанавливает локальную дату в TextView.
 */
@BindingAdapter("android:textDate")
fun setTextDate(textView: TextView, time: Long){
    val localTime = time+TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000
    textView.text = LocalDate.ofEpochDay(localTime/(24*60*60))
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}