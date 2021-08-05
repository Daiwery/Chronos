package com.daiwerystudio.chronos.ui

import android.widget.ImageView
import androidx.databinding.BindingAdapter


@BindingAdapter("android:colorFilter")
fun setColorFilter(imageView: ImageView, color: Int) {
    imageView.setColorFilter(color)
}
