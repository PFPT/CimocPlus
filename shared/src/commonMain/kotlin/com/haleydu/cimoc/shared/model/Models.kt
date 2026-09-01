package com.haleydu.cimoc.shared.model

data class ComicItem(
    val id: Long? = null,
    val source: Int,
    val cid: String,
    val title: String,
    val cover: String = "",
    val author: String? = null,
    val intro: String? = null,
    val update: String? = null,
    val last: String? = null,
    val chapter: String? = null,
    val url: String? = null,
    val favorite: Boolean = false,
    val history: Long? = null,
    val highlight: Boolean = false,
    val local: Boolean = false,
    val finish: Boolean = false,
    val page: Int = 1
)

data class ChapterItem(
    val id: Long? = null,
    val sourceComic: Long = 0L,
    val title: String,
    val path: String,
    val count: Int = 0,
    val complete: Boolean = false
)

data class PageImage(
    val id: Long? = null,
    val chapterId: Long? = null,
    val index: Int,
    val urls: List<String>,
    val lazy: Boolean = false
) {
    val displayUrl: String get() = urls.firstOrNull().orEmpty()
}

data class SourceItem(
    val id: Long? = null,
    val type: Int,
    val title: String,
    val enable: Boolean,
    val invalid: Boolean = false
)

sealed class LoadResult<out T> {
    data class Ok<T>(val value: T) : LoadResult<T>()
    data class Err(val message: String, val network: Boolean = false) : LoadResult<Nothing>()
}

fun sourceComicKey(source: Int, id: Long?): Long {
    val idPart = id ?: 0L
    return "$source${"000"}$idPart".toLongOrNull() ?: 0L
}

object PrefKeys {
    const val NIGHT = "pref_night"
    const val READER_MODE = "pref_reader_mode"
    const val READER_VOLUME = "pref_reader_volume_key_controls_page_turning"
    const val READER_PRELOAD = "pref_reader_preload"
    const val READER_AUTO = "pref_reader_auto"
    const val READER_AUTO_INTERVAL = "pref_reader_auto_interval"
    const val READER_WHITE_EDGE = "pref_reader_white_edge"
    const val READER_KEEP_BRIGHT = "pref_reader_keep_on"
    const val READER_HIDE_NAV = "pref_reader_hide_nav"
    const val READER_REVERSE = "pref_reader_paging_reverse"
    const val READER_VERTICAL = "pref_reader_vertical"
    const val READER_COLOR_INVERT = "pref_reader_color_invert"
    const val READER_COLOR_GRAY = "pref_reader_color_gray"
    const val CHAPTER_ASCEND = "pref_chapter_ascend_mode"
}

object ReaderMode {
    const val PAGE = 0
    const val STREAM = 1
}
