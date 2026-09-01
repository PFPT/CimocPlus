package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.DownloadRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val comics: ComicRepository,
    private val manga: MangaRepository,
    private val sources: SourceRepository,
    private val downloads: DownloadRepository
) {
    data class UiState(
        val comic: ComicItem? = null,
        val chapters: List<ChapterItem> = emptyList(),
        val sourceTitle: String = "",
        val loading: Boolean = false,
        val error: String? = null,
        val reversed: Boolean = false,
        val downloadMessage: String? = null
    ) {
        val displayed: List<ChapterItem>
            get() = if (reversed) chapters.reversed() else chapters
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(id: Long, source: Int, cid: String?, title: String?, cover: String?, author: String?) {
        if (id < 0 && cid.isNullOrEmpty()) {
            _ui.update { it.copy(loading = false, error = "漫画信息无效") }
            return
        }
        _ui.update { it.copy(loading = true, error = null) }
        scope.launch {
            val seed = if (id > 0) {
                comics.load(id)
            } else {
                comics.loadOrCreate(source, cid.orEmpty())
            } ?: ComicItem(source = source, cid = cid.orEmpty(), title = title.orEmpty(), cover = cover.orEmpty(), author = author)
            val merged = seed.copy(
                title = seed.title.ifEmpty { title.orEmpty() },
                cover = seed.cover.ifEmpty { cover.orEmpty() },
                author = seed.author ?: author
            )
            _ui.update {
                it.copy(
                    comic = merged,
                    sourceTitle = sources.sourceTitle(merged.source)
                )
            }
            when (val result = manga.loadDetail(merged)) {
                is LoadResult.Ok -> _ui.update {
                    it.copy(
                        comic = result.value.first,
                        chapters = result.value.second,
                        loading = false,
                        error = if (result.value.second.isEmpty()) "暂无章节" else null
                    )
                }
                is LoadResult.Err -> _ui.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun toggleReverse() {
        _ui.update { it.copy(reversed = !it.reversed) }
    }

    fun toggleFavorite() {
        val current = _ui.value.comic ?: return
        scope.launch {
            if (current.favorite) {
                current.id?.let { comics.unfavorite(it) }
                _ui.update { it.copy(comic = current.copy(favorite = false)) }
            } else {
                comics.favorite(current)
                val fresh = comics.loadOrCreate(current.source, current.cid)
                _ui.update { it.copy(comic = fresh.copy(favorite = true)) }
            }
        }
    }

    fun downloadUnread() {
        val state = _ui.value
        val comic = state.comic ?: return
        val chapters = state.chapters.filter { it.path.isNotEmpty() }
        if (chapters.isEmpty()) return
        scope.launch {
            when (val result = downloads.enqueue(comic, chapters)) {
                is LoadResult.Ok -> _ui.update { it.copy(downloadMessage = "已加入 ${result.value} 章下载") }
                is LoadResult.Err -> _ui.update { it.copy(downloadMessage = result.message) }
            }
        }
    }

    fun consumeDownloadMessage() {
        _ui.update { it.copy(downloadMessage = null) }
    }

    suspend fun prepareRead(path: String?): Long {
        val current = _ui.value.comic ?: return -1L
        val chapterPath = path?.takeIf { it.isNotEmpty() } ?: return -1L
        return manga.markRead(current, chapterPath)
    }

    fun defaultChapterPath(): String? {
        val state = _ui.value
        val last = state.comic?.last
        if (!last.isNullOrEmpty()) return last
        return state.displayed.firstOrNull()?.path?.takeIf { it.isNotEmpty() }
    }

    fun clear() {
        scope.cancel()
    }
}
