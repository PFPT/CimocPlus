package com.haleydu.cimoc.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ReaderPageCanvas(
    url: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    whiteEdge: Boolean = false,
    fillWidth: Boolean = false
)

@Composable
expect fun PrefetchReaderImages(urls: List<String>, headers: Map<String, String>)
