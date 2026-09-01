package com.haleydu.cimoc.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.haleydu.cimoc.app.image.ComicCover

@Composable
actual fun ReaderPageCanvas(
    url: String,
    headers: Map<String, String>,
    modifier: Modifier,
    whiteEdge: Boolean,
    fillWidth: Boolean
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
