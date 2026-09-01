package com.haleydu.cimoc.app.ui

import android.graphics.Bitmap
import kotlin.math.min

fun fixReaderBitmap(source: Bitmap, url: String, whiteEdge: Boolean): Bitmap {
    var working = decodeJmtt(source, url) ?: source
    if (whiteEdge) {
        val box = cropWhiteEdge(working)
        if (box[2] > 0 && box[3] > 0 && (box[0] != 0 || box[1] != 0 || box[2] != working.width || box[3] != working.height)) {
            working = Bitmap.createBitmap(working, box[0], box[1], box[2], box[3])
        }
    }
    return working
}

private fun decodeJmtt(source: Bitmap, url: String): Bitmap? {
    val scrambleId = 220980
    if (!url.contains("media/photos")) return null
    val start = url.indexOf("photos/") + 7
    val end = url.lastIndexOf("/")
    if (start < 7 || end <= start) return null
    val chapter = url.substring(start, end).toIntOrNull() ?: return null
    if (chapter <= scrambleId) return null
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

private fun cropWhiteEdge(bitmap: Bitmap): IntArray {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray((if (width > height) width else height) * 20)
    var y1 = 0
    var limit = height / 3
    while (y1 < limit) {
        bitmap.getPixels(pixels, 0, width, 0, y1, width, 1)
        if (!rowWhite(pixels, width)) break
        y1++
    }
    var y2 = height - 1
    limit = height * 2 / 3
    while (y2 > limit) {
        bitmap.getPixels(pixels, 0, width, 0, y2, width, 1)
        if (!rowWhite(pixels, width)) break
        y2--
    }
    val h = (y2 - y1 + 1).coerceAtLeast(1)
    var x1 = 0
    limit = width / 3
    while (x1 < limit) {
        bitmap.getPixels(pixels, 0, 1, x1, y1, 1, h)
        if (!rowWhite(pixels, h)) break
        x1++
    }
    var x2 = width - 1
    limit = width * 2 / 3
    while (x2 > limit) {
        bitmap.getPixels(pixels, 0, 1, x2, y1, 1, h)
        if (!rowWhite(pixels, h)) break
        x2--
    }
    val w = min(x2 - x1, width - x1).coerceAtLeast(1)
    val cropH = min(y2 - y1, height - y1).coerceAtLeast(1)
    return intArrayOf(x1, y1, w, cropH)
}

private fun rowWhite(pixels: IntArray, length: Int): Boolean {
    for (i in 0 until length) {
        val pixel = pixels[i]
        val gray = (pixel shr 16 and 0xFF) * 30 + (pixel shr 8 and 0xFF) * 59 + (pixel and 0xFF) * 11
        if (gray <= 21500) return false
    }
    return true
}
