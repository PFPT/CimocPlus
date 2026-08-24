package com.haleydu.cimoc.ui.reader

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.fresco.processor.MangaPostprocessor
import com.haleydu.cimoc.model.ImageUrl
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MangaTransformation(
    private val image: ImageUrl,
    private val paging: Boolean,
    private val pagingReverse: Boolean,
    private val whiteEdge: Boolean,
    private val streamTile: Boolean
) : Transformation {

    override val cacheKey: String =
        "${image.url}-${image.id}-${image.state}-$paging-$pagingReverse-$whiteEdge-$streamTile"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        var width = input.width
        var height = input.height
        var posX = 0
        var posY = 0
        var done = false
        var jmttDone = false
        var working = input

        val jmtt = decodeJmtt(input)
        if (jmtt != null) {
            working = jmtt
            jmttDone = true
            width = working.width
            height = working.height
        }

        if (streamTile && !jmttDone && (height > MangaPostprocessor.STREAM_TILE_HEIGHT || height > 3 * width)) {
            val sourceH = height
            val tiles = max(1, ceil(sourceH / MangaPostprocessor.STREAM_TILE_HEIGHT.toDouble()).toInt())
            if (tiles > 1) {
                if (image.state == ImageUrl.STATE_NULL) {
                    image.state = ImageUrl.STATE_PAGE_1
                    AppEventBus.post(AppEvent(AppEvent.EVENT_PICTURE_PAGING, image, tiles))
                }
                val index = max(image.state, 1)
                posX = 0
                posY = (index - 1) * MangaPostprocessor.STREAM_TILE_HEIGHT
                if (posY >= sourceH) {
                    posY = max(0, sourceH - MangaPostprocessor.STREAM_TILE_HEIGHT)
                }
                height = min(MangaPostprocessor.STREAM_TILE_HEIGHT, sourceH - posY)
                done = true
            }
        } else if (paging && !jmttDone) {
            if (width > 1.2 * height) {
                width /= 2
                if (image.state == ImageUrl.STATE_NULL) {
                    image.state = ImageUrl.STATE_PAGE_1
                    AppEventBus.post(AppEvent(AppEvent.EVENT_PICTURE_PAGING, image, 2))
                }
                posX = if (image.state == ImageUrl.STATE_PAGE_1) width else 0
                if (pagingReverse) {
                    posX = if (image.state == ImageUrl.STATE_PAGE_1) 0 else width
                }
                posY = 0
                done = true
            } else if (height > 3 * width) {
                height /= 2
                if (image.state == ImageUrl.STATE_NULL) {
                    image.state = ImageUrl.STATE_PAGE_1
                    AppEventBus.post(AppEvent(AppEvent.EVENT_PICTURE_PAGING, image, 2))
                }
                posX = 0
                posY = if (image.state == ImageUrl.STATE_PAGE_1) 0 else height
                if (pagingReverse) {
                    posY = if (image.state == ImageUrl.STATE_PAGE_1) height else 0
                }
                done = true
            }
        }

        if (whiteEdge && !jmttDone) {
            val box = cropWhiteEdge(working, posX, posY, width, height)
            posX = box[0]
            posY = box[1]
            width = box[2]
            height = box[3]
            done = true
        }

        if (jmttDone && !done) {
            return working
        }
        if (!done) {
            return input
        }
        return crop(working, posX, posY, width, height)
    }

    private fun crop(src: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        val w = width.coerceAtMost(src.width - x).coerceAtLeast(1)
        val h = height.coerceAtMost(src.height - y).coerceAtLeast(1)
        val dst = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val unit = h / 20
        val remain = h - 20 * unit
        val pixels = IntArray((if (remain > unit) remain else unit) * w)
        for (j in 0 until 20) {
            src.getPixels(pixels, 0, w, x, y + j * unit, w, unit)
            dst.setPixels(pixels, 0, w, 0, j * unit, w, unit)
        }
        if (remain > 0) {
            src.getPixels(pixels, 0, w, x, y + 20 * unit, w, remain)
            dst.setPixels(pixels, 0, w, 0, 20 * unit, w, remain)
        }
        return dst
    }

    private fun decodeJmtt(source: Bitmap): Bitmap? {
        val url = image.url ?: return null
        val scrambleId = 220980
        if (!url.contains("media/photos")) {
            return null
        }
        val start = url.indexOf("photos/") + 7
        val end = url.lastIndexOf("/")
        if (start < 7 || end <= start) {
            return null
        }
        val chapter = url.substring(start, end).toIntOrNull() ?: return null
        if (chapter <= scrambleId) {
            return null
        }
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val rows = 10
        val remainder = height % rows
        for (x in 0 until 10) {
            var chunkHeight = height / rows
            var py = chunkHeight * x
            var y = height - chunkHeight * (x + 1) - remainder
            if (x == 0) {
                chunkHeight += remainder
            } else {
                py += remainder
            }
            val pixels = IntArray(chunkHeight * width)
            source.getPixels(pixels, 0, width, 0, y, width, chunkHeight)
            result.setPixels(pixels, 0, width, 0, py, width, chunkHeight)
        }
        return result
    }

    private fun cropWhiteEdge(bitmap: Bitmap, startX: Int, startY: Int, startW: Int, startH: Int): IntArray {
        var posX = startX
        var posY = startY
        var width = startW
        var height = startH
        val pixels = IntArray((if (width > height) width else height) * 20)
        var limit = posY + height / 3
        var y1 = posY
        while (y1 < limit) {
            bitmap.getPixels(pixels, 0, width, posX, y1, width, 1)
            if (!oneDimensionScan(pixels, width)) {
                bitmap.getPixels(pixels, 0, width, 0, y1, width, 10)
                if (!twoDimensionScan(pixels, width, vertical = false, reverse = false)) {
                    break
                }
                y1 += 9
            }
            y1++
        }
        limit = posY + height * 2 / 3
        var y2 = posY + height - 1
        while (y2 > limit) {
            bitmap.getPixels(pixels, 0, width, posX, y2, width, 1)
            if (!oneDimensionScan(pixels, width)) {
                bitmap.getPixels(pixels, 0, width, 0, y2 - 9, width, 10)
                if (!twoDimensionScan(pixels, width, vertical = false, reverse = true)) {
                    break
                }
                y2 -= 9
            }
            y2--
        }
        val h = y2 - y1 + 1
        limit = posX + width / 3
        var x1 = posX
        while (x1 < limit) {
            bitmap.getPixels(pixels, 0, 1, x1, y1, 1, h)
            if (!oneDimensionScan(pixels, h)) {
                bitmap.getPixels(pixels, 0, 10, x1, y1, 10, h)
                if (!twoDimensionScan(pixels, h, vertical = true, reverse = false)) {
                    break
                }
                x1 += 9
            }
            x1++
        }
        limit = posX + width * 2 / 3
        var x2 = posX + width - 1
        while (x2 > limit) {
            bitmap.getPixels(pixels, 0, 1, x2, y1, 1, h)
            if (!oneDimensionScan(pixels, h)) {
                bitmap.getPixels(pixels, 0, 10, x2 - 9, y1, 10, h)
                if (!twoDimensionScan(pixels, h, vertical = true, reverse = true)) {
                    break
                }
                x2 -= 9
            }
            x2--
        }
        width = x2 - x1
        height = y2 - y1
        posX = x1
        posY = y1
        return intArrayOf(posX, posY, width, height)
    }

    private fun oneDimensionScan(pixels: IntArray, length: Int): Boolean {
        for (i in 0 until length) {
            if (!isWhite(pixels[i])) return false
        }
        return true
    }

    private fun twoDimensionScan(pixels: IntArray, length: Int, vertical: Boolean, reverse: Boolean): Boolean {
        if (length < 20) return false
        val value = IntArray(20)
        var result = 0
        for (i in 0 until length) {
            if (result > 60) return false
            result -= value[i % 20]
            value[i % 20] = 0
            for (j in 0 until 10) {
                val k = if (vertical) i * 10 + j else j * length + i
                value[i % 20] += getValue(isWhite(pixels[k]), reverse, j)
            }
            result += value[i % 20]
        }
        return true
    }

    private fun getValue(white: Boolean, reverse: Boolean, pos: Int): Int {
        if (white) return 0
        return when {
            pos < 2 -> if (reverse) 3 else 0
            pos < 5 -> if (reverse) 2 else 1
            pos < 8 -> if (reverse) 1 else 2
            else -> if (reverse) 0 else 3
        }
    }

    private fun isWhite(pixel: Int): Boolean {
        val red = pixel shr 16 and 0xFF
        val green = pixel shr 8 and 0xFF
        val blue = pixel and 0xFF
        val gray = red * 30 + green * 59 + blue * 11
        return gray > 21500
    }
}
