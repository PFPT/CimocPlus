package com.haleydu.cimoc.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import coil3.disk.DiskCache
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.size.Size
import coil3.toBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

internal class TileSlot {
    var snapshot: DiskCache.Snapshot? = null

    fun replace(next: DiskCache.Snapshot?) {
        val previous = snapshot
        snapshot = next
        if (previous != null && previous !== next) previous.close()
    }

    fun close() {
        snapshot?.close()
        snapshot = null
    }
}

internal class OwnedFile {
    var file: File? = null

    fun replace(next: File?) {
        val previous = file
        file = next
        if (previous != null && previous != next) previous.delete()
    }

    fun close() {
        file?.delete()
        file = null
    }
}

internal class PreparedPage(
    val uri: Uri?,
    val snapshot: DiskCache.Snapshot?,
    val ownedFile: File?,
    val ratio: Float?
) {
    fun discard() {
        snapshot?.close()
        ownedFile?.delete()
    }
}

internal suspend fun prepareDisplay(
    context: Context,
    url: String,
    headers: Map<String, String>,
    whiteEdge: Boolean
): PreparedPage? {
    val local = localTileUri(url)
    if (needsReaderPixelFix(url, whiteEdge)) {
        val decoded = decodeOriginal(context, url, headers)
        if (decoded != null) {
            val fixed = try {
                fixReaderBitmap(decoded.bitmap, url, whiteEdge)
            } catch (e: OutOfMemoryError) {
                decoded.bitmap.takeIf { !it.isRecycled }?.recycle()
                decoded.snapshot?.close()
                null
            }
            if (fixed == null) return cachedOrLocal(context, url, headers, local)
            if (fixed === decoded.bitmap) {
                fixed.takeIf { !it.isRecycled }?.recycle()
                val uri = local ?: decoded.uri
                val snapshot = if (local == null) decoded.snapshot else null
                if (local != null) decoded.snapshot?.close()
                if (uri == null) return null
                return PreparedPage(uri, snapshot, null, readBounds(context, uri)?.ratio())
            }
            val file = writeFixedJpeg(context, fixed)
            if (decoded.bitmap !== fixed) decoded.bitmap.takeIf { !it.isRecycled }?.recycle()
            fixed.takeIf { !it.isRecycled }?.recycle()
            decoded.snapshot?.close()
            if (file == null) return null
            val uri = Uri.fromFile(file)
            return PreparedPage(uri, null, file, readBounds(context, uri)?.ratio())
        }
    }
    return cachedOrLocal(context, url, headers, local)
}

internal suspend fun decodeReaderBitmap(
    context: Context,
    url: String,
    headers: Map<String, String>,
    whiteEdge: Boolean
): Bitmap? {
    val builder = ImageRequest.Builder(context)
        .data(url)
        .applyReaderDecode(context)
    applyImageHeaders(builder, headers)
    val result = context.imageLoader.execute(builder.build())
    return when (result) {
        is SuccessResult -> try {
            val source = result.image.toBitmap()
            val fixed = fixReaderBitmap(source, url, whiteEdge)
            val owned = fixed.copy(fixed.config ?: Bitmap.Config.ARGB_8888, false)
            if (fixed !== source) fixed.recycle()
            owned
        } catch (e: OutOfMemoryError) {
            null
        }
        is ErrorResult -> {
            if (result.throwable is CancellationException) throw result.throwable
            null
        }
        else -> null
    }
}

internal fun decodeScaled(context: Context, uri: Uri): Bitmap? {
    val bounds = readBounds(context, uri) ?: return null
    val metrics = context.resources.displayMetrics
    val maxW = metrics.widthPixels.coerceAtLeast(1)
    val maxH = (metrics.heightPixels * 4).coerceAtLeast(1)
    var sample = 1
    while (sample < 32 && (bounds.first / (sample * 2) >= maxW || bounds.second / (sample * 2) >= maxH)) {
        if (bounds.first / sample <= maxW && bounds.second / sample <= maxH) break
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return try {
        when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            else -> BitmapFactory.decodeFile(uri.path, opts)
        }
    } catch (e: OutOfMemoryError) {
        null
    }
}

internal fun ImageRequest.Builder.applyDiskWarm() = apply {
    size(Size(1, 1))
    precision(Precision.INEXACT)
    allowHardware(false)
    memoryCachePolicy(CachePolicy.DISABLED)
    diskCachePolicy(CachePolicy.ENABLED)
}

