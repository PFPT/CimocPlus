package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.nav.Screen
import kotlinx.coroutines.launch

private data class WebChapterSnap(
    val title: String,
    val pageUrl: String,
    val imageUrls: List<String>,
    val nextUrl: String?
)

@Composable
fun WebReaderScreen(args: Screen.WebReader, onBack: () -> Unit) {
    val session = rememberWebExtractSession()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val history = remember { ArrayDeque<WebChapterSnap>() }
    var title by remember { mutableStateOf(args.title) }
    var pageUrl by remember { mutableStateOf(args.pageUrl) }
    var pages by remember { mutableStateOf(args.imageUrls) }
    var nextUrl by remember { mutableStateOf(args.nextUrl) }
    var loading by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    val headers = remember(pageUrl) {
        mapOf(
            "Referer" to pageUrl,
            "User-Agent" to BrowserUserAgent
        )
    }
    PrefetchReaderImages(pages.take(4), headers)

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.85f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp
            )
            TextButton(
                onClick = {
                    val previous = history.removeLastOrNull() ?: return@TextButton
                    title = previous.title
                    pageUrl = previous.pageUrl
                    pages = previous.imageUrls
                    nextUrl = previous.nextUrl
                    hint = null
                    scope.launch { listState.scrollToItem(0) }
                },
                enabled = history.isNotEmpty() && !loading
            ) {
                Text("上一话", color = Color.White)
            }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(pages, key = { index, url -> "$index$url" }) { _, url ->
                ReaderPageCanvas(
                    url = url,
                    headers = headers,
                    fillWidth = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item(key = "next") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        loading -> CircularProgressIndicator(color = Color.White)
                        nextUrl != null -> Button(
                            onClick = {
                                val target = nextUrl ?: return@Button
                                scope.launch {
                                    loading = true
                                    hint = null
                                    val extracted = runCatching { session.extract(target) }.getOrNull()
                                    loading = false
                                    if (extracted == null || extracted.imageUrls.isEmpty()) {
                                        hint = "下一章没有找到漫画图片"
                                    } else {
                                        history.addLast(WebChapterSnap(title, pageUrl, pages, nextUrl))
                                        title = extracted.title.ifEmpty { title }
                                        pageUrl = extracted.pageUrl.ifEmpty { target }
                                        pages = extracted.imageUrls
                                        nextUrl = extracted.nextUrl
                                        listState.scrollToItem(0)
                                    }
                                }
                            }
                        ) {
                            Text("下一章")
                        }
                        else -> Text("已是最后一章", color = Color.White.copy(alpha = 0.7f))
                    }
                    hint?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(top = 12.dp),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
