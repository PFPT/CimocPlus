package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocActions
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.nav.Screen
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(
    onOpenDetail: (Screen.Detail) -> Unit,
    onOpenReader: (Screen.WebReader) -> Unit
) {
    val actions = LocalCimocActions.current
    val repos = LocalCimocRepos.current
    val handle = rememberBrowserHandle()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var browsing by remember { mutableStateOf(false) }
    var requestUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var navEpoch by remember { mutableIntStateOf(0) }

    fun openInput() {
        val target = browserTarget(input)
        if (target.isEmpty()) return
        hint = null
        requestUrl = target
        browsing = true
        loading = true
    }

    fun goHome() {
        browsing = false
        requestUrl = ""
        loading = false
        hint = null
        input = ""
    }

    LaunchedEffect(browsing, navEpoch) {
        actions.bindBack {
            if (!browsing) return@bindBack false
            if (handle.back()) {
                true
            } else {
                goHome()
                true
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (!browsing) {
            Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("浏览", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        placeholder = { Text("搜索漫画或输入网址") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { openInput() }),
                        trailingIcon = {
                            IconButton(onClick = { openInput() }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        }
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("搜索漫画或输入网址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { openInput() }),
                trailingIcon = {
                    IconButton(onClick = { openInput() }) {
                        Icon(Icons.Default.Search, contentDescription = "打开")
                    }
                }
            )
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            BrowserWebView(
                handle = handle,
                url = requestUrl,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onLoading = { loading = it },
                onPage = { url, title ->
                    pageTitle = title
                    if (url.isNotEmpty()) input = url
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (!handle.back()) goHome() }, enabled = navEpoch >= 0) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "后退")
                }
                IconButton(onClick = { handle.forward() }, enabled = handle.canForward()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "前进")
                }
                IconButton(onClick = { goHome() }) {
                    Icon(Icons.Default.Home, contentDescription = "主页")
                }
                IconButton(
                    onClick = {
                        if (busy) return@IconButton
                        scope.launch {
                            busy = true
                            hint = null
                            val url = handle.currentUrl().ifEmpty { requestUrl }
                            val matched = runCatching { repos.manga.matchUrl(url) }.getOrNull()
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
                                        pageUrl = extracted.pageUrl.ifEmpty { url },
                                        imageUrls = extracted.imageUrls,
                                        nextUrl = extracted.nextUrl
                                    )
                                )
                            }
                        }
                    },
                    enabled = !busy
                ) {
                    if (busy) {
                        CircularProgressIndicator()
                    } else {
                        Icon(Icons.Default.MenuBook, contentDescription = "阅读")
                    }
                }
            }
        }
    }
}
