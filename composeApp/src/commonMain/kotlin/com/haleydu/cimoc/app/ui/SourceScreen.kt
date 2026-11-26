package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.app.theme.LocalAppIcons

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
                        vm.stage(draft)
                        draft = ""
                        showImport = false
                    },
                    enabled = !state.importing
                ) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }, enabled = !state.importing) { Text("取消") }
            }
        )
    }
    val preview = state.preview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { if (!state.importing) vm.dismissPreview() },
            title = { Text("确认引入") },
            text = { Text("${preview.title}\n${preview.host}") },
            confirmButton = {
                TextButton(onClick = { vm.confirmImport() }, enabled = !state.importing) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissPreview() }, enabled = !state.importing) { Text("取消") }
            }
        )
    }
    Column(Modifier.fillMaxSize()) {
        val icons = LocalAppIcons.current
        PageHeader("图源", actions = {
            TextButton(onClick = { showImport = true }) { Text("添加") }
        })
        state.message?.let {
            Text(
                it,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        if (state.loading && state.items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        if (state.items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "还没有图源。添加脚本后再搜索。保存的普通网站会标成仅浏览，不能搜索。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        items(state.items, key = { it.type }) { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        val home = homes[source.type]
                        if (source.web && !home.isNullOrEmpty()) {
                            onOpen(Screen.WebBrowser(home))
                        } else {
                            onOpen(Screen.Explore(source.type))
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = source.title, fontSize = 16.sp)
                    if (source.web) {
                        Text("仅浏览，不能搜索", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else if (source.invalid) {
                        Text("失效", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
                homes[source.type]?.let { home ->
                    IconButton(onClick = { onOpen(Screen.WebBrowser(home)) }) {
                        Icon(icons.browser, contentDescription = "打开首页")
                    }
                }
                if (source.custom) {
                    IconButton(onClick = { vm.remove(source) }) {
                        Icon(icons.delete, contentDescription = "删除")
                    }
                }
                Switch(checked = source.enable, onCheckedChange = { vm.toggle(source) })
            }
        }
    }
    }
}
