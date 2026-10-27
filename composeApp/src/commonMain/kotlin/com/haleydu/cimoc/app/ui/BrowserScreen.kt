package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocActions
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.nav.Screen
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(onOpen: (Screen) -> Unit) {
    val settings = LocalCimocRepos.current.settings
    var input by remember { mutableStateOf("") }
    val marks = remember(BrowserMarks.revision) { BrowserMarks.load(settings) }
    val pins = marks.filter { it.pinned }
    val recent = marks.filter { !it.pinned }

    fun openInput() {
        val target = browserTarget(input)
        if (target.isEmpty()) return
        onOpen(Screen.WebBrowser(target))
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "浏览",
            modifier = Modifier.padding(top = 28.dp),
            style = MaterialTheme.typography.headlineMedium
        )
        SlimAddressBar(
            text = input,
            onTextChange = { input = it },
            onSubmit = { openInput() },
            collapsed = null,
            onExpand = {},
            placeholder = "搜索漫画或输入网址",
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 16.dp)) {
            if (pins.isNotEmpty()) {
                item(key = "pins") {
                    Text(
                        "书签",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(pins, key = { "pin_${it.url}" }) { mark ->
                    BrowserMarkRow(mark) { onOpen(Screen.WebBrowser(mark.url)) }
                }
            }
            if (recent.isNotEmpty()) {
                item(key = "recent") {
                    Text(
                        "最近",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(recent, key = { "hist_${it.url}" }) { mark ->
                    BrowserMarkRow(mark) { onOpen(Screen.WebBrowser(mark.url)) }
                }
            }
        }
    }
}

@Composable
fun WebBrowserScreen(
    url: String,
    onBack: () -> Unit,
    onOpenDetail: (Screen.Detail) -> Unit,
    onOpenReader: (Screen.WebReader) -> Unit
) {
    val actions = LocalCimocActions.current
    val repos = LocalCimocRepos.current
    val settings = repos.settings
    val handle = rememberBrowserHandle()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf(url) }
    var pageUrl by remember { mutableStateOf(url) }
    var pageTitle by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var comicReady by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var navEpoch by remember { mutableIntStateOf(0) }

    fun openInput() {
        val target = browserTarget(input)
        if (target.isEmpty()) return
        hint = null
        editing = false
        handle.load(target)
    }

    fun leaveOrBack() {
        if (!handle.back()) onBack()
    }

    fun openReading() {
        if (busy) return
        scope.launch {
            busy = true
            hint = null
            val page = handle.currentUrl().ifEmpty { url }
            val matched = runCatching { repos.manga.matchUrl(page) }.getOrNull()
            if (matched != null && matched.cid.isNotEmpty()) {
                busy = false
                onOpenDetail(
                    Screen.Detail(
                        id = -1L,
                        source = matched.source,
                        cid = matched.cid,
                        title = pageTitle.ifEmpty { null }
                    )
                )
                return@launch
            }
            val extracted = runCatching { handle.extract() }.getOrNull()
            busy = false
            if (extracted == null || extracted.imageUrls.isEmpty()) {
                hint = "这一页没有找到漫画图片"
            } else {
                onOpenReader(
                    Screen.WebReader(
                        title = extracted.title.ifEmpty { pageTitle.ifEmpty { "阅读" } },
                        pageUrl = extracted.pageUrl.ifEmpty { page },
                        imageUrls = extracted.imageUrls,
                        nextUrl = extracted.nextUrl
                    )
                )
            }
        }
    }

    val marks = remember(BrowserMarks.revision) { BrowserMarks.load(settings) }
    val pinned = marks.any { it.url == pageUrl && it.pinned }

    LaunchedEffect(loading) {
        if (loading) {
            comicReady = false
            return@LaunchedEffect
        }
        comicReady = runCatching { handle.looksLikeComic() }.getOrDefault(false)
    }
    LaunchedEffect(loading, pageUrl, pageTitle) {
        if (loading || pageUrl.isEmpty() || pageUrl.startsWith("about:")) return@LaunchedEffect
        BrowserMarks.remember(settings, pageUrl, pageTitle)
    }

    LaunchedEffect(navEpoch) {
        actions.bindBack {
            if (handle.back()) {
                true
            } else {
                onBack()
                true
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        SlimAddressBar(
            text = input,
            onTextChange = { input = it },
            onSubmit = { openInput() },
            collapsed = if (editing) null else pageLabel(pageTitle, pageUrl),
            onExpand = {
                input = pageUrl
                editing = true
            },
            placeholder = "搜索漫画或输入网址",
            pinned = pinned,
            onTogglePin = { BrowserMarks.togglePin(settings, pageUrl, pageTitle) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        }
        BrowserWebView(
            handle = handle,
            url = url,
            modifier = Modifier.fillMaxWidth().weight(1f),
            onLoading = { loading = it },
            onPage = { page, title ->
                pageTitle = title
                if (page.isNotEmpty() && !page.startsWith("about:")) {
                    pageUrl = page
                    if (!editing) input = page
                }
                navEpoch++
            }
        )
        hint?.let {
            Text(
                it,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (comicReady) {
            Text(
                "阅读模式",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(enabled = !busy) { openReading() }
                    .padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { leaveOrBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "后退")
            }
            IconButton(onClick = { handle.forward() }, enabled = handle.canForward()) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "前进")
            }
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Home, contentDescription = "主页")
            }
            IconButton(onClick = { openReading() }, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator()
                } else {
                    Icon(Icons.Default.MenuBook, contentDescription = "阅读")
                }
            }
        }
    }
}

@Composable
private fun SlimAddressBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    collapsed: String?,
    onExpand: () -> Unit,
    placeholder: String,
    pinned: Boolean? = null,
    onTogglePin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(collapsed) {
        if (collapsed == null && text.isNotEmpty()) focus.requestFocus()
    }
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (collapsed != null) {
            Text(
                collapsed,
                modifier = Modifier.weight(1f).clickable(onClick = onExpand),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
        } else {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).focusRequester(focus),
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                }
            )
        }
        if (pinned != null) {
            IconButton(onClick = onTogglePin) {
                Icon(
                    if (pinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "书签",
                    tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onSubmit) {
            Icon(Icons.Default.Search, contentDescription = "打开")
        }
    }
}

@Composable
private fun BrowserMarkRow(mark: BrowserMark, onClick: () -> Unit) {
    val label = mark.title.ifBlank { pageLabel("", mark.url) }
    Text(
        label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontSize = 15.sp
    )
}

private fun pageLabel(title: String, url: String): String {
    if (title.isNotBlank()) return title
    val host = url.substringAfter("://").substringBefore("/").substringBefore("?")
    return host.ifEmpty { url }
}
