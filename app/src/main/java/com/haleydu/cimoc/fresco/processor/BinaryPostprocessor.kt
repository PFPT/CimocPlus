package com.haleydu.cimoc.fresco.processor

import android.graphics.Bitmap
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.request.BasePostprocessor

class BinaryPostprocessor(private val url: String) : BasePostprocessor() {

    override fun process(sourceBitmap: Bitmap, bitmapFactory: PlatformBitmapFactory): CloseableReference<Bitmap> {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in 0 until height) {
            for (j in 0 until width) {
                val pixel = pixels[i * width + j]
                val red = (pixel and 0x00FF0000) shr 16
                val green = (pixel and 0x0000FF00) shr 8
                val blue = pixel and 0x000000FF
                val gray = red * 30 + green * 59 + blue * 11
                pixels[i * width + j] = if (gray > 21500) 0xFFFFFF else 0x000000
            }
        }
        val reference = bitmapFactory.createBitmap(width, height, Bitmap.Config.RGB_565)
        try {
            val bitmap = reference.get()
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return CloseableReference.cloneOrNull(reference)!!
        } finally {
            CloseableReference.closeSafely(reference)
        }
    }

    override fun getPostprocessorCacheKey(): CacheKey {
        return SimpleCacheKey(url + "binary")
    }
}
