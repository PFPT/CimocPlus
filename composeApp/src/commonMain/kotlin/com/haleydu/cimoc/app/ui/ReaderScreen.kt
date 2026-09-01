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
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocActions
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.ReaderMode
import com.haleydu.cimoc.shared.vm.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(args: Screen.Reader, onBack: () -> Unit) {
    val vm = LocalCimocSession.current.reader
    val actions = LocalCimocActions.current
    val state by vm.ui.collectAsState()
    val resolved = remember { mutableStateMapOf<Int, String>() }
    LaunchedEffect(args.comicId, args.path) {
        vm.load(args.comicId, args.path, args.chapters)
    }
    LaunchedEffect(state.hideNav, state.overlay, state.keepBright) {
        actions.setImmersive(state.hideNav && !state.overlay)
        actions.setKeepScreenOn(state.keepBright)
    }
    DisposableEffect(Unit) {
        onDispose {
            actions.setImmersive(false)
            actions.setKeepScreenOn(false)
        }
    }
    LaunchedEffect(state.auto, state.page, state.pages.size, state.loading) {
        if (!state.auto || state.pages.isEmpty() || state.loading) return@LaunchedEffect
        delay(state.autoInterval * 1000L)
        vm.turnPage(true)
    }
    val start = (state.page - 1).coerceAtLeast(0)
    val prefetchUrls = state.pages.drop(start).take(4).mapNotNull { page ->
        val url = if (page.lazy) resolved[page.index] else page.displayUrl
        url?.takeIf { it.isNotEmpty() }
    }
    PrefetchReaderImages(prefetchUrls, state.headers)

    val filter = remember(state.invert, state.gray) { readerColorFilter(state.invert, state.gray) }

    Box(Modifier.fillMaxSize().background(Color.Black).then(filter)) {
        when {
            state.loading && state.pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null && state.pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty(), color = Color.White)
                    Button(onClick = { vm.retry() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("重试")
                    }
                }
            }
            state.mode == ReaderMode.PAGE -> {
                val pagerState = rememberPagerState(
                    initialPage = (state.page - 1).coerceAtLeast(0)
                ) { state.pages.size.coerceAtLeast(1) }
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { vm.setPage(it + 1) }
                }
                LaunchedEffect(state.seekPage, state.pages.size) {
                    val seek = state.seekPage ?: return@LaunchedEffect
                    val index = (seek - 1).coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                    if (pagerState.currentPage != index) pagerState.scrollToPage(index)
                    vm.consumeSeek()
                }
                val pagerModifier = Modifier.fillMaxSize().pointerInput(state.reverse, state.vertical) {
                    detectTapGestures { offset ->
                        handleReaderTap(offset, size.width.toFloat(), size.height.toFloat(), state.vertical, vm)
                    }
                }
                if (state.vertical) {
                    VerticalPager(
                        state = pagerState,
                        reverseLayout = state.reverse,
                        beyondViewportPageCount = 1,
                        modifier = pagerModifier
                    ) { index ->
                        ReaderPagerPage(state.pages.getOrNull(index), resolved, state.headers, state.whiteEdge, vm::resolveLazy)
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = state.reverse,
                        beyondViewportPageCount = 1,
                        modifier = pagerModifier
                    ) { index ->
                        ReaderPagerPage(state.pages.getOrNull(index), resolved, state.headers, state.whiteEdge, vm::resolveLazy)
                    }
                }
            }
            else -> {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                var scale by remember { mutableStateOf(1f) }
                val transform = rememberTransformableState { zoom, _, _ ->
                    scale = (scale * zoom).coerceIn(1f, 3f)
                }
                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { vm.setPage(it + 1) }
                }
                LaunchedEffect(state.seekPage, state.pages.size) {
                    val seek = state.seekPage ?: return@LaunchedEffect
                    val index = (seek - 1).coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                    listState.scrollToItem(index)
                    vm.consumeSeek()
                }
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .transformable(transform)
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

        if (!state.overlay && state.pages.isNotEmpty()) {
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
            ReaderTopHud(state = state, vm = vm, onBack = onBack, onNight = { actions.onNightChanged(vm.toggleNight()) })
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
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable { vm.toggleChapterSheet() }
            )
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.62f)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp)
            ) {
                items(state.chapters, key = { it.path }) { chapter ->
                    val selected = chapter.path == state.chapter?.path
                    Text(
                        text = chapter.title.ifEmpty { chapter.path },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.jumpChapter(chapter.path) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
        }
        state.error?.takeIf { state.pages.isNotEmpty() }?.let {
            Text(it, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), color = Color.White)
        }
    }
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
    onNight: () -> Unit
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
}

@Composable
private fun ReaderPagerPage(
    page: PageImage?,
    resolved: MutableMap<Int, String>,
    headers: Map<String, String>,
    whiteEdge: Boolean,
    resolveLazy: (PageImage, (String) -> Unit) -> Unit,
    fillWidth: Boolean = false
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
