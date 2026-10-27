package com.haleydu.cimoc.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@Composable
actual fun ReaderPageCanvas(
    url: String,
    headers: Map<String, String>,
    modifier: Modifier,
    whiteEdge: Boolean,
    fillWidth: Boolean,
    vertical: Boolean,
    onTap: ((Offset, Float, Float) -> Unit)?,
    onFling: ((Float, Float) -> Unit)?
) {
    val context = LocalContext.current
    val tileSlot = remember(url) { TileSlot() }
    val ownedSlot = remember(url) { OwnedFile() }
    val tileError = remember { mutableStateOf<() -> Unit>({}) }
    var bitmap by remember(url, whiteEdge) { mutableStateOf<Bitmap?>(null) }
    var displayUri by remember(url, whiteEdge) { mutableStateOf<Uri?>(null) }
    var ratio by remember(url, whiteEdge) { mutableStateOf<Float?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var forceBitmap by remember(url, whiteEdge) { mutableStateOf(false) }
    var regionSession by remember(url, whiteEdge) { mutableStateOf<RegionSession?>(null) }
    tileError.value = {
        forceBitmap = true
        displayUri = null
        regionSession = null
    }
    val frame = when {
        fillWidth && ratio != null -> Modifier.fillMaxWidth().aspectRatio(ratio!!)
        fillWidth -> Modifier.fillMaxWidth().height(220.dp)
        else -> Modifier.fillMaxSize()
    }
    DisposableEffect(url) {
        val tiles = tileSlot
        val owned = ownedSlot
        onDispose {
            tiles.close()
            owned.close()
        }
    }
    DisposableEffect(bitmap) {
        val current = bitmap
        onDispose { current?.takeIf { !it.isRecycled }?.recycle() }
    }
    DisposableEffect(regionSession) {
        val current = regionSession
        onDispose { current?.close() }
    }
    LaunchedEffect(url, headers, whiteEdge, fillWidth, retry, forceBitmap) {
        if (url.isEmpty()) {
            bitmap = null
            displayUri = null
            ratio = null
            failed = false
            regionSession = null
            return@LaunchedEffect
        }
        failed = false
        bitmap = null
        displayUri = null
        ratio = null
        regionSession = null
        try {
            if (!forceBitmap) {
                val prepared = withContext(Dispatchers.IO) {
                    prepareDisplay(context, url, headers, whiteEdge)
                }
                if (!isActive) {
                    prepared?.discard()
                    return@LaunchedEffect
                }
                if (prepared?.uri != null) {
                    val region = if (fillWidth) {
                        withContext(Dispatchers.IO) { RegionSession.open(context, prepared.uri) }
                    } else {
                        null
                    }
                    if (!isActive) {
                        region?.close()
                        prepared.discard()
                        return@LaunchedEffect
                    }
                    if (fillWidth && region == null) {
                        val scaled = withContext(Dispatchers.IO) { decodeScaled(context, prepared.uri) }
                        if (!isActive) {
                            scaled?.recycle()
                            prepared.discard()
                            return@LaunchedEffect
                        }
                        prepared.discard()
                        if (scaled != null && scaled.width > 0 && scaled.height > 0) {
                            ratio = scaled.width.toFloat() / scaled.height.toFloat()
                        }
                        bitmap = scaled
                        failed = scaled == null
                        return@LaunchedEffect
                    }
                    tileSlot.replace(prepared.snapshot)
                    ownedSlot.replace(prepared.ownedFile)
                    val resolvedRatio = prepared.ratio ?: region?.ratio()
                    regionSession = region
                    displayUri = prepared.uri
                    ratio = resolvedRatio
                    return@LaunchedEffect
                }
                prepared?.discard()
            }
            val decoded = withContext(Dispatchers.IO) {
                decodeReaderBitmap(context, url, headers, whiteEdge)
            }
            if (!isActive) {
                decoded?.recycle()
                return@LaunchedEffect
            }
            if (decoded != null && decoded.width > 0 && decoded.height > 0) {
                ratio = decoded.width.toFloat() / decoded.height.toFloat()
            }
            bitmap = decoded
            failed = decoded == null
        } catch (e: CancellationException) {
            throw e
        }
    }
    when {
        failed -> Box(
            modifier = modifier.then(frame).background(Color.Black).clickable { retry += 1 },
            contentAlignment = Alignment.Center
        ) {
            Text("加载失败 / 点击重试", color = Color.White, fontSize = 14.sp)
        }
        fillWidth && regionSession != null && displayUri != null -> VisibleRegionImage(
            session = regionSession!!,
            modifier = modifier.then(frame)
        )
        !fillWidth && (displayUri != null || bitmap != null) -> AndroidView(
            modifier = modifier,
            factory = { ctx ->
                ReaderPageView(ctx).apply {
                    setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    setDoubleTapZoomDuration(200)
                    setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        override fun onImageLoadError(e: Exception?) {
                            post { tileError.value.invoke() }
                        }
                    })
                }
            },
            update = { view ->
                view.vertical = vertical
                view.onTap = onTap
                view.onFling = onFling
                val uri = displayUri
                val bmp = bitmap
                when {
                    uri != null && view.tag != uri -> {
                        view.tag = uri
                        view.setImage(ImageSource.uri(uri))
                    }
                    uri == null && bmp != null && !bmp.isRecycled && view.tag != bmp -> {
                        view.tag = bmp
                        view.setImage(ImageSource.bitmap(bmp))
                    }
                }
            }
        )
        bitmap != null && !bitmap!!.isRecycled -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.then(frame),
            contentScale = if (fillWidth) ContentScale.FillWidth else ContentScale.Fit
        )
        else -> Box(
            modifier = modifier.then(frame).background(Color.Black),
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
                    .applyDiskWarm()
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
