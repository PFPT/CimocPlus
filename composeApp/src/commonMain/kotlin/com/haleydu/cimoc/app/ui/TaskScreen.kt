package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    DisposableEffect(Unit) {
        onDispose { vm.clear() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                    Text("暂无下载任务")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
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
    onPause: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val fraction = if (task.max > 0) (task.progress.toFloat() / task.max).coerceIn(0f, 1f) else 0f
    val running = task.state == DownloadTaskItem.STATE_DOING ||
        task.state == DownloadTaskItem.STATE_PARSE ||
        task.state == DownloadTaskItem.STATE_WAIT
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.title, maxLines = 1)
                Text(
                    "${taskStateLabel(task.state)}  ${task.progress}/${task.max}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
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
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = fraction,
            modifier = Modifier.fillMaxWidth()
        )
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
