package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen

@Composable
fun SourceScreen(onOpen: (Screen) -> Unit) {
    val vm = LocalCimocSession.current.sources
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { if (state.items.isEmpty()) vm.load() }

    if (state.loading && state.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
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
                Switch(checked = source.enable, onCheckedChange = { vm.toggle(source) })
            }
        }
    }
}
