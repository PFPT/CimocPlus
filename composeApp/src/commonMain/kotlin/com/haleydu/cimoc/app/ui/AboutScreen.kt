package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocActions

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val actions = LocalCimocActions.current
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = { PageHeader("关于", onBack = onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Cimoc", fontSize = 22.sp)
            Text("Android 漫画阅读器", modifier = Modifier.padding(top = 8.dp, bottom = 12.dp), fontSize = 14.sp)
            ListRow("项目主页") { actions.openUrl("https://github.com/Haleydu/Cimoc") }
        }
    }
}
