package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpen: (Screen.Detail) -> Unit) {
    val vm = LocalCimocSession.current.search
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { if (state.enabledSources.isEmpty()) vm.loadSources() }
    var input by remember { mutableStateOf(state.keyword) }

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
        state.error?.let {
            Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error)
        }
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
            },
            onNearEnd = { vm.searchMore() }
        )
    }
}
