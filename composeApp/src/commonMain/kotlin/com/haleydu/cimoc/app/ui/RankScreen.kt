package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.image.ComicCover
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.model.ComicItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankScreen(onBack: () -> Unit, onOpen: (Screen.Detail) -> Unit) {
    val session = LocalCimocSession.current
    val repos = LocalCimocRepos.current
    val vm = remember { session.rank() }
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    DisposableEffect(Unit) {
        onDispose { vm.clear() }
    }
    LaunchedEffect(state.pendingOpen) {
        val comic = state.pendingOpen ?: return@LaunchedEffect
        vm.consumeOpen()
        onOpen(comic.toDetail())
    }
    val headers = remember(state.items.firstOrNull()?.source) {
        val source = state.items.firstOrNull()?.source ?: return@remember emptyMap()
        repos.manga.imageHeaders(source)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("热度榜") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading && state.items.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("正在更新", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.stale) {
                Text(
                    "显示上次的榜单",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            state.message?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            when {
                state.loading && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(state.items, key = { _, comic -> "${comic.source}_${comic.cid}" }) { index, comic ->
                            RankRow(
                                rank = index + 1,
                                comic = comic,
                                headers = headers,
                                onClick = { vm.open(comic) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankRow(
    rank: Int,
    comic: ComicItem,
    headers: Map<String, String>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(32.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
        ComicCover(
            url = comic.cover,
            headers = headers,
            modifier = Modifier.width(56.dp).height(76.dp).clip(RoundedCornerShape(6.dp))
        )
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(comic.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
            val author = comic.author
            if (!author.isNullOrEmpty()) {
                Text(
                    author,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun ComicItem.toDetail() = Screen.Detail(
    id = id ?: -1L,
    source = source,
    cid = cid,
    title = title,
    cover = cover,
    author = author
)
