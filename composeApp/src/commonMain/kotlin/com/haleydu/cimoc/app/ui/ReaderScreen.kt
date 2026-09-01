package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.image.ComicCover
import com.haleydu.cimoc.app.nav.ReaderSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.vm.ReaderViewModel

@Composable
fun ReaderScreen(args: Screen.Reader, onBack: () -> Unit) {
    val repos = LocalCimocRepos.current
    val vm = remember { ReaderViewModel(repos.comics, repos.manga) }
    DisposableEffect(vm) { onDispose { vm.clear() } }
    val state by vm.ui.collectAsState()
    val resolved = remember { mutableStateMapOf<Int, String>() }
    LaunchedEffect(args.comicId, args.path) {
        vm.load(args.comicId, args.path, ReaderSession.chapters)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.chapter?.title ?: state.comic?.title ?: "阅读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading && state.pages.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            state.error != null && state.pages.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(state.error.orEmpty()) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(state.pages, key = { it.index }) { page ->
                    val url = resolved[page.index] ?: page.displayUrl
                    if (page.lazy && resolved[page.index] == null) {
                        LaunchedEffect(page.index, page.displayUrl) {
                            vm.resolveLazy(page) { resolved[page.index] = it }
                        }
                    }
                    ComicCover(
                        url = url,
                        headers = state.headers,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}
