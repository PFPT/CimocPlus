package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.image.ComicCover
import com.haleydu.cimoc.app.nav.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(args: Screen.Detail, onBack: () -> Unit, onRead: (Screen.Reader) -> Unit) {
    val session = LocalCimocSession.current
    val vm = remember(args.id, args.source, args.cid) { session.detail() }
    val state by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var introExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(args.id, args.source, args.cid) {
        vm.load(args.id, args.source, args.cid, args.title, args.cover, args.author)
    }
    LaunchedEffect(state.downloadMessage) {
        val msg = state.downloadMessage ?: return@LaunchedEffect
        snack.showSnackbar(msg)
        vm.consumeDownloadMessage()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(snack) },
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
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val comic = state.comic
                Column {
                    Box(Modifier.fillMaxWidth().height(220.dp)) {
                        ComicCover(
                            url = comic?.cover.orEmpty(),
                            modifier = Modifier.fillMaxSize().blur(24.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                ComicCover(
                                    url = comic?.cover.orEmpty(),
                                    modifier = Modifier
                                        .size(width = 108.dp, height = 144.dp)
                                        .shadow(8.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        comic?.title.orEmpty(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        state.sourceTitle,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    comic?.author?.takeIf { it.isNotEmpty() }?.let {
                                        Text(it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text(
                                        if (comic?.finish == true) "完结" else "连载",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch { openReader(vm, vm.defaultChapterPath(), onRead) }
                            }, enabled = state.chapters.isNotEmpty()) {
                                Text(if (comic?.last.isNullOrEmpty()) "开始阅读" else "继续阅读")
                            }
                            OutlinedButton(onClick = { vm.toggleFavorite() }) {
                                Text(if (comic?.favorite == true) "已在书架" else "加入书架")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { vm.downloadUnread() }, enabled = state.chapters.isNotEmpty()) {
                            Text("批量下载")
                        }
                        comic?.intro?.takeIf { it.isNotEmpty() }?.let { intro ->
                            Text(
                                intro,
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clickable { introExpanded = !introExpanded },
                                fontSize = 13.sp,
                                maxLines = if (introExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (introExpanded) "收起" else "展开",
                                modifier = Modifier.clickable { introExpanded = !introExpanded }.padding(top = 4.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        state.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                        }
                        Text("章节 ${state.chapters.size}", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                }
            }
            items(state.displayed, key = { it.path + it.title }) { chapter ->
                Text(
                    text = chapter.title.ifEmpty { chapter.path },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
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

private suspend fun openReader(
    vm: com.haleydu.cimoc.shared.vm.DetailViewModel,
    path: String?,
    onRead: (Screen.Reader) -> Unit
) {
    val chapterPath = path?.takeIf { it.isNotEmpty() } ?: return
    val id = vm.prepareRead(chapterPath)
    if (id <= 0) return
    onRead(Screen.Reader(id, chapterPath, vm.ui.value.chapters))
}
