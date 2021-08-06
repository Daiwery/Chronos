/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.daiwerystudio.chronos.R

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
 * Устанавливает видимость View в зависимости от булевой переменной. Если true - VISIBLE, false - GONE
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

