package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.haleydu.cimoc.shared.model.checkKindLabel
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.app.theme.LocalAppIcons
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.TagIds
import com.haleydu.cimoc.shared.model.TagItem
import com.haleydu.cimoc.shared.vm.LibraryViewModel

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
        val icons = LocalAppIcons.current
        PageHeader(
            title = "书架",
            actions = {
                when (state.tab) {
                    LibraryViewModel.Tab.Favorite -> {
                        IconButton(onClick = { vm.checkFavorites() }, enabled = !state.checking) {
                            Icon(icons.refresh, contentDescription = "检查更新")
                        }
                    }
                    LibraryViewModel.Tab.Download -> {
                        IconButton(onClick = { onOpen(Screen.DownloadTasks) }) {
                            Icon(icons.list, contentDescription = "下载任务")
                        }
                    }
                    else -> {}
                }
            }
        )
        SegmentedControl(
            labels = listOf("收藏", "历史", "下载", "本地"),
            selected = tabs.indexOf(state.tab).coerceAtLeast(0),
            onSelect = { index -> vm.load(tabs[index]) }
        )
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
        if (state.tab == LibraryViewModel.Tab.Favorite && state.failures.isNotEmpty()) {
            Text(
                state.failures.joinToString("，") { "${it.title}（${checkKindLabel(it.kind)}）" },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
        ComicGrid(
            items = state.items,
            titles = state.titles,
            loading = state.loading,
            emptyText = when (state.tab) {
                LibraryViewModel.Tab.Favorite -> "还没有收藏。搜索到漫画后可以加入书架。"
                LibraryViewModel.Tab.History -> "还没有阅读记录。打开一部漫画就会记在这里。"
                LibraryViewModel.Tab.Download -> "还没有下完的漫画。在详情页开始下载，进度在下载页。"
                LibraryViewModel.Tab.Local -> "没有本地漫画。授权目录后，已下载的内容会出现在这里。"
            },
            onClick = { comic ->
                val cid = comic.cid
                if (cid.startsWith("http://") || cid.startsWith("https://")) {
                    onOpen(Screen.WebBrowser(cid))
                } else {
                    onOpen(
                        Screen.Detail(
                            id = comic.id ?: -1L,
                            source = comic.source,
                            cid = cid,
                            title = comic.title,
                            cover = comic.cover,
                            author = comic.author
                        )
                    )
                }
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
