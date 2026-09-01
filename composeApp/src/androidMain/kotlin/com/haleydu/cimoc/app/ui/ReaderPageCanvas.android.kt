package com.haleydu.cimoc.app.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

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
            failed = true
            return@LaunchedEffect
        }
        failed = false
        val builder = ImageRequest.Builder(context).data(url).allowHardware(false)
        headers.forEach { (k, v) -> builder.setHeader(k, v) }
        val result = runCatching { context.imageLoader.execute(builder.build()) }.getOrNull()
        val raw = (result as? SuccessResult)?.drawable.let { (it as? BitmapDrawable)?.bitmap }
        val fixed = raw?.let { fixReaderBitmap(it, url, whiteEdge) }
        bitmap = fixed
        failed = fixed == null
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
        fillWidth -> {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
        else -> AndroidView(
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
    }
}

@Composable
actual fun PrefetchReaderImages(urls: List<String>, headers: Map<String, String>) {
    val context = LocalContext.current
    LaunchedEffect(urls, headers) {
        val loader = context.imageLoader
        urls.forEach { url ->
            val builder = ImageRequest.Builder(context).data(url)
            headers.forEach { (k, v) -> builder.setHeader(k, v) }
            loader.enqueue(builder.build())
        }
    }
}
