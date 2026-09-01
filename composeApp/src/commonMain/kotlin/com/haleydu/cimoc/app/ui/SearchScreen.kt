package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.shared.vm.SearchViewModel

@Composable
fun SearchScreen(onOpen: (Screen.Detail) -> Unit) {
    val repos = LocalCimocRepos.current
    val vm = remember { SearchViewModel(repos.sources, repos.manga) }
    DisposableEffect(vm) { onDispose { vm.clear() } }
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.loadSources() }
    var input by remember { mutableStateOf("") }

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
        state.error?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp), color = androidx.compose.material.MaterialTheme.colors.error) }
        ComicGrid(
            items = state.items,
            titles = state.enabledSources.associate { it.type to it.title },
            loading = state.loading,
            emptyText = if (state.empty) "没有搜索结果" else "输入关键字搜索",
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
            }
        )
    }
}
