package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.image.ComicCover
import com.haleydu.cimoc.app.nav.ReaderSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.vm.DetailViewModel
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(args: Screen.Detail, onBack: () -> Unit, onRead: (Screen.Reader) -> Unit) {
    val repos = LocalCimocRepos.current
    val vm = remember { DetailViewModel(repos.comics, repos.manga, repos.sources) }
    DisposableEffect(vm) { onDispose { vm.clear() } }
    val state by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(args.id, args.source, args.cid) {
        vm.load(args.id, args.source, args.cid, args.title, args.cover, args.author)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.comic?.title ?: "详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            if (state.comic?.favorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { vm.toggleReverse() }) {
                        Icon(Icons.Default.SwapVert, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading && state.chapters.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val comic = state.comic
                Column(Modifier.padding(bottom = 12.dp)) {
                    Row {
                        ComicCover(
                            url = comic?.cover.orEmpty(),
                            modifier = Modifier.size(width = 110.dp, height = 150.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(comic?.title.orEmpty(), style = MaterialTheme.typography.h6, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(state.sourceTitle, fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                            comic?.author?.takeIf { it.isNotEmpty() }?.let { Text(it, fontSize = 13.sp) }
                            comic?.update?.takeIf { it.isNotEmpty() }?.let { Text("更新：$it", fontSize = 12.sp) }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch { openReader(vm, vm.defaultChapterPath(), onRead) }
                                }, enabled = state.chapters.isNotEmpty()) {
                                    Text(if (comic?.last.isNullOrEmpty()) "开始阅读" else "继续阅读")
                                }
                                OutlinedButton(onClick = { vm.toggleFavorite() }) {
                                    Text(if (comic?.favorite == true) "已收藏" else "收藏")
                                }
                            }
                        }
                    }
                    comic?.intro?.takeIf { it.isNotEmpty() }?.let {
                        Text(it, modifier = Modifier.padding(top = 12.dp), fontSize = 13.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text("章节 ${state.chapters.size}", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
            }
            items(state.displayed, key = { it.path + it.title }) { chapter ->
                Text(
                    text = chapter.title.ifEmpty { chapter.path },
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = chapter.path.isNotEmpty()) {
                            scope.launch { openReader(vm, chapter.path, onRead) }
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private suspend fun openReader(vm: DetailViewModel, path: String?, onRead: (Screen.Reader) -> Unit) {
    val chapterPath = path?.takeIf { it.isNotEmpty() } ?: return
    val id = vm.prepareRead(chapterPath)
    if (id <= 0) return
    ReaderSession.chapters = vm.ui.value.chapters
    onRead(Screen.Reader(id, chapterPath))
}
