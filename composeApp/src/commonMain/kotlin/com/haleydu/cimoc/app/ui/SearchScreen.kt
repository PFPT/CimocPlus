package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.vm.SearchViewModel

@Composable
fun SearchScreen(onOpen: (Screen.Detail) -> Unit, onRank: () -> Unit) {
    val vm = LocalCimocSession.current.search
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { if (state.enabledSources.isEmpty()) vm.loadSources() }
    var input by remember { mutableStateOf(state.keyword) }
    var filtersOpen by remember { mutableStateOf(false) }
    LaunchedEffect(state.pendingOpen) {
        val comic = state.pendingOpen ?: return@LaunchedEffect
        vm.consumeOpen()
        onOpen(comic.toDetail())
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader("搜索", actions = {
            TextButton(onClick = onRank) { Text("热度榜") }
        })
        FilledSearchField(
            value = input,
            onValueChange = { input = it },
            onSubmit = { vm.search(input) },
            placeholder = "搜索漫画",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        TextButton(
            onClick = { filtersOpen = !filtersOpen },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(if (filtersOpen) "收起图源" else "图源筛选")
        }
        if (filtersOpen) {
            SourceFilters(state, vm)
        }
        state.error?.let {
            Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.error)
        }
        if (state.failedSources > 0) {
            Text(
                "${state.failedSources} 个图源失败。可以换源，或检查网络后重试。",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.keyword.isEmpty() && state.history.isNotEmpty() && state.items.isEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.history, key = { it }) { keyword ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            input = keyword
                            vm.search(keyword)
                        },
                        label = { Text(keyword) }
                    )
                }
            }
        }
        SearchResults(
            vm = vm,
            loading = state.loading,
            empty = state.empty,
            started = state.keyword.isNotEmpty(),
            titles = state.enabledSources.associate { it.type to it.title },
            onOpen = onOpen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SourceFilters(state: SearchViewModel.UiState, vm: SearchViewModel) {
    val healthy = state.enabledSources.filter { !it.invalid }
    val broken = state.enabledSources.filter { it.invalid }
    if (healthy.isNotEmpty()) {
        SectionCard("图源") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(healthy, key = { it.type }) { source ->
                    FilterChip(
                        selected = source.type in state.selectedTypes,
                        onClick = { vm.toggleSource(source.type) },
                        label = { Text(source.title) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("精确")
                Switch(checked = state.strict, onCheckedChange = { vm.toggleStrict() })
                TextButton(onClick = { vm.selectAll() }) { Text("全选") }
                TextButton(onClick = { vm.clearSources() }) { Text("清空") }
            }
        }
    }
    if (broken.isNotEmpty()) {
        SectionCard("失效") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(broken, key = { it.type }) { source ->
                    FilterChip(
                        selected = source.type in state.selectedTypes,
                        onClick = { vm.toggleSource(source.type) },
                        label = { Text(source.title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    vm: SearchViewModel,
    loading: Boolean,
    empty: Boolean,
    started: Boolean,
    titles: Map<Int, String>,
    onOpen: (Screen.Detail) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by vm.items.collectAsState()
    ComicGrid(
        items = items,
        titles = titles,
        loading = loading,
        emptyText = when {
            empty -> "没有结果。换一个图源，或换个关键字再试。"
            started -> "正在搜索…"
            else -> "输入关键字后搜索。没有结果时可以换图源。"
        },
        onClick = { comic -> onOpen(comic.toDetail()) },
        onNearEnd = { vm.searchMore() },
        modifier = modifier
    )
}

private fun ComicItem.toDetail() = Screen.Detail(
    id = id ?: -1L,
    source = source,
    cid = cid,
    title = title,
    cover = cover,
    author = author
)
