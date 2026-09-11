package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.TagIds
import com.haleydu.cimoc.shared.model.TagItem
import com.haleydu.cimoc.shared.vm.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpen: (Screen) -> Unit) {
    val vm = LocalCimocSession.current.library
    val state by vm.ui.collectAsState()
    val snack = remember { SnackbarHostState() }
    var pending by remember { mutableStateOf<Pair<LibraryViewModel.Tab, ComicItem>?>(null) }
    LaunchedEffect(Unit) {
        vm.resume()
    }
    LaunchedEffect(state.checkMessage) {
        val msg = state.checkMessage ?: return@LaunchedEffect
        snack.showSnackbar(msg)
        vm.consumeCheckMessage()
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        val tabs = LibraryViewModel.Tab.values()
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(state.tab).coerceAtLeast(0),
                edgePadding = 12.dp,
                modifier = Modifier.weight(1f)
            ) {
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
            when (state.tab) {
                LibraryViewModel.Tab.Favorite -> {
                    IconButton(onClick = { vm.checkFavorites() }, enabled = !state.checking) {
                        Icon(Icons.Default.Refresh, contentDescription = "检查更新")
                    }
                }
                LibraryViewModel.Tab.Download -> {
                    IconButton(onClick = { onOpen(Screen.DownloadTasks) }) {
                        Icon(Icons.Default.List, contentDescription = "下载任务")
                    }
                }
                else -> {}
            }
        }
        if (state.tab == LibraryViewModel.Tab.Favorite) {
            val chips = buildList {
                add(TagItem(TagIds.ALL, "全部"))
                add(TagItem(TagIds.CONTINUE, "连载"))
                add(TagItem(TagIds.FINISH, "完结"))
                addAll(state.tags)
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chips, key = { it.id }) { tag ->
                    FilterChip(
                        selected = state.tagId == tag.id,
                        onClick = { vm.selectTag(tag.id) },
                        label = { Text(tag.title) }
                    )
                }
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
            onLongClick = { comic ->
                when (state.tab) {
                    LibraryViewModel.Tab.Favorite, LibraryViewModel.Tab.History -> pending = state.tab to comic
                    else -> {}
                }
            },
            onNearEnd = { vm.loadMore() },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    SnackbarHost(hostState = snack, modifier = Modifier.align(Alignment.BottomCenter))
    }

    val confirm = pending
    if (confirm != null) {
        val (tab, comic) = confirm
        val removeFavorite = tab == LibraryViewModel.Tab.Favorite
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (removeFavorite) "取消收藏" else "删除历史") },
            text = {
                Text(
                    if (removeFavorite) {
                        "取消收藏「${comic.title}」？"
                    } else {
                        "删除「${comic.title}」的阅读记录？"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (removeFavorite) vm.unfavorite(comic.id) else vm.deleteHistory(comic.id)
                        pending = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("取消") }
            }
        )
    }
}
