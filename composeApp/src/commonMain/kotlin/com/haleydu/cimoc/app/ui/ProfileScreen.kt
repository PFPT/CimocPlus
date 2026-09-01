package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.nav.Screen

@Composable
fun ProfileScreen(onOpen: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        ProfileRow("设置") { onOpen(Screen.Settings) }
        HorizontalDivider()
        ProfileRow("备份") { onOpen(Screen.Backup) }
        HorizontalDivider()
        ProfileRow("关于") { onOpen(Screen.About) }
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
