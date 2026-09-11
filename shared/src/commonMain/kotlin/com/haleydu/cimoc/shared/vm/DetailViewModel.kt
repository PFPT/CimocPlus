package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PrefKeys
import com.haleydu.cimoc.shared.model.TagItem
import com.haleydu.cimoc.shared.model.unreadChapters
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.DownloadRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SettingsRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import com.haleydu.cimoc.shared.repo.TagRepository
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
    private val downloads: DownloadRepository,
    private val settings: SettingsRepository,
    private val tags: TagRepository
) {
    data class UiState(
        val comic: ComicItem? = null,
        val chapters: List<ChapterItem> = emptyList(),
        val sourceTitle: String = "",
        val loading: Boolean = false,
        val error: String? = null,
        val reversed: Boolean = true,
        val downloadMessage: String? = null,
        val tags: List<TagItem> = emptyList(),
        val comicTagIds: Set<Long> = emptySet(),
        val tagDialog: Boolean = false,
        val selecting: Boolean = false,
        val selectedPaths: Set<String> = emptySet()
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
        _ui.update {
            it.copy(
                loading = true,
                error = null,
                reversed = !settings.bool(PrefKeys.CHAPTER_ASCEND, false),
                selecting = false,
                selectedPaths = emptySet(),
                tagDialog = false
            )
        }
        scope.launch {
            if (id > 0) runCatching { comics.clearHighlight(id) }
            val seed = if (id > 0) {
                comics.load(id)
            } else {
                comics.loadOrCreate(source, cid.orEmpty())
            } ?: ComicItem(source = source, cid = cid.orEmpty(), title = title.orEmpty(), cover = cover.orEmpty(), author = author)
            val merged = seed.copy(
                title = seed.title.ifEmpty { title.orEmpty() },
                cover = seed.cover.ifEmpty { cover.orEmpty() },
                author = seed.author ?: author,
                highlight = false
            )
            _ui.update {
                it.copy(
                    comic = merged,
                    sourceTitle = sources.sourceTitle(merged.source)
                )
            }
            loadTags(merged.id)
            when (val result = manga.loadDetail(merged)) {
                is LoadResult.Ok -> _ui.update {
                    it.copy(
                        comic = result.value.first.copy(highlight = false),
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
        val next = !_ui.value.reversed
        settings.setBool(PrefKeys.CHAPTER_ASCEND, !next)
        _ui.update { it.copy(reversed = next) }
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
        val chapters = unreadChapters(state.chapters, comic.last)
        enqueue(comic, chapters)
    }

    fun enterSelect() {
        _ui.update { it.copy(selecting = true, selectedPaths = emptySet()) }
    }

    fun cancelSelect() {
        _ui.update { it.copy(selecting = false, selectedPaths = emptySet()) }
    }

    fun toggleChapter(path: String) {
        val chapter = _ui.value.chapters.firstOrNull { it.path == path } ?: return
        if (chapter.complete) return
        _ui.update {
            val next = if (path in it.selectedPaths) it.selectedPaths - path else it.selectedPaths + path
            it.copy(selectedPaths = next)
        }
    }

    fun selectAllUndownloaded() {
        val paths = _ui.value.chapters.filter { !it.complete && it.path.isNotEmpty() }.map { it.path }.toSet()
        _ui.update { it.copy(selectedPaths = paths) }
    }

    fun downloadSelected() {
        val state = _ui.value
        val comic = state.comic ?: return
        val chapters = state.chapters.filter { it.path in state.selectedPaths && !it.complete }
        enqueue(comic, chapters)
        _ui.update { it.copy(selecting = false, selectedPaths = emptySet()) }
    }

    fun openTagDialog() {
        scope.launch {
            loadTags(_ui.value.comic?.id)
            _ui.update { it.copy(tagDialog = true) }
        }
    }

    fun closeTagDialog() {
        _ui.update { it.copy(tagDialog = false) }
    }

    fun toggleTag(id: Long) {
        val comicId = _ui.value.comic?.id ?: return
        val next = if (id in _ui.value.comicTagIds) _ui.value.comicTagIds - id else _ui.value.comicTagIds + id
        _ui.update { it.copy(comicTagIds = next) }
        scope.launch {
            runCatching { tags.setTags(comicId, next.toList()) }
        }
    }

    fun createTag(title: String) {
        val comicId = _ui.value.comic?.id ?: return
        scope.launch {
            val created = runCatching { tags.create(title) }.getOrNull() ?: return@launch
            val next = _ui.value.comicTagIds + created.id
            runCatching { tags.setTags(comicId, next.toList()) }
            loadTags(comicId)
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

    private fun enqueue(comic: ComicItem, chapters: List<ChapterItem>) {
        if (chapters.isEmpty()) {
            _ui.update { it.copy(downloadMessage = "没有可下载的章节") }
            return
        }
        scope.launch {
            when (val result = downloads.enqueue(comic, chapters)) {
                is LoadResult.Ok -> _ui.update { it.copy(downloadMessage = "已加入 ${result.value} 章下载") }
                is LoadResult.Err -> _ui.update { it.copy(downloadMessage = result.message) }
            }
        }
    }

    private suspend fun loadTags(comicId: Long?) {
        val all = runCatching { tags.list() }.getOrDefault(emptyList())
        val owned = if (comicId != null && comicId > 0) {
            runCatching { tags.tagsOf(comicId) }.getOrDefault(emptyList()).map { it.id }.toSet()
        } else {
            emptySet()
        }
        _ui.update { it.copy(tags = all, comicTagIds = owned) }
    }
}
