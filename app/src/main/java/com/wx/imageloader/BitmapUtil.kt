package com.wx.imageloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.FileDescriptor

class BitmapUtil {
    fun decodeBitmapSampleFromFileDescriptor(fileDescriptor: FileDescriptor,reqHeight:Int,reqWidth:Int):Bitmap{
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inSampleSize = 1
        BitmapFactory.decodeFileDescriptor(fileDescriptor,null, options)

        options.inSampleSize = calculateInSampleSize(options,reqHeight,reqWidth)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqHeight: Int,
        reqWidth: Int
    ):Int {
        if(reqHeight==0 && reqWidth==0){
            return  1
        }

        var width = options.outWidth
        var height = options.outHeight
        var inSampleSize = 1

        if(width>reqWidth || height>reqHeight){
            var halfWidth = width/2
            var halfHeight = height/2
            while(halfWidth/inSampleSize>reqWidth && halfHeight/inSampleSize>reqHeight){
                inSampleSize*2
            }
        }
        Log.e("===","inSampleSize:${inSampleSize}")
        return inSampleSize
    }
}