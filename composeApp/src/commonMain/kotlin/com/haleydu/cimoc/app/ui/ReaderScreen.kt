package com.haleydu.cimoc.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocActions
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.ReaderMode
import com.haleydu.cimoc.shared.vm.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    args: Screen.Reader,
    onBack: () -> Unit,
    onSearchSource: (title: String, source: Int, cid: String) -> Unit
) {
    val vm = LocalCimocSession.current.reader
    val actions = LocalCimocActions.current
    val state by vm.ui.collectAsState()
    val resolved = remember(args.comicId, args.path) { mutableStateMapOf<Int, String>() }
    var edgeArmed by remember(args.comicId, args.path) { mutableStateOf(false) }
    LaunchedEffect(args.comicId, args.path) {
        vm.load(args.comicId, args.path, args.chapters)
    }
    LaunchedEffect(state.chapter?.path) {
        resolved.clear()
    }
    LaunchedEffect(state.chapter?.path, state.pages.size, state.loading, state.loadingMore) {
        edgeArmed = false
        if (state.loading || state.loadingMore || state.pages.isEmpty()) return@LaunchedEffect
        delay(350)
        edgeArmed = true
    }
    LaunchedEffect(state.hideNav, state.overlay, state.keepBright) {
        actions.setImmersive(state.hideNav && !state.overlay)
        actions.setKeepScreenOn(state.keepBright)
    }
    LaunchedEffect(state.brightness) {
        val value = state.brightness
        actions.setBrightness(if (value <= 0) null else (value / 100f).coerceIn(0.01f, 1f))
    }
    DisposableEffect(args.comicId, args.path) {
        onDispose {
            actions.setImmersive(false)
            actions.setKeepScreenOn(false)
        }
    }
    val stale = state.comic != null && state.comic?.id != null && state.comic?.id != args.comicId
    val chapterKey = state.chapter?.path ?: args.path
    val reportPage = state.seekPage == null && !state.loading && !state.loadingMore
    LaunchedEffect(state.auto, state.page, state.pages.size, state.loading, stale) {
        if (!state.auto || state.pages.isEmpty() || state.loading || stale) return@LaunchedEffect
        delay(state.autoInterval * 1000L)
        vm.turnPage(true)
    }
    val start = (state.page - 1).coerceAtLeast(0)
    LaunchedEffect(chapterKey, state.page, state.pages.size, stale) {
        if (stale || state.pages.isEmpty()) return@LaunchedEffect
        var left = 2
        for (page in state.pages.drop(state.page.coerceAtLeast(0))) {
            if (left == 0) break
            if (!page.lazy) continue
            if (!resolved[page.index].isNullOrEmpty()) {
                left--
                continue
            }
            left--
            vm.resolveLazy(page) { url ->
                if (url.isNotEmpty()) resolved[page.index] = url
            }
        }
    }
    val prefetchUrls = if (stale) emptyList() else state.pages.drop(start).take(4).mapNotNull { page ->
        val url = if (page.lazy) resolved[page.index] else page.displayUrl
        url?.takeIf { it.isNotEmpty() }
    }
    PrefetchReaderImages(prefetchUrls, state.headers)

    val filter = remember(state.invert, state.gray) { readerColorFilter(state.invert, state.gray) }

    Box(Modifier.fillMaxSize().background(Color.Black).then(filter)) {
        when {
            state.error != null && state.pages.isEmpty() && !stale -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty(), color = Color.White)
                    Button(onClick = { vm.retry() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("重试")
                    }
                }
            }
            stale || (state.pages.isEmpty() && state.error == null) -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.mode == ReaderMode.PAGE -> {
                key(args.comicId, chapterKey) {
                val pagerState = rememberPagerState(
                    initialPage = (state.page - 1).coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                ) { state.pages.size.coerceAtLeast(1) }
                LaunchedEffect(pagerState, reportPage) {
                    if (!reportPage) return@LaunchedEffect
                    snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { vm.setPage(it + 1) }
                }
                LaunchedEffect(state.seekPage, state.pages.size) {
                    val seek = state.seekPage ?: return@LaunchedEffect
                    val index = (seek - 1).coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                    if (pagerState.currentPage != index) pagerState.scrollToPage(index)
                    vm.consumeSeek()
                }
                val lastIndex = (state.pages.size - 1).coerceAtLeast(0)
                val edgeScroll = rememberChapterEdgeConnection(
                    atStart = pagerState.currentPage <= 0,
                    atEnd = pagerState.currentPage >= lastIndex,
                    reverseLayout = state.reverse,
                    vertical = state.vertical,
                    enabled = edgeArmed && state.seekPage == null && !state.loadingMore,
                    onPrev = { vm.loadAdjacent(false) },
                    onNext = { vm.loadAdjacent(true) }
                )
                val pagerModifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(edgeScroll)
                    .pointerInput(state.reverse, state.vertical) {
                        detectTapGestures { offset ->
                            handleReaderTap(offset, size.width.toFloat(), size.height.toFloat(), state.vertical, vm)
                        }
                    }
                val pageTap = { offset: Offset, width: Float, height: Float ->
                    handleReaderTap(offset, width, height, state.vertical, vm)
                }
                val pageFling = { velocityX: Float, velocityY: Float ->
                    val next = if (state.vertical) {
                        if (state.reverse) velocityY > 0f else velocityY < 0f
                    } else {
                        if (state.reverse) velocityX > 0f else velocityX < 0f
                    }
                    vm.turnPage(next)
                }
                if (state.vertical) {
                    VerticalPager(
                        state = pagerState,
                        reverseLayout = state.reverse,
                        beyondViewportPageCount = 1,
                        userScrollEnabled = false,
                        modifier = pagerModifier
                    ) { index ->
                        ReaderPagerPage(
                            state.pages.getOrNull(index),
                            resolved,
                            state.headers,
                            state.whiteEdge,
                            vm::resolveLazy,
                            vertical = true,
                            onTap = pageTap,
                            onFling = pageFling
                        )
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = state.reverse,
                        beyondViewportPageCount = 1,
                        userScrollEnabled = false,
                        modifier = pagerModifier
                    ) { index ->
                        ReaderPagerPage(
                            state.pages.getOrNull(index),
                            resolved,
                            state.headers,
                            state.whiteEdge,
                            vm::resolveLazy,
                            vertical = false,
                            onTap = pageTap,
                            onFling = pageFling
                        )
                    }
                }
                }
            }
            else -> {
                val listState = remember(args.comicId, chapterKey) {
                    androidx.compose.foundation.lazy.LazyListState(
                        firstVisibleItemIndex = (state.page - 1).coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                    )
                }
                val scope = rememberCoroutineScope()
                LaunchedEffect(listState, reportPage) {
                    if (!reportPage) return@LaunchedEffect
                    snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { vm.setPage(it + 1) }
                }
                LaunchedEffect(state.seekPage, state.pages.size) {
                    val seek = state.seekPage ?: return@LaunchedEffect
                    val index = (seek - 1).coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                    listState.scrollToItem(index)
                    vm.consumeSeek()
                }
                val edgeScroll = rememberChapterEdgeConnection(
                    atStart = !listState.canScrollBackward,
                    atEnd = !listState.canScrollForward,
                    reverseLayout = false,
                    vertical = true,
                    enabled = edgeArmed && state.seekPage == null && !state.loadingMore,
                    onPrev = { vm.loadAdjacent(false) },
                    onNext = { vm.loadAdjacent(true) }
                )
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(edgeScroll)
                        .pointerInput(state.reverse) {
                            detectTapGestures { offset ->
                                handleReaderTap(offset, size.width.toFloat(), size.height.toFloat(), true, vm)
                            }
                        }
                ) {
                    items(state.pages, key = { it.index }) { page ->
                        ReaderPagerPage(
                            page = page,
                            resolved = resolved,
                            headers = state.headers,
                            whiteEdge = state.whiteEdge,
                            resolveLazy = vm::resolveLazy,
                            fillWidth = true
                        )
                    }
                }
                val showFab = listState.firstVisibleItemIndex > 2
                AnimatedVisibility(
                    visible = showFab && !state.overlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp)
                ) {
                    FloatingActionButton(onClick = { scope.launch { listState.scrollToItem(0) } }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                }
            }
        }

        if (!state.overlay && state.pages.isNotEmpty() && !stale) {
            Text(
                "${state.page} / ${state.pages.size}",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        AnimatedVisibility(
            visible = state.overlay,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
        ) {
            ReaderTopHud(
                state = state,
                vm = vm,
                onBack = onBack,
                onNight = { actions.onNightChanged(vm.toggleNight()) },
                onSearchSource = onSearchSource
            )
        }

        AnimatedVisibility(
            visible = state.overlay && state.pages.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            ReaderBottomHud(state = state, vm = vm)
        }

        if (state.chapterSheet) {
            ReaderChapterSheet(
                chapters = state.chapters,
                currentPath = state.chapter?.path,
                onDismiss = { vm.toggleChapterSheet() },
                onJump = { vm.jumpChapter(it) }
            )
        }
        state.error?.takeIf { state.pages.isNotEmpty() }?.let {
            Text(it, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), color = Color.White)
        }
    }
}

