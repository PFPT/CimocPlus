package com.haleydu.cimoc.app.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun ComicCover(
    url: String,
    modifier: Modifier,
    headers: Map<String, String>,
    contentScale: ContentScale
) {
    Box(modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f)))
}
