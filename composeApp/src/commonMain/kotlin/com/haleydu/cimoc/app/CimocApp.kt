package com.haleydu.cimoc.app

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.haleydu.cimoc.app.di.startCimocKoin
import com.haleydu.cimoc.app.nav.Screen
import com.haleydu.cimoc.app.nav.Tab
import com.haleydu.cimoc.app.theme.CimocTheme
import com.haleydu.cimoc.app.theme.LocalAppIcons
import com.haleydu.cimoc.app.theme.ProvideAppIcons
import com.haleydu.cimoc.shared.repo.CimocRepositories

@Composable
fun CimocApp(
    repos: CimocRepositories,
    actions: CimocActions = CimocActions(),
    start: Screen = Screen.Library
) {
    val session = remember(repos) {
        startCimocKoin(repos)
        CimocSession().also { it.settings.load() }
    }
    val appearance by session.settings.ui.collectAsState()
    LaunchedEffect(appearance.night) {
        actions.onNightChanged(appearance.night)
    }
    ProvideAppIcons(appearance.iconStyle) {
        CimocTheme(theme = appearance.theme, dark = appearance.night) {
            CompositionLocalProvider(
                LocalCimocRepos provides repos,
                LocalCimocSession provides session,
                LocalCimocActions provides actions
            ) {
                CimocRoot(start)
            }
        }
    }
}

@Composable
private fun CimocRoot(start: Screen) {
    val actions = LocalCimocActions.current
    val session = LocalCimocSession.current
    Navigator(start) { navigator ->
        val current = navigator.lastItem
        val showBottom = current is Screen.Library || current is Screen.Search || current is Screen.Browser || current is Screen.Sources || current is Screen.Profile
        val isReader = current is Screen.Reader
        val isWebReader = current is Screen.WebReader
        val isWebBrowser = current is Screen.WebBrowser
        val keepReader = isReader || current is Screen.SourceHits
        val tab = when (current) {
            is Screen.Library -> Tab.Library
            is Screen.Search -> Tab.Search
            is Screen.Browser -> Tab.Browser
            is Screen.Sources -> Tab.Sources
            is Screen.Profile -> Tab.Profile
            else -> null
        }
        LaunchedEffect(current, navigator) {
            if (!isWebBrowser) {
                actions.bindBack {
                    if (navigator.canPop) {
                        navigator.pop()
                        true
                    } else {
                        false
                    }
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
                actions.setBrightness(null)
                session.reader.reset()
            }
            if (!keepReader && !isWebReader) {
                actions.setImmersive(false)
                actions.setKeepScreenOn(false)
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottom) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        val icons = LocalAppIcons.current
                        val colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Library,
                            onClick = { navigator.replaceAll(Screen.Library) },
                            icon = { Icon(icons.library, contentDescription = "书架") },
                            label = { Text("书架") },
                            colors = colors
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Search,
                            onClick = { navigator.replaceAll(Screen.Search) },
                            icon = { Icon(icons.search, contentDescription = "搜索") },
                            label = { Text("搜索") },
                            colors = colors
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Browser,
                            onClick = { navigator.replaceAll(Screen.Browser) },
                            icon = { Icon(icons.browser, contentDescription = "浏览") },
                            label = { Text("浏览") },
                            colors = colors
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Sources,
                            onClick = { navigator.replaceAll(Screen.Sources) },
                            icon = { Icon(icons.sources, contentDescription = "图源") },
                            label = { Text("图源") },
                            colors = colors
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Profile,
                            onClick = { navigator.replaceAll(Screen.Profile) },
                            icon = { Icon(icons.profile, contentDescription = "我的") },
                            label = { Text("我的") },
                            colors = colors
                        )
                    }
                }
            }
        ) { padding ->
            val contentModifier = Modifier.padding(padding).then(
                if (isReader || isWebReader || isWebBrowser) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
            Box(contentModifier) {
                CurrentScreen()
            }
        }
    }
}
