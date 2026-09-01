package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val comics: ComicRepository,
    private val manga: MangaRepository
) {
    data class UiState(
        val comic: ComicItem? = null,
        val chapter: ChapterItem? = null,
        val chapters: List<ChapterItem> = emptyList(),
        val pages: List<PageImage> = emptyList(),
        val headers: Map<String, String> = emptyMap(),
        val loading: Boolean = false,
        val error: String? = null
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(comicId: Long, path: String, chapters: List<ChapterItem>) {
        if (path.isEmpty()) {
            _ui.update { it.copy(error = "章节无效") }
            return
        }
        _ui.update { it.copy(loading = true, error = null, chapters = chapters) }
        scope.launch {
            val comic = comics.load(comicId)
            if (comic == null) {
                _ui.update { it.copy(loading = false, error = "漫画不存在") }
                return@launch
            }
            val chapter = chapters.firstOrNull { it.path == path } ?: ChapterItem(title = comic.chapter.orEmpty(), path = path)
            _ui.update {
                it.copy(
                    comic = comic,
                    chapter = chapter,
                    headers = manga.imageHeaders(comic.source)
                )
            }
            when (val result = manga.loadPages(comic, chapter)) {
                is LoadResult.Ok -> _ui.update {
                    it.copy(pages = result.value, loading = false, error = if (result.value.isEmpty()) "暂无图片" else null)
                }
                is LoadResult.Err -> _ui.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun resolveLazy(page: PageImage, onResolved: (String) -> Unit) {
        val source = _ui.value.comic?.source ?: return
        val url = page.displayUrl
        if (url.isEmpty()) return
        scope.launch {
            val resolved = runCatching { manga.resolveLazy(source, url) }.getOrNull()
            if (!resolved.isNullOrEmpty()) onResolved(resolved)
        }
    }

    fun clear() {
        scope.cancel()
    }
}
