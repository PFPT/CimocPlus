package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocRepos
import com.haleydu.cimoc.shared.vm.SourceListViewModel

@Composable
fun SourceScreen() {
    val repos = LocalCimocRepos.current
    val vm = remember { SourceListViewModel(repos.sources) }
    DisposableEffect(vm) { onDispose { vm.clear() } }
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    if (state.loading && state.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.items, key = { it.type }) { source ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
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
                        color = MaterialTheme.colors.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Switch(checked = source.enable, onCheckedChange = { vm.toggle(source) })
            }
        }
    }
}
