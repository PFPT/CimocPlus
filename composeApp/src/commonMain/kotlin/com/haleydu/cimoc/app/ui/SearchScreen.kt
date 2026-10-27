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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.vm.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpen: (Screen.Detail) -> Unit, onRank: () -> Unit) {
    val vm = LocalCimocSession.current.search
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { if (state.enabledSources.isEmpty()) vm.loadSources() }
    var input by remember { mutableStateOf(state.keyword) }
    LaunchedEffect(state.pendingOpen) {
        val comic = state.pendingOpen ?: return@LaunchedEffect
        vm.consumeOpen()
        onOpen(comic.toDetail())
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("搜索漫画") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { vm.search(input) }) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(input) })
        )
        if (state.enabledSources.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.enabledSources, key = { it.type }) { source ->
                    FilterChip(
                        selected = source.type in state.selectedTypes,
                        onClick = { vm.toggleSource(source.type) },
                        label = { Text(source.title) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("精确")
            Switch(checked = state.strict, onCheckedChange = { vm.toggleStrict() })
            TextButton(onClick = { vm.selectAll() }) { Text("全选") }
            TextButton(onClick = { vm.clearSources() }) { Text("清空") }
            TextButton(onClick = onRank) { Text("热度榜") }
        }
        if (input.isBlank() && state.history.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
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
        state.error?.let {
            Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error)
        }
        if (state.failedSources > 0) {
            Text(
                "${state.failedSources} 个图源失败",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SearchResults(
            vm = vm,
            loading = state.loading,
            empty = state.empty,
            titles = state.enabledSources.associate { it.type to it.title },
            onOpen = onOpen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SearchResults(
    vm: SearchViewModel,
    loading: Boolean,
    empty: Boolean,
    titles: Map<Int, String>,
    onOpen: (Screen.Detail) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by vm.items.collectAsState()
    ComicGrid(
        items = items,
        titles = titles,
        loading = loading,
        emptyText = if (empty) "没有搜索结果" else "输入关键字搜索",
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
