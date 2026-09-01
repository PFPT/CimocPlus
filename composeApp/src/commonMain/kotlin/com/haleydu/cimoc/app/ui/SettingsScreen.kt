package com.haleydu.cimoc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.LocalCimocActions
import com.haleydu.cimoc.app.LocalCimocSession
import com.haleydu.cimoc.shared.model.ReaderMode

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = LocalCimocSession.current.settings
    val actions = LocalCimocActions.current
    val state by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(state.night) { actions.onNightChanged(state.night) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingSwitch("夜间模式", state.night) { vm.setNight(it) }
            SettingSwitch("条漫模式", state.readerMode == ReaderMode.STREAM) {
                vm.setReaderMode(if (it) ReaderMode.STREAM else ReaderMode.PAGE)
            }
            SettingSwitch("纵向分页", state.vertical) { vm.setVertical(it) }
            SettingSwitch("反向翻页", state.reverse) { vm.setReverse(it) }
            SettingSwitch("音量键翻页", state.volumeKeys) { vm.setVolumeKeys(it) }
            SettingSwitch("预载下一章", state.preload) { vm.setPreload(it) }
            SettingSwitch("自动翻页", state.auto) { vm.setAuto(it) }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("自动翻页间隔 ${state.autoInterval} 秒", fontSize = 16.sp)
                Slider(
                    value = state.autoInterval.toFloat(),
                    onValueChange = { vm.setAutoInterval(it.toInt()) },
                    valueRange = 1f..15f,
                    steps = 13
                )
            }
            SettingSwitch("阅读常亮", state.keepBright) { vm.setKeepBright(it) }
            SettingSwitch("隐藏系统栏", state.hideNav) { vm.setHideNav(it) }
            SettingSwitch("裁切白边", state.whiteEdge) { vm.setWhiteEdge(it) }
            SettingSwitch("反色", state.invert) { vm.setInvert(it) }
            SettingSwitch("灰阶", state.gray) { vm.setGray(it) }
        }
    }
}

@Composable
fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
