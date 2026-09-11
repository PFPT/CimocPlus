package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.image.ComicCover
import com.haleydu.cimoc.app.nav.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    args: Screen.Detail,
    onBack: () -> Unit,
    onRead: (Screen.Reader) -> Unit,
    onSearchSource: (title: String, source: Int, cid: String) -> Unit,
    onOpenTasks: () -> Unit
) {
    val session = LocalCimocSession.current
    val vm = remember(args.id, args.source, args.cid) { session.detail() }
    val state by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    var introExpanded by remember { mutableStateOf(false) }
    var scrollChapters by remember { mutableStateOf(false) }
    LaunchedEffect(args.id, args.source, args.cid) {
        vm.load(args.id, args.source, args.cid, args.title, args.cover, args.author)
    }
    LaunchedEffect(state.reversed, scrollChapters) {
        if (!scrollChapters) return@LaunchedEffect
        scrollChapters = false
        gridState.scrollToItem(1)
    }
    LaunchedEffect(state.downloadMessage) {
        val msg = state.downloadMessage ?: return@LaunchedEffect
        val action = if (msg.startsWith("已加入")) "查看任务" else null
        val result = snack.showSnackbar(message = msg, actionLabel = action)
        vm.consumeDownloadMessage()
        if (result == SnackbarResult.ActionPerformed) onOpenTasks()
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
                    val comic = state.comic
                    val title = comic?.title.orEmpty()
                    if (comic != null && !comic.local && title.isNotEmpty()) {
                        IconButton(onClick = { onSearchSource(title, comic.source, comic.cid) }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "换源")
                        }
                    }
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            if (state.comic?.favorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (state.selecting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { vm.selectAllUndownloaded() }) { Text("全选未下载") }
                    Button(
                        onClick = { vm.downloadSelected() },
                        enabled = state.selectedPaths.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("下载选中 ${state.selectedPaths.size}")
                    }
                    TextButton(onClick = { vm.cancelSelect() }) { Text("取消") }
                }
            }
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
            state = gridState,
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item(key = "detail_header", span = { GridItemSpan(maxLineSpan) }) {
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
                            }, enabled = state.chapters.isNotEmpty() && !state.selecting) {
                                Text(if (comic?.last.isNullOrEmpty()) "开始阅读" else "继续阅读")
                            }
                            OutlinedButton(onClick = { vm.toggleFavorite() }) {
                                Text(if (comic?.favorite == true) "已在书架" else "加入书架")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (comic?.local != true) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { vm.downloadUnread() },
                                    enabled = state.chapters.isNotEmpty() && !state.selecting
                                ) {
                                    Text("下载未读")
                                }
                                OutlinedButton(
                                    onClick = { vm.enterSelect() },
                                    enabled = state.chapters.isNotEmpty() && !state.selecting
                                ) {
                                    Text("选择下载")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = { vm.openTagDialog() },
                            enabled = (comic?.id ?: 0L) > 0
                        ) {
                            Text("标签")
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
                    }
                }
            }
            item(key = "chapter_bar", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("章节 ${state.chapters.size}", modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        vm.toggleReverse()
                        scrollChapters = true
                    }) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (state.reversed) "倒序" else "正序",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            items(state.displayed, key = { it.path + it.title }) { chapter ->
                val selected = chapter.path in state.selectedPaths
                val current = !state.selecting && chapter.path == state.comic?.last
                Text(
                    text = buildString {
                        append(chapter.title.ifEmpty { chapter.path })
                        if (chapter.complete) append(" · 已下载")
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable(enabled = chapter.path.isNotEmpty() && !(state.selecting && chapter.complete)) {
                            if (state.selecting) {
                                vm.toggleChapter(chapter.path)
                            } else {
                                scope.launch { openReader(vm, chapter.path, onRead) }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    color = if (chapter.complete) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }

    if (state.tagDialog) {
        TagPickDialog(
            tags = state.tags,
            selected = state.comicTagIds,
            onToggle = { vm.toggleTag(it) },
            onCreate = { vm.createTag(it) },
            onDismiss = { vm.closeTagDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPickDialog(
    tags: List<com.haleydu.cimoc.shared.model.TagItem>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标签") },
        text = {
            Column {
                if (tags.isEmpty()) {
                    Text("还没有标签，先新建一个")
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(tags, key = { it.id }) { tag ->
                            FilterChip(
                                selected = tag.id in selected,
                                onClick = { onToggle(tag.id) },
                                label = { Text(tag.title) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("新建标签") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (input.isNotBlank()) {
                            onCreate(input)
                            input = ""
                        }
                    })
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onCreate(input)
                        input = ""
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (input.isNotBlank()) "创建" else "完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
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