@Composable
private fun ReaderChapterSheet(
    chapters: List<ChapterItem>,
    currentPath: String?,
    onDismiss: () -> Unit,
    onJump: (String) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentPath, chapters) {
        val index = chapters.indexOfFirst { it.path == currentPath }
        if (index >= 0) listState.scrollToItem(index)
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onDismiss)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp)
        ) {
            items(chapters, key = { it.path }) { chapter ->
                val selected = chapter.path == currentPath
                Text(
                    text = chapter.title.ifEmpty { chapter.path },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onJump(chapter.path) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun rememberChapterEdgeConnection(
    atStart: Boolean,
    atEnd: Boolean,
    reverseLayout: Boolean,
    vertical: Boolean,
    enabled: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
): NestedScrollConnection {
    return remember(atStart, atEnd, reverseLayout, vertical, enabled) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val axis = if (vertical) available.y else available.x
                dispatch(axis, 24f, source)
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val axis = if (vertical) available.y else available.x
                return if (dispatch(axis, 400f, NestedScrollSource.Fling)) available else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val axis = if (vertical) available.y else available.x
                return if (dispatch(axis, 280f, NestedScrollSource.Fling)) available else Velocity.Zero
            }

            private fun dispatch(axis: Float, threshold: Float, source: NestedScrollSource): Boolean {
                if (!enabled || !isUserChapterScroll(source)) return false
                val towardNext = if (reverseLayout) axis > threshold else axis < -threshold
                val towardPrev = if (reverseLayout) axis < -threshold else axis > threshold
                return when {
                    atEnd && towardNext -> {
                        onNext()
                        true
                    }
                    atStart && towardPrev -> {
                        onPrev()
                        true
                    }
                    else -> false
                }
            }
        }
    }
}

