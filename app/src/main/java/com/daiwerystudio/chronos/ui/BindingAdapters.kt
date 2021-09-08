/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменена логика для показа времени в TextView.
*
* Дата изменения: 08.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменена логика для показа времени в TextView.
*/

package com.daiwerystudio.chronos.ui

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
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
