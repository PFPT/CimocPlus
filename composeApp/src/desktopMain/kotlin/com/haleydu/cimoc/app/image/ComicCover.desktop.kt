package com.haleydu.cimoc.app.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
actual fun ComicCover(
    url: String,
    modifier: Modifier,
    headers: Map<String, String>,
    contentScale: ContentScale
) {
    if (url.isEmpty()) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }
    val context = LocalPlatformContext.current
    var retry by remember(url) { mutableStateOf(0) }
    val request = remember(url, headers, retry) {
        val builder = ImageRequest.Builder(context).data(url).crossfade(true)
        if (headers.isNotEmpty()) {
            builder.httpHeaders(
                NetworkHeaders.Builder().apply {
                    headers.forEach { (k, v) -> set(k, v) }
                }.build()
            )
        }
        builder.build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        loading = { CoverShimmer(Modifier.fillMaxSize()) },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { retry += 1 },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
