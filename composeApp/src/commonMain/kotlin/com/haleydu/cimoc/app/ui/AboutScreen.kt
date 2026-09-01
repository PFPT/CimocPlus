package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val actions = LocalCimocActions.current
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Cimoc", fontSize = 22.sp)
            Text("Android 漫画阅读器", modifier = Modifier.padding(top = 8.dp), fontSize = 14.sp)
            Text(
                "项目主页",
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth().clickable {
                    actions.openUrl("https://github.com/Haleydu/Cimoc")
                },
                fontSize = 16.sp
            )
        }
    }
}
