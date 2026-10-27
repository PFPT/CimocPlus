package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen

@Composable
fun SourceScreen(onOpen: (Screen) -> Unit) {
    val vm = LocalCimocSession.current.sources
    val repos = LocalCimocRepos.current
    val state by vm.ui.collectAsState()
    var homes by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showImport by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { if (state.items.isEmpty()) vm.load() }
    LaunchedEffect(state.items) {
        val map = HashMap<Int, String>()
        for (source in state.items) {
            val url = repos.sources.homepage(source.type)
            if (url.isNotEmpty()) map[source.type] = url
        }
        homes = map
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { if (!state.importing) showImport = false },
            title = { Text("引入图源") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("脚本内容或 http(s) 地址") },
                    minLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.import(draft)
                        draft = ""
                        showImport = false
                    },
                    enabled = !state.importing
                ) { Text("引入") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }, enabled = !state.importing) { Text("取消") }
            }
        )
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showImport = true }) { Text("添加") }
            state.message?.let {
                Text(
                    it,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        if (state.loading && state.items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        LazyColumn(Modifier.weight(1f)) {
        items(state.items, key = { it.type }) { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(Screen.Explore(source.type)) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp
                )
                if (source.invalid) {
                    Text(
                        "失效",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                homes[source.type]?.let { home ->
                    IconButton(onClick = { onOpen(Screen.WebBrowser(home)) }) {
                        Icon(Icons.Default.Language, contentDescription = "打开首页")
                    }
                }
                if (source.custom) {
                    IconButton(onClick = { vm.remove(source) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
                Switch(checked = source.enable, onCheckedChange = { vm.toggle(source) })
            }
        }
    }
    }
}
