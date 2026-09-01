package com.haleydu.cimoc.fresco.processor

import android.graphics.Bitmap
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.request.BasePostprocessor
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.utils.StringUtils
import kotlin.math.ceil
import kotlin.math.max

class MangaPostprocessor @JvmOverloads constructor(
    private val mImage: ImageUrl,
    private val isPaging: Boolean,
    private val isPagingReverse: Boolean,
    private val isWhiteEdge: Boolean,
    private val isStreamTile: Boolean = false
) : BasePostprocessor() {

    private var mWidth = 0
    private var mHeight = 0
    private var mPosX = 0
    private var mPosY = 0
    private var isDone = false
    private var jmttIsDone = false

    override fun process(sourceBitmap: Bitmap, bitmapFactory: PlatformBitmapFactory): CloseableReference<Bitmap> {
        mWidth = sourceBitmap.width
        mHeight = sourceBitmap.height
        var reference = bitmapFactory.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565)
        decodeJMTTImage(sourceBitmap, reference)
        if (isStreamTile && !jmttIsDone && needStreamTiles()) {
            prepareStreamTiles()
            isDone = true
        } else if (isPaging && !jmttIsDone) {
            preparePaging(isPagingReverse)
            isDone = true
        }
        if (isWhiteEdge && !jmttIsDone) {
            prepareWhiteEdge(sourceBitmap)
            isDone = true
        }
        try {
            if (isDone) {
                if (!jmttIsDone) {
                    reference = bitmapFactory.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565)
                    processing(sourceBitmap, reference.get())
                }
                return CloseableReference.cloneOrNull(reference)!!
            } else if (jmttIsDone) {
                return CloseableReference.cloneOrNull(reference)!!
            }
        } finally {
            CloseableReference.closeSafely(reference)
        }
        return super.process(sourceBitmap, bitmapFactory)
    }

    private fun preparePaging(reverse: Boolean) {
        if (needHorizontalPaging()) {
            mWidth = mWidth / 2
            if (mImage.state == ImageUrl.STATE_NULL) {
                mImage.state = ImageUrl.STATE_PAGE_1
                AppEventBus.post(AppEvent(AppEvent.EVENT_PICTURE_PAGING, mImage))
            }
            mPosX = if (mImage.state == ImageUrl.STATE_PAGE_1) mWidth else 0
            if (reverse) {
                mPosX = if (mImage.state == ImageUrl.STATE_PAGE_1) 0 else mWidth
            }
            mPosY = 0
        } else if (needVerticalPaging()) {
            mHeight = mHeight / 2
            if (mImage.state == ImageUrl.STATE_NULL) {
                mImage.state = ImageUrl.STATE_PAGE_1
                AppEventBus.post(AppEvent(AppEvent.EVENT_PICTURE_PAGING, mImage))
            }
            mPosX = 0
            mPosY = if (mImage.state == ImageUrl.STATE_PAGE_1) 0 else mHeight
            if (reverse) {
                mPosY = if (mImage.state == ImageUrl.STATE_PAGE_1) mHeight else 0
            }
        }
    }

    private fun needVerticalPaging(): Boolean {
        return mHeight > 3 * mWidth
    }

    private fun needHorizontalPaging(): Boolean {
        return mWidth > 1.2 * mHeight
    }

    private fun needStreamTiles(): Boolean {
        return mHeight > STREAM_TILE_HEIGHT || mHeight > 3 * mWidth
    }

    private fun prepareStreamTiles() {
        val sourceH = mHeight
        val tiles = max(1, ceil(sourceH / STREAM_TILE_HEIGHT.toDouble()).toInt())
        if (tiles <= 1) {
            return
        }
        if (mImage.state == ImageUrl.STATE_NULL) {
            mImage.state = ImageUrl.STATE_PAGE_1
            AppEventBus.post(AppEvent(AppEvent.EVENT_PICTURE_PAGING, mImage, tiles))
        }
        val index = max(mImage.state, 1)
        mPosX = 0
        mPosY = (index - 1) * STREAM_TILE_HEIGHT
        if (mPosY >= sourceH) {
            mPosY = max(0, sourceH - STREAM_TILE_HEIGHT)
        }
        mHeight = minOf(STREAM_TILE_HEIGHT, sourceH - mPosY)
    }

    private fun prepareWhiteEdge(bitmap: Bitmap) {
        var y1: Int
        var y2: Int
        var x1: Int
        var x2: Int
        val pixels = IntArray((if (mWidth > mHeight) mWidth else mHeight) * 20)
        var limit = mPosY + mHeight / 3
        y1 = mPosY
        while (y1 < limit) {
            bitmap.getPixels(pixels, 0, mWidth, mPosX, y1, mWidth, 1)
            if (!oneDimensionScan(pixels, mWidth)) {
                bitmap.getPixels(pixels, 0, mWidth, 0, y1, mWidth, 10)
                if (!twoDimensionScan(pixels, mWidth, false, false)) {
                    break
                }
                y1 += 9
            }
            ++y1
        }
        limit = mPosY + mHeight * 2 / 3
        y2 = mPosY + mHeight - 1
        while (y2 > limit) {
            bitmap.getPixels(pixels, 0, mWidth, mPosX, y2, mWidth, 1)
            if (!oneDimensionScan(pixels, mWidth)) {
                bitmap.getPixels(pixels, 0, mWidth, 0, y2 - 9, mWidth, 10)
                if (!twoDimensionScan(pixels, mWidth, false, true)) {
                    break
                }
                y2 -= 9
            }
            --y2
        }
        val h = y2 - y1 + 1
        limit = mPosX + mWidth / 3
        x1 = mPosX
        while (x1 < limit) {
            bitmap.getPixels(pixels, 0, 1, x1, y1, 1, h)
            if (!oneDimensionScan(pixels, h)) {
                bitmap.getPixels(pixels, 0, 10, x1, y1, 10, h)
                if (!twoDimensionScan(pixels, h, true, false)) {
                    break
                }
                x1 += 9
            }
            ++x1
        }
        limit = mPosX + mWidth * 2 / 3
        x2 = mPosX + mWidth - 1
        while (x2 > limit) {
            bitmap.getPixels(pixels, 0, 1, x2, y1, 1, h)
            if (!oneDimensionScan(pixels, h)) {
                bitmap.getPixels(pixels, 0, 10, x2 - 9, y1, 10, h)
                if (!twoDimensionScan(pixels, h, true, true)) {
                    break
                }
                x2 -= 9
            }
            --x2
        }
        mWidth = x2 - x1
        mHeight = y2 - y1
        mPosX = x1
        mPosY = y1
    }

    private fun processing(src: Bitmap, dst: Bitmap) {
        val unit = mHeight / 20
        val remain = mHeight - 20 * unit
        val pixels = IntArray((if (remain > unit) remain else unit) * mWidth)
        for (j in 0 until 20) {
            src.getPixels(pixels, 0, mWidth, mPosX, mPosY + j * unit, mWidth, unit)
            dst.setPixels(pixels, 0, mWidth, 0, j * unit, mWidth, unit)
        }
        if (remain > 0) {
            src.getPixels(pixels, 0, mWidth, mPosX, mPosY + 20 * unit, mWidth, remain)
            dst.setPixels(pixels, 0, mWidth, 0, 20 * unit, mWidth, remain)
        }
    }

    override fun getPostprocessorCacheKey(): CacheKey {
        return SimpleCacheKey(
            StringUtils.format(
                "%s-post-%d-%d-%b-%b-%b-%b",
                mImage.getUrl(),
                mImage.id,
                mImage.state,
                isPaging,
                isPagingReverse,
                isWhiteEdge,
                isStreamTile
            )
        )
    }

    private fun oneDimensionScan(pixels: IntArray, length: Int): Boolean {
        for (i in 0 until length) {
            if (!isWhite(pixels[i])) {
                return false
            }
        }
        return true
    }

    private fun twoDimensionScan(pixels: IntArray, length: Int, vertical: Boolean, reverse: Boolean): Boolean {
        if (length < 20) {
            return false
        }
        val value = IntArray(20)
        var result = 0
        for (i in 0 until length) {
            if (result > 60) {
                return false
            }
            result -= value[i % 20]
            value[i % 20] = 0
            for (j in 0 until 10) {
                val k = if (vertical) (i * 10 + j) else (j * length + i)
                value[i % 20] += getValue(isWhite(pixels[k]), reverse, j)
            }
            result += value[i % 20]
        }
        return true
    }

    private fun getValue(white: Boolean, reverse: Boolean, pos: Int): Int {
        if (white) {
            return 0
        }
        return if (pos < 2) {
            if (reverse) 3 else 0
        } else if (pos < 5) {
            if (reverse) 2 else 1
        } else if (pos < 8) {
            if (reverse) 1 else 2
        } else {
            if (reverse) 0 else 3
        }
    }

    private fun isWhite(pixel: Int): Boolean {
        val red = (pixel and 0x00FF0000) shr 16
        val green = (pixel and 0x0000FF00) shr 8
        val blue = pixel and 0x000000FF
        val gray = red * 30 + green * 59 + blue * 11
        return gray > 21500
    }

    fun decodeJMTTImage(sourceBitmap: Bitmap, reference: CloseableReference<Bitmap>) {
        val url = mImage.getUrl()
        val scrambleId = 220980
        if (url.contains("media/photos") &&
            url.substring(url.indexOf("photos/") + 7, url.lastIndexOf("/")).toInt() > scrambleId
        ) {
            val resultBitmap = reference.get()
            val rows = 10
            val remainder = mHeight % rows
            for (x in 0 until 10) {
                var chunkHeight = Math.floor((mHeight / rows).toDouble()).toInt()
                var py = chunkHeight * x
                val y = mHeight - chunkHeight * (x + 1) - remainder
                if (x == 0) {
                    chunkHeight = chunkHeight + remainder
                } else {
                    py = py + remainder
                }
                val pixels = IntArray(chunkHeight * mWidth)
                sourceBitmap.getPixels(pixels, 0, mWidth, 0, y, mWidth, chunkHeight)
                resultBitmap.setPixels(pixels, 0, mWidth, 0, py, mWidth, chunkHeight)
            }
            jmttIsDone = true
        }
    }

    companion object {
        const val STREAM_TILE_HEIGHT = 2048
    }
}
