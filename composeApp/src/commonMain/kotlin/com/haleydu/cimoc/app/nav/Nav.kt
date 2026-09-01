package com.haleydu.cimoc.app.nav

import com.haleydu.cimoc.shared.model.ChapterItem

sealed interface Screen {
    data object Library : Screen
    data object Search : Screen
    data object Sources : Screen
    data object Profile : Screen
    data class Detail(
        val id: Long,
        val source: Int,
        val cid: String,
        val title: String? = null,
        val cover: String? = null,
        val author: String? = null
    ) : Screen
    data class Reader(
        val comicId: Long,
        val path: String
    ) : Screen
}

object ReaderSession {
    var chapters: List<ChapterItem> = emptyList()
}

enum class Tab { Library, Search, Sources, Profile }

fun Tab.toScreen(): Screen = when (this) {
    Tab.Library -> Screen.Library
    Tab.Search -> Screen.Search
    Tab.Sources -> Screen.Sources
    Tab.Profile -> Screen.Profile
}
