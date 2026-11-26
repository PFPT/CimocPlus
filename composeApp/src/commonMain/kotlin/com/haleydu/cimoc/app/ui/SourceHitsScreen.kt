package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen

@Composable
fun SourceHitsScreen(
    title: String,
    excludeSource: Int,
    excludeCid: String,
    onBack: () -> Unit,
    onOpen: (Screen.Detail) -> Unit
) {
    val session = LocalCimocSession.current
    val vm = remember(title, excludeSource, excludeCid) { session.sourceHits() }
    val state by vm.ui.collectAsState()
    LaunchedEffect(title, excludeSource, excludeCid) { vm.search(title, excludeSource, excludeCid) }
    DisposableEffect(Unit) {
        onDispose { vm.clear() }
    }

    Scaffold(
        topBar = { PageHeader("换源 · $title", onBack = onBack) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.error != null && state.items.isEmpty() && !state.loading) {
                Text(
                    state.error.orEmpty(),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                ComicGrid(
                    items = state.items,
                    titles = state.titles,
                    loading = state.loading,
                    emptyText = if (state.empty) "没有找到同名漫画" else "正在搜索…",
                    onClick = { comic ->
                        onOpen(
                            Screen.Detail(
                                id = comic.id ?: -1L,
                                source = comic.source,
                                cid = comic.cid,
                                title = comic.title,
                                cover = comic.cover,
                                author = comic.author
                            )
                        )
                    }
                )
            }
            if (state.loading && state.items.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
