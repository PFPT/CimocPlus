package com.haleydu.cimoc.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.haleydu.cimoc.app.di.startCimocKoin
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.app.nav.Tab
import com.haleydu.cimoc.app.theme.CimocTheme
import com.haleydu.cimoc.shared.repo.CimocRepositories

@Composable
fun CimocApp(
    repos: CimocRepositories,
    actions: CimocActions = CimocActions(),
    dark: Boolean = true,
    start: Screen = Screen.Library
) {
    val session = remember(repos) {
        startCimocKoin(repos)
        CimocSession()
    }
    CimocTheme(dark = dark) {
        CompositionLocalProvider(
            LocalCimocRepos provides repos,
            LocalCimocSession provides session,
            LocalCimocActions provides actions
        ) {
            CimocRoot(start)
        }
    }
}

@Composable
private fun CimocRoot(start: Screen) {
    val actions = LocalCimocActions.current
    val session = LocalCimocSession.current
    Navigator(start) { navigator ->
        val current = navigator.lastItem
        val showBottom = current is Screen.Library || current is Screen.Search || current is Screen.Sources || current is Screen.Profile
        val isReader = current is Screen.Reader
        val keepReader = isReader || current is Screen.SourceHits
        val tab = when (current) {
            is Screen.Library -> Tab.Library
            is Screen.Search -> Tab.Search
            is Screen.Sources -> Tab.Sources
            is Screen.Profile -> Tab.Profile
            else -> null
        }
        LaunchedEffect(current, navigator) {
            actions.bindBack {
                if (navigator.canPop) {
                    navigator.pop()
                    true
                } else {
                    false
                }
            }
            actions.bindVolume(
                if (isReader) {
                    { up -> session.reader.onVolume(up) }
                } else {
                    { _ -> false }
                }
            )
            if (!keepReader) {
                actions.setImmersive(false)
                actions.setKeepScreenOn(false)
                actions.setBrightness(null)
                session.reader.reset()
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottom) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        NavigationBarItem(
                            selected = tab == Tab.Library,
                            onClick = { navigator.replaceAll(Screen.Library) },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "书架") },
                            label = { Text("书架") }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Search,
                            onClick = { navigator.replaceAll(Screen.Search) },
                            icon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                            label = { Text("搜索") }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Sources,
                            onClick = { navigator.replaceAll(Screen.Sources) },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "图源") },
                            label = { Text("图源") }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Profile,
                            onClick = { navigator.replaceAll(Screen.Profile) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                            label = { Text("我的") }
                        )
                    }
                }
            }
        ) { padding ->
            val contentModifier = Modifier.padding(padding).then(
                if (isReader) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
            Box(contentModifier) {
                CurrentScreen()
            }
        }
    }
}
