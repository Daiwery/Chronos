package com.daiwerystudio.chronos

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.jaredrummler.android.colorpicker.ColorPickerView


@BindingAdapter("android:colorFilter")
fun setColorFilter(imageView: ImageView, color: Int) {
    imageView.setColorFilter(color)
}

@BindingAdapter("android:color")
fun setColor(colorPickerView: ColorPickerView, color: Int) {
    colorPickerView.setColor(color)
}
