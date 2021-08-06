/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import android.widget.ImageView
import androidx.databinding.BindingAdapter

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
