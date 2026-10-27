package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.shared.model.DownloadTaskItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(onBack: () -> Unit) {
    val session = LocalCimocSession.current
    val vm = remember { session.downloadTasks() }
    val state by vm.ui.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { vm.clear() }
    }

    if (confirmDelete) {
        val count = state.selected.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除任务") },
            text = { Text("删除选中的 $count 个下载任务？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteSelected()
                        confirmDelete = false
                    }
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.selecting) "已选 ${state.selected.size}" else "下载任务") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.selecting) vm.exitSelect() else onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (state.selecting) {
                        TextButton(onClick = {
                            if (state.selected.size == state.items.size) vm.clearSelection() else vm.selectAll()
                        }) {
                            Text(if (state.selected.size == state.items.size && state.items.isNotEmpty()) "取消全选" else "全选")
                        }
                        TextButton(
                            onClick = { confirmDelete = true },
                            enabled = state.selected.isNotEmpty()
                        ) { Text("删除") }
                    } else if (state.items.isNotEmpty()) {
                        TextButton(onClick = { vm.enterSelect() }) { Text("选择") }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading && state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("暂无下载任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            selecting = state.selecting,
                            selected = task.id in state.selected,
                            onToggle = { vm.toggleSelect(task.id) },
                            onPause = { vm.pause(task.id) },
                            onRetry = { vm.retry(task.id) },
                            onDelete = { vm.delete(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: DownloadTaskItem,
    selecting: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onPause: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val fraction = if (task.max > 0) (task.progress.toFloat() / task.max).coerceIn(0f, 1f) else 0f
    val percent = (fraction * 100).toInt()
    val running = task.state == DownloadTaskItem.STATE_DOING ||
        task.state == DownloadTaskItem.STATE_PARSE ||
        task.state == DownloadTaskItem.STATE_WAIT
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selecting) Modifier.clickable(onClick = onToggle) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selecting) {
                    Checkbox(checked = selected, onCheckedChange = { onToggle() })
                }
                Column(Modifier.weight(1f)) {
                    Text(task.title, maxLines = 1, fontSize = 15.sp)
                    Text(
                        "${taskStateLabel(task.state)} · ${task.progress}/${task.max} · $percent%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                if (!selecting) {
                    if (running) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = "暂停")
                        }
                    } else if (task.state != DownloadTaskItem.STATE_FINISH) {
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.Replay, contentDescription = "重试")
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun taskStateLabel(state: Int): String = when (state) {
    DownloadTaskItem.STATE_FINISH -> "完成"
    DownloadTaskItem.STATE_PAUSE -> "暂停"
    DownloadTaskItem.STATE_PARSE -> "解析"
    DownloadTaskItem.STATE_DOING -> "下载中"
    DownloadTaskItem.STATE_WAIT -> "等待"
    DownloadTaskItem.STATE_ERROR -> "失败"
    else -> "等待"
}
