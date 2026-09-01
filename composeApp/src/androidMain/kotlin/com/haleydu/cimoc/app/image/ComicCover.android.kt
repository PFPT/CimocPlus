package com.haleydu.cimoc.app.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
actual fun ComicCover(
    url: String,
    modifier: Modifier,
    headers: Map<String, String>,
    contentScale: ContentScale
) {
    if (url.isEmpty()) {
        Box(modifier.background(MaterialTheme.colors.surface))
        return
    }
    val context = LocalContext.current
    val builder = ImageRequest.Builder(context).data(url)
    headers.forEach { (k, v) -> builder.setHeader(k, v) }
    AsyncImage(
        model = builder.build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}
