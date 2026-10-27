package com.haleydu.cimoc.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import com.haleydu.cimoc.app.image.ComicCover

@Suppress("UNUSED_PARAMETER")
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
    ComicCover(
        url = url,
        headers = headers,
        modifier = modifier,
        contentScale = if (fillWidth) ContentScale.FillWidth else ContentScale.Fit
    )
}

@Composable
actual fun PrefetchReaderImages(urls: List<String>, headers: Map<String, String>) {
}