private fun isUserChapterScroll(source: NestedScrollSource): Boolean {
    return source == NestedScrollSource.Drag ||
        source == NestedScrollSource.Fling ||
        source == NestedScrollSource.Wheel
}

private fun handleReaderTap(
    offset: Offset,
    width: Float,
    height: Float,
    vertical: Boolean,
    vm: ReaderViewModel
) {
    if (vertical) {
        when {
            offset.y < height * 0.3f -> vm.tapEdge(true)
            offset.y > height * 0.7f -> vm.tapEdge(false)
            else -> vm.toggleOverlay()
        }
    } else {
        when {
            offset.x < width * 0.3f -> vm.tapEdge(true)
            offset.x > width * 0.7f -> vm.tapEdge(false)
            else -> vm.toggleOverlay()
        }
    }
}

@Composable
private fun ReaderTopHud(
    state: ReaderViewModel.UiState,
    vm: ReaderViewModel,
    onBack: () -> Unit,
    onNight: () -> Unit,
    onSearchSource: (title: String, source: Int, cid: String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                text = state.chapter?.title ?: state.comic?.title ?: "阅读",
                modifier = Modifier.weight(1f),
                color = Color.White,
                maxLines = 1,
                fontSize = 16.sp
            )
            IconButton(onClick = { vm.toggleAuto() }) {
                Icon(if (state.auto) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "自动翻页", tint = Color.White)
            }
            IconButton(onClick = { vm.toggleReaderMode() }) {
                Icon(Icons.Default.ViewDay, contentDescription = "条漫/分页", tint = Color.White)
            }
            IconButton(onClick = { vm.toggleVertical() }) {
                Icon(Icons.Default.ScreenRotation, contentDescription = "纵向分页", tint = Color.White)
            }
            IconButton(onClick = onNight) {
                Icon(Icons.Default.DarkMode, contentDescription = "夜间", tint = Color.White)
            }
            val title = state.comic?.title.orEmpty()
            val comic = state.comic
            if (comic != null && !comic.local && title.isNotEmpty()) {
                IconButton(onClick = { onSearchSource(title, comic.source, comic.cid) }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "换源", tint = Color.White)
                }
            }
            IconButton(onClick = { vm.toggleChapterSheet() }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "目录", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ReaderBottomHud(state: ReaderViewModel.UiState, vm: ReaderViewModel) {
    var sliderPage by remember { mutableStateOf<Int?>(null) }
    val max = state.pages.size.coerceAtLeast(1)
    val display = (sliderPage ?: state.page).coerceIn(1, max)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.loadAdjacent(false) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一章", tint = Color.White)
            }
            Slider(
                value = display.toFloat(),
                onValueChange = { sliderPage = it.toInt() },
                onValueChangeFinished = {
                    sliderPage?.let { vm.seekTo(it) }
                    sliderPage = null
                },
                valueRange = 1f..max.toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                "$display/$max",
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 13.sp,
                color = Color.White
            )
            IconButton(onClick = { vm.loadAdjacent(true) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一章", tint = Color.White)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.brightness <= 0) "亮度 系统" else "亮度 ${state.brightness}",
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Slider(
                value = state.brightness.toFloat(),
                onValueChange = { vm.setBrightness(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun ReaderPagerPage(
    page: PageImage?,
    resolved: MutableMap<Int, String>,
    headers: Map<String, String>,
    whiteEdge: Boolean,
    resolveLazy: (PageImage, (String) -> Unit) -> Unit,
    fillWidth: Boolean = false,
    vertical: Boolean = false,
    onTap: ((Offset, Float, Float) -> Unit)? = null,
    onFling: ((Float, Float) -> Unit)? = null
) {
    if (page == null) return
    val resolvedUrl = resolved[page.index]
    if (page.lazy && resolvedUrl == null) {
        LaunchedEffect(page.index, page.displayUrl) {
            resolveLazy(page) { url -> if (url.isNotEmpty()) resolved[page.index] = url }
        }
        ReaderPagePlaceholder(fillWidth)
        return
    }
    val url = resolvedUrl ?: page.displayUrl
    if (url.isEmpty()) {
        ReaderPagePlaceholder(fillWidth)
        return
    }
    ReaderPageCanvas(
        url = url,
        headers = headers,
        whiteEdge = whiteEdge,
        fillWidth = fillWidth,
        vertical = vertical,
        onTap = onTap,
        onFling = onFling,
        modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
    )
}

@Composable
private fun ReaderPagePlaceholder(fillWidth: Boolean) {
    Box(
        modifier = if (fillWidth) {
            Modifier.fillMaxWidth().height(220.dp)
        } else {
            Modifier.fillMaxSize()
        },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

private fun readerColorFilter(invert: Boolean, gray: Boolean): Modifier {
    if (!invert && !gray) return Modifier
    val matrix = ColorMatrix()
    if (gray) matrix.setToSaturation(0f)
    if (invert) {
        val invertMatrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.timesAssign(invertMatrix)
    }
    val filter = ColorFilter.colorMatrix(matrix)
    return Modifier.drawWithContent {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply { colorFilter = filter }
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
    }
}
