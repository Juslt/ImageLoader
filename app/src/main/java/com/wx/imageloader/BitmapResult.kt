package com.wx.imageloader

import android.graphics.Bitmap
import android.widget.ImageView

data class BitmapResult(
    val imageView:ImageView,
    val bitmap:Bitmap,
    val url:String
)