internal fun applyImageHeaders(builder: ImageRequest.Builder, headers: Map<String, String>) {
    if (headers.isEmpty()) return
    builder.httpHeaders(
        NetworkHeaders.Builder().apply {
            headers.forEach { (k, v) -> set(k, v) }
        }.build()
    )
}

private suspend fun cachedOrLocal(
    context: Context,
    url: String,
    headers: Map<String, String>,
    local: Uri?
): PreparedPage? {
    if (local != null) {
        return PreparedPage(local, null, null, readBounds(context, local)?.ratio())
    }
    val cached = openCachedTile(context, url, headers) ?: return null
    return PreparedPage(cached.uri, cached.snapshot, null, readBounds(context, cached.uri)?.ratio())
}

private fun Pair<Int, Int>.ratio(): Float? {
    if (first <= 0 || second <= 0) return null
    return first.toFloat() / second.toFloat()
}

private class DecodedOriginal(
    val bitmap: Bitmap,
    val snapshot: DiskCache.Snapshot?,
    val uri: Uri?
)

private suspend fun decodeOriginal(
    context: Context,
    url: String,
    headers: Map<String, String>
): DecodedOriginal? {
    val loader = context.imageLoader
    val builder = ImageRequest.Builder(context)
        .data(url)
        .size(Size.ORIGINAL)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .allowHardware(false)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
    applyImageHeaders(builder, headers)
    val result = try {
        loader.execute(builder.build())
    } catch (e: CancellationException) {
        throw e
    } catch (e: OutOfMemoryError) {
        return null
    }
    if (result is ErrorResult) {
        if (result.throwable is CancellationException) throw result.throwable
        return null
    }
    val success = result as? SuccessResult ?: return null
    val bitmap = try {
        success.image.toBitmap()
    } catch (e: OutOfMemoryError) {
        return null
    }
    val key = success.diskCacheKey
    val snapshot = key?.let { loader.diskCache?.openSnapshot(it) }
    val uri = snapshot?.let { runCatching { Uri.fromFile(it.data.toFile()) }.getOrNull() }
    return DecodedOriginal(bitmap, snapshot, uri)
}

private fun writeFixedJpeg(context: Context, bitmap: Bitmap): File? {
    return try {
        val dir = File(context.cacheDir, "reader_fixed").apply { mkdirs() }
        val file = File(dir, "p_${System.nanoTime()}.jpg")
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                file.delete()
                return null
            }
        }
        file
    } catch (e: Exception) {
        null
    }
}

private data class CachedTile(val snapshot: DiskCache.Snapshot, val uri: Uri)

private suspend fun openCachedTile(context: Context, url: String, headers: Map<String, String>): CachedTile? {
    val loader = context.imageLoader
    val builder = ImageRequest.Builder(context)
        .data(url)
        .applyDiskWarm()
    applyImageHeaders(builder, headers)
    val result = loader.execute(builder.build())
    if (result is ErrorResult) {
        if (result.throwable is CancellationException) throw result.throwable
        return null
    }
    val success = result as? SuccessResult ?: return null
    val key = success.diskCacheKey ?: return null
    val snapshot = loader.diskCache?.openSnapshot(key) ?: return null
    val file = runCatching { snapshot.data.toFile() }.getOrNull()
    if (file == null) {
        snapshot.close()
        return null
    }
    return CachedTile(snapshot, Uri.fromFile(file))
}

private fun readBounds(context: Context, uri: Uri): Pair<Int, Int>? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            else -> BitmapFactory.decodeFile(uri.path, opts)
        }
    } catch (e: Exception) {
        return null
    }
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
    return opts.outWidth to opts.outHeight
}

private fun localTileUri(url: String): Uri? {
    return when {
        url.startsWith("content:") || url.startsWith("file:") -> Uri.parse(url)
        url.startsWith("/") -> Uri.fromFile(File(url))
        else -> null
    }
}

private fun readerDecodeSize(context: Context): Size {
    val metrics = context.resources.displayMetrics
    val width = metrics.widthPixels.coerceAtLeast(1)
    val height = (metrics.heightPixels * 4).coerceAtLeast(1)
    return Size(width, height)
}

private fun ImageRequest.Builder.applyReaderDecode(context: Context) = apply {
    size(readerDecodeSize(context))
    precision(Precision.INEXACT)
    bitmapConfig(Bitmap.Config.RGB_565)
    allowHardware(false)
    memoryCachePolicy(CachePolicy.ENABLED)
    diskCachePolicy(CachePolicy.ENABLED)
}
