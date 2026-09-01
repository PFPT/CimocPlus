package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.image.ComicCover
import com.haleydu.cimoc.shared.model.ComicItem

@Composable
fun ComicGrid(
    items: List<ComicItem>,
    titles: Map<Int, String>,
    loading: Boolean,
    emptyText: String,
    onClick: (ComicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        loading && items.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        items.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f))
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            contentPadding = PaddingValues(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(items, key = { "${it.source}_${it.cid}_${it.id}" }) { comic ->
                ComicCard(
                    comic = comic,
                    sourceTitle = titles[comic.source].orEmpty(),
                    onClick = { onClick(comic) }
                )
            }
        }
    }
}

@Composable
fun ComicCard(comic: ComicItem, sourceTitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.padding(6.dp).clickable(onClick = onClick),
        elevation = 2.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column {
            ComicCover(
                url = comic.cover,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            )
            Text(
                text = comic.title,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            if (sourceTitle.isNotEmpty()) {
                Text(
                    text = sourceTitle,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}
