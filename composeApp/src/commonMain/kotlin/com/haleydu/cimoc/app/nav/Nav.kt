package com.haleydu.cimoc.app.nav

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.haleydu.cimoc.app.ui.AboutScreen
import com.haleydu.cimoc.app.ui.BackupScreen
import com.haleydu.cimoc.app.ui.DetailScreen
import com.haleydu.cimoc.app.ui.ExploreScreen
import com.haleydu.cimoc.app.ui.LibraryScreen
import com.haleydu.cimoc.app.ui.ProfileScreen
import com.haleydu.cimoc.app.ui.ReaderScreen
import com.haleydu.cimoc.app.ui.SearchScreen
import com.haleydu.cimoc.app.ui.SettingsScreen
import com.haleydu.cimoc.app.ui.SourceScreen
import com.haleydu.cimoc.shared.model.ChapterItem

sealed class Screen : VoyagerScreen {
    data object Library : Screen() {
        override val key: ScreenKey = "library"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            LibraryScreen { destination -> navigator.push(destination) }
        }
    }

    data object Search : Screen() {
        override val key: ScreenKey = "search"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            SearchScreen { destination -> navigator.push(destination) }
        }
    }

    data object Sources : Screen() {
        override val key: ScreenKey = "sources"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            SourceScreen { destination -> navigator.push(destination) }
        }
    }

    data object Profile : Screen() {
        override val key: ScreenKey = "profile"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            ProfileScreen { destination -> navigator.push(destination) }
        }
    }

    data object Settings : Screen() {
        override val key: ScreenKey = "settings"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            SettingsScreen(onBack = { navigator.pop() })
        }
    }

    data object Backup : Screen() {
        override val key: ScreenKey = "backup"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            BackupScreen(onBack = { navigator.pop() })
        }
    }

    data object About : Screen() {
        override val key: ScreenKey = "about"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            AboutScreen(onBack = { navigator.pop() })
        }
    }

    data class Explore(val type: Int) : Screen() {
        override val key: ScreenKey = "explore_$type"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            ExploreScreen(
                type = type,
                onBack = { navigator.pop() },
                onOpen = { destination -> navigator.push(destination) }
            )
        }
    }

    data class Detail(
        val id: Long,
        val source: Int,
        val cid: String,
        val title: String? = null,
        val cover: String? = null,
        val author: String? = null
    ) : Screen() {
        override val key: ScreenKey = "detail_${source}_$cid"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            DetailScreen(
                args = this,
                onBack = { navigator.pop() },
                onRead = { destination -> navigator.push(destination) }
            )
        }
    }

    data class Reader(
        val comicId: Long,
        val path: String,
        val chapters: List<ChapterItem> = emptyList()
    ) : Screen() {
        override val key: ScreenKey = "reader_${comicId}_$path"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            ReaderScreen(args = this, onBack = { navigator.pop() })
        }
    }
}

enum class Tab { Library, Search, Sources, Profile }
