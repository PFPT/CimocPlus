package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.app.theme.themeSwatch
import com.haleydu.cimoc.shared.model.Appearance

@Composable
fun ProfileScreen(onOpen: (Screen) -> Unit) {
    val vm = LocalCimocSession.current.settings
    val state by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
        PageHeader("我的")
        SectionCard("外观") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("夜间", modifier = Modifier.weight(1f), fontSize = 16.sp)
                Switch(checked = state.night, onCheckedChange = { vm.setNight(it) })
            }
            Text("配色", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(Appearance.THEME_COUNT) { id ->
                    val selected = state.theme == id
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themeSwatch(id))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { vm.setTheme(id) }
                    )
                }
            }
            Text(
                Appearance.themeTitle(state.theme),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
            Text("图标", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(Appearance.ICON_COUNT) { id ->
                    FilterChip(
                        selected = state.iconStyle == id,
                        onClick = { vm.setIconStyle(id) },
                        label = { Text(Appearance.iconTitle(id)) }
                    )
                }
            }
        }
        SectionCard("阅读") {
            ListRow("设置") { onOpen(Screen.Settings) }
        }
        SectionCard("数据") {
            ListRow("备份") { onOpen(Screen.Backup) }
        }
        SectionCard("关于") {
            ListRow("关于") { onOpen(Screen.About) }
        }
    }
}
