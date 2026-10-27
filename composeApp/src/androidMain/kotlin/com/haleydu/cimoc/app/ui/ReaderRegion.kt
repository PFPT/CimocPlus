package com.haleydu.cimoc.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun VisibleRegionImage(
    session: RegionSession,
    modifier: Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var shown by remember(session) { mutableStateOf<Bitmap?>(null) }
    var topPx by remember(session) { mutableIntStateOf(0) }
    var heightPx by remember(session) { mutableIntStateOf(0) }
    var job by remember(session) { mutableStateOf<Job?>(null) }
    DisposableEffect(session) {
        onDispose {
            job?.cancel()
            shown?.takeIf { !it.isRecycled }?.recycle()
        }
    }
    Box(
        modifier.onGloballyPositioned { coords ->
            if (!coords.isAttached || session.closed) return@onGloballyPositioned
            val itemHeight = coords.size.height
            val itemWidth = coords.size.width
            if (itemHeight <= 0 || itemWidth <= 0) return@onGloballyPositioned
            val rootHeight = coords.rootSize().height
            val top = coords.positionInRoot().y
            val visibleTop = (-top).roundToInt().coerceIn(0, itemHeight)
            val visibleBottom = (rootHeight - top).roundToInt().coerceIn(0, itemHeight)
            if (visibleBottom <= visibleTop) return@onGloballyPositioned
            if (covers(topPx, heightPx, visibleTop, visibleBottom, itemHeight)) return@onGloballyPositioned
            val request = ++session.request
            job?.cancel()
            job = scope.launch {
                val band = withContext(Dispatchers.IO) {
                    session.decode(itemWidth, itemHeight, rootHeight, visibleTop, visibleBottom)
                }
                if (!isActive || request != session.request || band == null) {
                    band?.bitmap?.takeIf { !it.isRecycled }?.recycle()
                    return@launch
                }
                val previous = shown
                shown = band.bitmap
                topPx = band.topPx
                heightPx = band.heightPx
                if (previous != null && previous !== band.bitmap) {
                    withFrameNanos { }
                    withFrameNanos { }
                    if (!previous.isRecycled && previous !== shown) previous.recycle()
                }
            }
        }
    ) {
        val frame = shown
        if (frame != null && !frame.isRecycled && heightPx > 0) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(0, topPx) }
                    .fillMaxWidth()
                    .height(with(density) { heightPx.toDp() }),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

internal class RegionSession(
    private val decoder: BitmapRegionDecoder,
    private val pfd: ParcelFileDescriptor?,
    val imageWidth: Int,
    val imageHeight: Int
) {
    private val gate = Any()
    var request = 0
    @Volatile var closed = false

    fun ratio(): Float? {
        if (imageWidth <= 0 || imageHeight <= 0) return null
        return imageWidth.toFloat() / imageHeight.toFloat()
    }

    fun decode(viewWidth: Int, viewHeight: Int, viewport: Int, visibleTop: Int, visibleBottom: Int): RegionBand? {
        synchronized(gate) {
            if (closed || imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return null
            val pad = (visibleBottom - visibleTop).coerceAtLeast(viewport / 2).coerceAtLeast(1)
            var top = (visibleTop - pad).coerceAtLeast(0)
            var bottom = (visibleBottom + pad).coerceAtMost(viewHeight)
            if (viewHeight <= viewport * 3) {
                top = 0
                bottom = viewHeight
            }
            if (bottom <= top) return null
            val scale = imageHeight.toFloat() / viewHeight.toFloat()
            val srcTop = (top * scale).roundToInt().coerceIn(0, imageHeight - 1)
            val srcBottom = (bottom * scale).roundToInt().coerceIn(srcTop + 1, imageHeight)
            var sample = sampleSize(imageWidth, viewWidth)
            val srcHeight = srcBottom - srcTop
            while (sample < 16) {
                val pixels = (imageWidth / sample).toLong() * (srcHeight / sample).coerceAtLeast(1)
                if (pixels * 2L <= 16L * 1024L * 1024L) break
                sample *= 2
            }
            val rect = Rect(0, srcTop, imageWidth, srcBottom)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = try {
                decoder.decodeRegion(rect, opts)
            } catch (e: Exception) {
                null
            } ?: return null
            return RegionBand(decoded, top, bottom - top)
        }
    }

    fun close() {
        synchronized(gate) {
            if (closed) return
            closed = true
            decoder.recycle()
            runCatching { pfd?.close() }
        }
    }

    companion object {
        fun open(context: Context, uri: Uri): RegionSession? {
            return try {
                val (decoder, pfd) = openDecoder(context, uri) ?: return null
                if (decoder.width <= 0 || decoder.height <= 0) {
                    decoder.recycle()
                    runCatching { pfd?.close() }
                    return null
                }
                RegionSession(decoder, pfd, decoder.width, decoder.height)
            } catch (e: Exception) {
                null
            }
        }
    }
}

internal class RegionBand(val bitmap: Bitmap, val topPx: Int, val heightPx: Int)

private fun covers(top: Int, height: Int, visibleTop: Int, visibleBottom: Int, limit: Int): Boolean {
    if (height <= 0) return false
    val bottom = top + height
    if (visibleTop < top || visibleBottom > bottom) return false
    val pad = (visibleBottom - visibleTop) / 5
    val topOk = top == 0 || visibleTop - top >= pad
    val bottomOk = bottom >= limit || bottom - visibleBottom >= pad
    return topOk && bottomOk
}

private fun sampleSize(imageWidth: Int, viewWidth: Int): Int {
    var sample = 1
    val target = viewWidth.coerceAtLeast(1)
    while (sample < 16 && imageWidth / (sample * 2) >= target) sample *= 2
    return sample
}

private fun openDecoder(context: Context, uri: Uri): Pair<BitmapRegionDecoder, ParcelFileDescriptor?>? {
    if (uri.scheme == "content") {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val decoder = newRegionDecoder(pfd)
        if (decoder == null) {
            pfd.close()
            return null
        }
        return decoder to pfd
    }
    val path = uri.path ?: return null
    val decoder = newRegionDecoder(path) ?: return null
    return decoder to null
}

private fun newRegionDecoder(path: String): BitmapRegionDecoder? {
    return if (Build.VERSION.SDK_INT >= 31) {
        BitmapRegionDecoder.newInstance(path)
    } else {
        @Suppress("DEPRECATION")
        BitmapRegionDecoder.newInstance(path, false)
    }
}

private fun newRegionDecoder(pfd: ParcelFileDescriptor): BitmapRegionDecoder? {
    return if (Build.VERSION.SDK_INT >= 31) {
        BitmapRegionDecoder.newInstance(pfd)
    } else {
        @Suppress("DEPRECATION")
        BitmapRegionDecoder.newInstance(pfd.fileDescriptor, false)
    }
}

private fun androidx.compose.ui.layout.LayoutCoordinates.rootSize(): androidx.compose.ui.unit.IntSize {
    var current = this
    var parent = current.parentLayoutCoordinates
    while (parent != null) {
        current = parent
        parent = current.parentLayoutCoordinates
    }
    return current.size
}
