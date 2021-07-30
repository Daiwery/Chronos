package com.daiwerystudio.chronos

import android.widget.ImageView
import androidx.databinding.BindingAdapter


@BindingAdapter("android:colorFilter")
fun setColorFilter(imageView: ImageView, color: Int) {
    imageView.setColorFilter(color)
}
