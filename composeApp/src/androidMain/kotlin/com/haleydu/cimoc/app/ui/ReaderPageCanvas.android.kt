package com.haleydu.cimoc.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@Composable
actual fun ReaderPageCanvas(
    url: String,
    headers: Map<String, String>,
    modifier: Modifier,
    whiteEdge: Boolean,
    fillWidth: Boolean
) {
    val context = LocalContext.current
    var bitmap by remember(url, whiteEdge) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    LaunchedEffect(url, headers, whiteEdge, retry) {
        if (url.isEmpty()) {
            bitmap = null
            failed = false
            return@LaunchedEffect
        }
        failed = false
        try {
            val decoded = withContext(Dispatchers.IO) {
                val builder = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                applyImageHeaders(builder, headers)
                val result = context.imageLoader.execute(builder.build())
                when (result) {
                    is SuccessResult -> {
                        val raw = result.image.toBitmap()
                        fixReaderBitmap(raw, url, whiteEdge)
                    }
                    is ErrorResult -> {
                        if (result.throwable is CancellationException) throw result.throwable
                        null
                    }
                    else -> null
                }
            }
            bitmap = decoded
            failed = decoded == null
        } catch (e: CancellationException) {
            throw e
        }
    }
    when {
        failed -> Box(
            modifier = modifier
                .then(if (fillWidth) Modifier.fillMaxWidth().height(220.dp) else Modifier.fillMaxSize())
                .background(Color.Black)
                .clickable { retry += 1 },
            contentAlignment = Alignment.Center
        ) {
            Text("加载失败 / 点击重试", color = Color.White, fontSize = 14.sp)
        }
        bitmap != null && fillWidth -> {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        bitmap != null -> AndroidView(
            modifier = modifier,
            factory = { ctx ->
                SubsamplingScaleImageView(ctx).apply {
                    setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    setDoubleTapZoomDuration(200)
                }
            },
            update = { view ->
                val bmp = bitmap
                if (bmp != null) {
                    view.setImage(ImageSource.bitmap(bmp))
                }
            }
        )
        else -> Box(
            modifier = modifier
                .then(if (fillWidth) Modifier.fillMaxWidth().height(220.dp) else Modifier.fillMaxSize())
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
actual fun PrefetchReaderImages(urls: List<String>, headers: Map<String, String>) {
    val context = LocalContext.current
    val jobs = remember { mutableMapOf<String, Disposable>() }
    val boundHeaders = remember { mutableStateOf(headers) }
    DisposableEffect(urls, headers) {
        if (boundHeaders.value != headers) {
            jobs.values.forEach { it.dispose() }
            jobs.clear()
            boundHeaders.value = headers
        }
        val loader = context.imageLoader
        val wanted = urls.toSet()
        jobs.keys.filter { it !in wanted }.forEach { key ->
            jobs.remove(key)?.dispose()
        }
        wanted.forEach { url ->
            if (url.isNotEmpty() && url !in jobs) {
                val builder = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                applyImageHeaders(builder, headers)
                jobs[url] = loader.enqueue(builder.build())
            }
        }
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose {
            jobs.values.forEach { it.dispose() }
            jobs.clear()
        }
    }
}

private fun applyImageHeaders(builder: ImageRequest.Builder, headers: Map<String, String>) {
    if (headers.isEmpty()) return
    builder.httpHeaders(
        NetworkHeaders.Builder().apply {
            headers.forEach { (k, v) -> set(k, v) }
        }.build()
    )
}
