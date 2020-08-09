package com.wx.imageloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class HttpUtil {
    val IO_BUFFER_SIZE = 8 * 1024
    fun getBitmapFromUrl(urlString: String): Bitmap? {
        Log.e("===", "網絡獲取圖片")
        var bufferedInputStream: BufferedInputStream? = null
        var httpURLConnection: HttpURLConnection? = null
        var bitmap: Bitmap? = null
        try {
            val url = URL(urlString)
            httpURLConnection = url.openConnection() as HttpURLConnection?
            bufferedInputStream =
                BufferedInputStream(httpURLConnection?.inputStream!!, IO_BUFFER_SIZE)
            bitmap = BitmapFactory.decodeStream(bufferedInputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bufferedInputStream?.close()
            httpURLConnection?.disconnect()
        }
        return bitmap
    }

    fun downloadImageToStream(urlString: String, outPutStream: OutputStream): Boolean {
        var bufferedInputStream: BufferedInputStream? = null
        var bufferedOutputStream: BufferedOutputStream? = null
        var httpURLConnection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            httpURLConnection = url.openConnection() as HttpURLConnection?
            bufferedInputStream =
                BufferedInputStream(httpURLConnection?.inputStream!!, IO_BUFFER_SIZE)
            bufferedOutputStream = BufferedOutputStream(outPutStream)
            var b = 0
            while (b != -1) {
                b = bufferedInputStream.read()
                bufferedOutputStream.write(b)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bufferedInputStream?.close()
            bufferedOutputStream?.close()
            httpURLConnection?.disconnect()
        }
        return false
    }
}