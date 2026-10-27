package com.haleydu.cimoc.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

@Composable
expect fun ReaderPageCanvas(
    url: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    whiteEdge: Boolean = false,
    fillWidth: Boolean = false,
    vertical: Boolean = false,
    onTap: ((Offset, Float, Float) -> Unit)? = null,
    onFling: ((Float, Float) -> Unit)? = null
)

@Composable
expect fun PrefetchReaderImages(urls: List<String>, headers: Map<String, String>)
