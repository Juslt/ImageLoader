package com.wx.imageloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import com.wx.imageloader.disklrucache.DiskLruCache
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class DownloadImageManager private constructor() {
    private val lruCacheMemory = Runtime.getRuntime().maxMemory() / 8
    private var DISK_MAX_SIZE = 256 * 1024 * 1024L

    private val CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private val THREAD_POOL_EXECUTOR by lazy {
        ThreadPoolExecutor(
            CPU_COUNT + 1, CPU_COUNT * 2 + 1,
            10, TimeUnit.SECONDS, LinkedBlockingDeque<Runnable>(), threadFactory
        )
    }
    private val threadFactory = ThreadFactory { r -> Thread(r) }
    private val lruCache: LruCache<String, Bitmap> =
        object : LruCache<String, Bitmap>(lruCacheMemory.toInt()) {
            override fun sizeOf(key: String?, bitmap: Bitmap?): Int {
                return bitmap!!.rowBytes * bitmap.height / 1024
            }
        }


    private val mainHandler = Handler(Handler.Callback { msg ->
        if(msg.obj is BitmapResult){
           val result =  msg.obj as BitmapResult
            result.imageView.setImageBitmap(result.bitmap)
        }
        true
    })

    fun downloadImage(image: ImageView, url: String) {
        THREAD_POOL_EXECUTOR.execute(Runnable {
            //从本地缓存中取出bitmap
            if(lruCache.get(url)!=null){
               val bitmap =  getBitmapFromLRUCache(url)
                val msg = Message()
                msg.obj = BitmapResult(image,bitmap!!,url)
                mainHandler.sendMessage(msg)
                return@Runnable
            }
            //从本地文件中取出bitmap
            val cacheDirectory = geCacheFileDirectory(image.context)
            val diskLruCache = DiskLruCache.open(cacheDirectory, 1, 1, DISK_MAX_SIZE)
            val edit = diskLruCache.edit(hashKeyFromUrl(url))
            if(HttpUtil().downloadImageToStream(url,edit.newOutputStream(0))){
                edit.commit()
                getBitmapFromDiskLruCache(image,diskLruCache,url)
            }else{
                edit.abort()
            }
        })
    }

    private fun getBitmapFromDiskLruCache(
        imageView: ImageView,
        diskLruCache: DiskLruCache?,
        url: String
    ) {
        val snapshot = diskLruCache?.get(hashKeyFromUrl(url))
        snapshot?.let {
            val fd = (it.getInputStream(0) as FileInputStream).fd
            val bitmap = BitmapUtil().decodeBitmapSampleFromFileDescriptor(fd, 0, 0)
            Log.e("===","从fd中取出bitmap")
            if ( lruCache.get(url) == null) {
                saveBitmapToLRUCache(url, bitmap)
                Log.e("===","保存到缓存中")
            }

            val msg = Message()
            msg.obj = BitmapResult(imageView,bitmap,url)
            mainHandler.sendMessage(msg)
        }
    }

    private fun hashKeyFromUrl(url: String): String? {
        var cacheKey = ""
        try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(url.toByte())
            cacheKey = bytesToHexString(digest.digest())
            return cacheKey
        } catch (e: Exception) {
            cacheKey = url.hashCode().toString()
        }

        return cacheKey
    }

    private fun bytesToHexString(digest: ByteArray): String {
        val sb = StringBuilder()
        digest.forEachIndexed { index, byte ->
            val hex = Integer.toHexString(0xFF and byte.toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)

        }
        return sb.toString()
    }

    private fun geCacheFileDirectory(context: Context): File? {
        return context.applicationContext.externalCacheDir
    }

    private fun saveBitmapToLRUCache(url: String, bitmap: Bitmap?) {
        bitmap?.let {
            lruCache.put(url, bitmap)
        }
    }

    private fun getBitmapFromLRUCache(url: String): Bitmap? {
        Log.e("===","从缓存中取出bitmap")
        return lruCache.get(url)
    }

    fun bitmapSampleSize(context: Context) {
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
    }

    companion object {
        private val loadImageInstance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { DownloadImageManager() }
        fun getInstance(): DownloadImageManager {
            return loadImageInstance
        }
    }
}