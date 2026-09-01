package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val vm = LocalCimocSession.current.settings
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text("备份") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = { vm.backup() }, modifier = Modifier.fillMaxWidth()) {
                Text("备份收藏")
            }
            state.message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
            Text("恢复文件", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), fontSize = 14.sp)
            LazyColumn {
                items(state.backupFiles) { name ->
                    Text(
                        text = name,
                        modifier = Modifier.fillMaxWidth().clickable { vm.restore(name) }.padding(vertical = 12.dp),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
