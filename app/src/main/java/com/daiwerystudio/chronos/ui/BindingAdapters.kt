/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменена логика для показа времени в TextView.
*/

package com.daiwerystudio.chronos.ui

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.daiwerystudio.chronos.ui.widgets.TimeTextView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/*
 * В данном файле написаны все кастомные Binding Adapter, использующиеся в приложении.
 */


@BindingAdapter("android:colorFilter")
fun setColorFilter(imageView: ImageView, color: Int) {
    imageView.setColorFilter(color)
}


@BindingAdapter("android:booleanVisibility")
fun setBooleanVisibility(view: View, visibility: Boolean){
    if (visibility) view.visibility = View.VISIBLE
    else view.visibility = View.GONE
}


@BindingAdapter("android:booleanVisibilityInvisible")
fun setBooleanVisibilityInvisible(view: View, visibility: Boolean){
    if (visibility) view.visibility = View.VISIBLE
    else view.visibility = View.INVISIBLE
}


@BindingAdapter("android:activated")
fun setActivated(image: ImageView, activated: Boolean){
    image.isActivated = activated
}

/**
 * Устанавливает кастомному виджету текст, показывающий время.
 */
@BindingAdapter("android:textTime")
fun setTextTime(timeTextView: TimeTextView, millis: Long){
    var time = millis
    if (timeTextView.timeLocal == 1) time += TimeZone.getDefault().getOffset(System.currentTimeMillis())

    val formatStyle = when (timeTextView.timeStyle){
        0 -> FormatStyle.FULL
        1 -> FormatStyle.LONG
        2 -> FormatStyle.MEDIUM
        3 -> FormatStyle.SHORT
        else -> throw IllegalArgumentException("Invalid style")
    }

    val text = when (timeTextView.timeType){
        0 -> LocalTime.ofSecondOfDay((time/1000)%(60*60*24)).format(DateTimeFormatter.ofLocalizedTime(formatStyle))
        1 -> LocalDate.ofEpochDay(time/(1000*60*60*24)).format(DateTimeFormatter.ofLocalizedDate(formatStyle))
        else -> throw IllegalArgumentException("Invalid type")
    }

    timeTextView.text = text
}
