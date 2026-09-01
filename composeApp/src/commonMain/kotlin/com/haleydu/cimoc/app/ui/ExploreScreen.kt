package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(type: Int, onBack: () -> Unit, onOpen: (Screen.Detail) -> Unit) {
    val vm = LocalCimocSession.current.explore
    val state by vm.ui.collectAsState()
    LaunchedEffect(type) { vm.open(type) }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifEmpty { "浏览" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading && state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ComicGrid(
                    items = state.items,
                    titles = mapOf(state.source to state.title),
                    loading = state.loading,
                    emptyText = state.error ?: "暂无内容",
                    onClick = { comic ->
                        onOpen(
                            Screen.Detail(
                                id = comic.id ?: -1L,
                                source = comic.source,
                                cid = comic.cid,
                                title = comic.title,
                                cover = comic.cover,
                                author = comic.author
                            )
                        )
                    },
                    onNearEnd = { vm.loadMore() }
                )
            }
        }
    }
}
