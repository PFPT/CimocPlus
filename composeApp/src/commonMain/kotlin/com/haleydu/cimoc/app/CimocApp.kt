package com.haleydu.cimoc.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.app.nav.Tab
import com.haleydu.cimoc.app.theme.CimocTheme
import com.haleydu.cimoc.app.ui.DetailScreen
import com.haleydu.cimoc.app.ui.LibraryScreen
import com.haleydu.cimoc.app.ui.ProfileScreen
import com.haleydu.cimoc.app.ui.ReaderScreen
import com.haleydu.cimoc.app.ui.SearchScreen
import com.haleydu.cimoc.app.ui.SourceScreen
import com.haleydu.cimoc.shared.repo.CimocRepositories

@Composable
fun CimocApp(
    repos: CimocRepositories,
    actions: CimocActions = CimocActions(),
    dark: Boolean = false
) {
    CimocTheme(dark = dark) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalCimocRepos provides repos,
            LocalCimocActions provides actions
        ) {
            CimocRoot()
        }
    }
}

@Composable
private fun CimocRoot() {
    val stack = remember { mutableStateListOf<Screen>(Screen.Library) }
    var tab by remember { mutableStateOf(Tab.Library) }
    val current = stack.last()
    val showBottom = current is Screen.Library || current is Screen.Search || current is Screen.Sources || current is Screen.Profile

    Scaffold(
        bottomBar = {
            if (showBottom) {
                Surface(elevation = 8.dp, color = MaterialTheme.colors.surface) {
                    Row(Modifier.fillMaxWidth()) {
                        BottomNavItem(Tab.Library, tab, Icons.Default.MenuBook, "书架") {
                            tab = Tab.Library
                            stack.clear()
                            stack.add(Screen.Library)
                        }
                        BottomNavItem(Tab.Search, tab, Icons.Default.Search, "搜索") {
                            tab = Tab.Search
                            stack.clear()
                            stack.add(Screen.Search)
                        }
                        BottomNavItem(Tab.Sources, tab, Icons.Default.Explore, "图源") {
                            tab = Tab.Sources
                            stack.clear()
                            stack.add(Screen.Sources)
                        }
                        BottomNavItem(Tab.Profile, tab, Icons.Default.Person, "我的") {
                            tab = Tab.Profile
                            stack.clear()
                            stack.add(Screen.Profile)
                        }
                    }
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when (val screen = current) {
                Screen.Library -> LibraryScreen { stack.add(it) }
                Screen.Search -> SearchScreen { stack.add(it) }
                Screen.Sources -> SourceScreen()
                Screen.Profile -> ProfileScreen()
                is Screen.Detail -> DetailScreen(
                    args = screen,
                    onBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
                    onRead = { stack.add(it) }
                )
                is Screen.Reader -> ReaderScreen(
                    args = screen,
                    onBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    value: Tab,
    selected: Tab,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val color = if (selected == value) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, color = color, fontSize = 12.sp)
    }
}
