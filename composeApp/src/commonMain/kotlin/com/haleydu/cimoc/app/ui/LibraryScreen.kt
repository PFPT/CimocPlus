package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.vm.LibraryViewModel

@Composable
fun LibraryScreen(onOpen: (Screen.Detail) -> Unit) {
    val vm = LocalCimocSession.current.library
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) {
        if (state.items.isEmpty()) vm.load(LibraryViewModel.Tab.Favorite)
    }

    Column(Modifier.fillMaxSize()) {
        val tabs = LibraryViewModel.Tab.values()
        ScrollableTabRow(selectedTabIndex = tabs.indexOf(state.tab).coerceAtLeast(0), edgePadding = 12.dp) {
            tabs.forEach { tab ->
                Tab(
                    selected = state.tab == tab,
                    onClick = { vm.load(tab) },
                    text = {
                        Text(
                            when (tab) {
                                LibraryViewModel.Tab.Favorite -> "收藏"
                                LibraryViewModel.Tab.History -> "历史"
                                LibraryViewModel.Tab.Download -> "下载"
                                LibraryViewModel.Tab.Local -> "本地"
                            }
                        )
                    }
                )
            }
        }
        ComicGrid(
            items = state.items,
            titles = state.titles,
            loading = state.loading,
            emptyText = when (state.tab) {
                LibraryViewModel.Tab.Favorite -> "暂无收藏"
                LibraryViewModel.Tab.History -> "暂无历史"
                LibraryViewModel.Tab.Download -> "暂无下载"
                LibraryViewModel.Tab.Local -> "暂无本地漫画"
            },
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
            onNearEnd = { vm.loadMore() },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
