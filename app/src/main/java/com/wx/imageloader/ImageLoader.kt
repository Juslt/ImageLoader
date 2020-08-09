package com.wx.imageloader

import android.widget.ImageView

class ImageLoader {
    fun loadImage(image:ImageView,url:String){
        DownloadImageManager.getInstance().downloadImage(image,url)
    }
}