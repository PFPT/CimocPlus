package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.vm.LibraryViewModel

@Composable
fun LibraryScreen(onOpen: (Screen.Detail) -> Unit) {
    val repos = LocalCimocRepos.current
    val vm = remember { LibraryViewModel(repos.comics, repos.sources) }
    DisposableEffect(vm) { onDispose { vm.clear() } }
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.load(LibraryViewModel.Tab.Favorite) }

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = if (state.tab == LibraryViewModel.Tab.Favorite) 0 else 1) {
            Tab(
                selected = state.tab == LibraryViewModel.Tab.Favorite,
                onClick = { vm.load(LibraryViewModel.Tab.Favorite) },
                text = { Text("收藏") }
            )
            Tab(
                selected = state.tab == LibraryViewModel.Tab.History,
                onClick = { vm.load(LibraryViewModel.Tab.History) },
                text = { Text("历史") }
            )
        }
        ComicGrid(
            items = state.items,
            titles = state.titles,
            loading = state.loading,
            emptyText = if (state.tab == LibraryViewModel.Tab.Favorite) "暂无收藏" else "暂无历史",
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
            },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
