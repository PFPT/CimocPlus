package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocActions

@Composable
fun ProfileScreen() {
    val actions = LocalCimocActions.current
    Column(Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        ProfileRow("设置", actions.openSettings)
        Divider()
        ProfileRow("备份", actions.openBackup)
        Divider()
        ProfileRow("关于", actions.openAbout)
        Divider()
        ProfileRow("经典界面", actions.openClassic)
    }
}

@Composable
private fun ProfileRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        fontSize = 16.sp
    )
}
