package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.PrefKeys
import com.haleydu.cimoc.shared.model.ReaderMode
import com.haleydu.cimoc.shared.model.chapterStoryDelta
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val comics: ComicRepository,
    private val manga: MangaRepository,
    private val settings: SettingsRepository
) {
    data class UiState(
        val comic: ComicItem? = null,
        val chapter: ChapterItem? = null,
        val chapters: List<ChapterItem> = emptyList(),
        val pages: List<PageImage> = emptyList(),
        val headers: Map<String, String> = emptyMap(),
        val page: Int = 1,
        val seekPage: Int? = null,
        val mode: Int = ReaderMode.STREAM,
        val overlay: Boolean = false,
        val chapterSheet: Boolean = false,
        val auto: Boolean = false,
        val autoInterval: Int = 4,
        val whiteEdge: Boolean = false,
        val keepBright: Boolean = true,
        val hideNav: Boolean = true,
        val reverse: Boolean = false,
        val vertical: Boolean = false,
        val invert: Boolean = false,
        val gray: Boolean = false,
        val brightness: Int = 0,
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val error: String? = null
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var preloadJob: Job? = null
    private var loadJob: Job? = null
    private var nextCache: Pair<ChapterItem, List<PageImage>>? = null
    private var prevCache: Pair<ChapterItem, List<PageImage>>? = null
    private var lastComicId = 0L
    private var lastPath = ""
    private var lastChapters: List<ChapterItem> = emptyList()
    private var adjacentLock = false
    private val lazyInflight = mutableMapOf<String, Deferred<String?>>()
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(comicId: Long, path: String, chapters: List<ChapterItem>) {
        if (
            lastComicId == comicId &&
            lastPath == path &&
            _ui.value.pages.isNotEmpty() &&
            !_ui.value.loading &&
            _ui.value.error == null
        ) {
            if (chapters.isNotEmpty()) {
                lastChapters = chapters
                _ui.update { it.copy(chapters = chapters) }
            }
            return
        }
        loadJob?.cancel()
        preloadJob?.cancel()
        progressJob?.cancel()
        cancelLazy()
        lastComicId = comicId
        lastPath = path
        lastChapters = chapters
        nextCache = null
        prevCache = null
        adjacentLock = false
        if (path.isEmpty()) {
            _ui.update {
                it.copy(
                    comic = null,
                    chapter = null,
                    pages = emptyList(),
                    error = "章节无效",
                    loading = false
                )
            }
            return
        }
        _ui.update {
            it.copy(
                loading = true,
                error = null,
                comic = null,
                chapter = null,
                pages = emptyList(),
                headers = emptyMap(),
                chapters = chapters,
                mode = settings.int(PrefKeys.READER_MODE, ReaderMode.STREAM),
                auto = settings.bool(PrefKeys.READER_AUTO),
                autoInterval = settings.int(PrefKeys.READER_AUTO_INTERVAL, 4).coerceIn(1, 15),
                whiteEdge = settings.bool(PrefKeys.READER_WHITE_EDGE),
                keepBright = settings.bool(PrefKeys.READER_KEEP_BRIGHT, true),
                hideNav = settings.bool(PrefKeys.READER_HIDE_NAV, true),
                reverse = settings.bool(PrefKeys.READER_REVERSE),
                vertical = settings.bool(PrefKeys.READER_VERTICAL),
                invert = settings.bool(PrefKeys.READER_COLOR_INVERT),
                gray = settings.bool(PrefKeys.READER_COLOR_GRAY),
                brightness = settings.int(PrefKeys.READER_BRIGHTNESS, 0).coerceIn(0, 100),
                page = 1,
                seekPage = null,
                chapterSheet = false,
                overlay = false,
                loadingMore = false
            )
        }
        loadJob = scope.launch {
            val comic = comics.load(comicId)
            if (!matchesRequest(comicId, path)) return@launch
            if (comic == null) {
                _ui.update { it.copy(loading = false, error = "漫画不存在") }
                return@launch
            }
            val chapter = chapters.firstOrNull { it.path == path }
                ?: ChapterItem(title = comic.chapter.orEmpty(), path = path)
            _ui.update {
                it.copy(
                    comic = comic,
                    chapter = chapter,
                    headers = manga.imageHeaders(comic.source),
                    page = comic.page.coerceAtLeast(1)
                )
            }
            applyPages(comic, chapter, comic.page.coerceAtLeast(1), replace = true)
        }
    }

    fun reset(comicId: Long? = null, path: String? = null) {
        if (comicId != null && path != null && (lastComicId != comicId || lastPath != path)) return
        loadJob?.cancel()
        preloadJob?.cancel()
        progressJob?.cancel()
        cancelLazy()
        nextCache = null
        prevCache = null
        adjacentLock = false
        lastComicId = 0L
        lastPath = ""
        lastChapters = emptyList()
        _ui.value = UiState()
    }

    fun retry() {
        load(lastComicId, lastPath, lastChapters)
    }

    fun resolveLazy(page: PageImage, onResolved: (String) -> Unit) {
        val source = _ui.value.comic?.source ?: return
        val url = page.displayUrl
        if (url.isEmpty()) return
        scope.launch {
            val deferred = lazyInflight.getOrPut(url) {
                async { runCatching { manga.resolveLazy(source, url) }.getOrNull() }
            }
            val resolved = try {
                deferred.await()
            } catch (e: CancellationException) {
                throw e
            }
            if (lazyInflight[url] === deferred) lazyInflight.remove(url)
            if (!resolved.isNullOrEmpty()) onResolved(resolved)
        }
    }

    private fun cancelLazy() {
        lazyInflight.values.forEach { it.cancel() }
        lazyInflight.clear()
    }

    fun toggleOverlay() {
        _ui.update { it.copy(overlay = !it.overlay, chapterSheet = false) }
    }

    fun toggleChapterSheet() {
        _ui.update { it.copy(chapterSheet = !it.chapterSheet, overlay = true) }
    }

    fun toggleAuto() {
        val next = !_ui.value.auto
        settings.setBool(PrefKeys.READER_AUTO, next)
        _ui.update { it.copy(auto = next) }
    }

    fun toggleReaderMode() {
        val next = if (_ui.value.mode == ReaderMode.STREAM) ReaderMode.PAGE else ReaderMode.STREAM
        settings.setInt(PrefKeys.READER_MODE, next)
        _ui.update { it.copy(mode = next) }
    }

    fun toggleVertical() {
        val next = !_ui.value.vertical
        settings.setBool(PrefKeys.READER_VERTICAL, next)
        _ui.update { it.copy(vertical = next) }
    }

    fun toggleNight(): Boolean {
        val next = !settings.bool(PrefKeys.NIGHT)
        settings.setBool(PrefKeys.NIGHT, next)
        return next
    }

    fun setBrightness(value: Int) {
        val next = value.coerceIn(0, 100)
        settings.setInt(PrefKeys.READER_BRIGHTNESS, next)
        _ui.update { it.copy(brightness = next) }
    }

    fun setPage(index: Int) {
        val page = index.coerceAtLeast(1)
        _ui.update { it.copy(page = page) }
        progressJob?.cancel()
        progressJob = scope.launch {
            delay(500)
            val id = _ui.value.comic?.id ?: return@launch
            runCatching { manga.saveProgress(id, page) }
        }
    }

    fun seekTo(page: Int) {
        val target = page.coerceIn(1, _ui.value.pages.size.coerceAtLeast(1))
        setPage(target)
        _ui.update { it.copy(seekPage = target) }
    }

    fun consumeSeek() {
        _ui.update { it.copy(seekPage = null) }
    }

    fun turnPage(next: Boolean) {
        val state = _ui.value
        if (state.pages.isEmpty()) return
        val target = if (next) state.page + 1 else state.page - 1
        when {
            target < 1 -> loadAdjacent(false)
            target > state.pages.size -> loadAdjacent(true)
            else -> {
                setPage(target)
                _ui.update { it.copy(seekPage = target) }
            }
        }
    }

    fun tapEdge(startSide: Boolean) {
        val next = if (_ui.value.reverse) startSide else !startSide
        turnPage(next)
    }

    fun jumpChapter(path: String) {
        val comic = _ui.value.comic ?: return
        val chapter = _ui.value.chapters.firstOrNull { it.path == path } ?: return
        lastPath = path
        _ui.update { it.copy(chapterSheet = false, loading = true, error = null) }
        loadJob?.cancel()
        loadJob = scope.launch {
            manga.markRead(comic, chapter.path)
            applyPages(comic, chapter, 1, replace = true)
        }
    }

    fun loadAdjacent(next: Boolean) {
        val state = _ui.value
        val current = state.chapter ?: return
        val list = state.chapters
        if (list.isEmpty() || state.loadingMore || state.loading || adjacentLock) return
        val idx = list.indexOfFirst { it.path == current.path }
        if (idx < 0) return
        val delta = chapterStoryDelta(list)
        val target = list.getOrNull(if (next) idx + delta else idx - delta) ?: return
        val comic = state.comic ?: return
        adjacentLock = true
        lastPath = target.path
        val cached = if (next) nextCache else prevCache
        if (cached != null && cached.first.path == target.path) {
            nextCache = null
            prevCache = null
            _ui.update {
                it.copy(
                    chapter = cached.first,
                    pages = cached.second,
                    page = 1,
                    seekPage = 1,
                    loadingMore = false,
                    error = if (cached.second.isEmpty()) "暂无图片" else null
                )
            }
            scope.launch {
                manga.markRead(comic, target.path)
                delay(400)
                adjacentLock = false
            }
            preloadNeighbors(comic, target)
            return
        }
        _ui.update { it.copy(loadingMore = true, error = null) }
        loadJob?.cancel()
        loadJob = scope.launch {
            manga.markRead(comic, target.path)
            applyPages(comic, target, 1, replace = true)
            delay(400)
            adjacentLock = false
        }
    }

    fun onVolume(up: Boolean): Boolean {
        if (!settings.bool(PrefKeys.READER_VOLUME)) return false
        val next = if (_ui.value.reverse) up else !up
        turnPage(next)
        return true
    }

    private suspend fun applyPages(comic: ComicItem, chapter: ChapterItem, page: Int, replace: Boolean) {
        if (!matchesRequest(comic.id ?: lastComicId, chapter.path)) return
        when (val result = manga.loadPages(comic, chapter)) {
            is LoadResult.Ok -> {
                if (!matchesRequest(comic.id ?: lastComicId, chapter.path)) return
                val pages = result.value
                _ui.update {
                    it.copy(
                        chapter = chapter,
                        pages = pages,
                        page = page.coerceIn(1, pages.size.coerceAtLeast(1)),
                        seekPage = if (replace) page.coerceIn(1, pages.size.coerceAtLeast(1)) else it.seekPage,
                        loading = false,
                        loadingMore = false,
                        error = if (pages.isEmpty()) "暂无图片" else null
                    )
                }
                preloadNeighbors(comic, chapter)
            }
            is LoadResult.Err -> {
                if (!matchesRequest(comic.id ?: lastComicId, chapter.path)) return
                _ui.update {
                    it.copy(loading = false, loadingMore = false, error = result.message)
                }
            }
        }
    }

    private fun matchesRequest(comicId: Long, path: String): Boolean {
        return lastComicId == comicId && lastPath == path
    }

    private fun preloadNeighbors(comic: ComicItem, chapter: ChapterItem) {
        if (!settings.bool(PrefKeys.READER_PRELOAD)) return
        preloadJob?.cancel()
        preloadJob = scope.launch {
            val list = _ui.value.chapters
            val idx = list.indexOfFirst { it.path == chapter.path }
            if (idx < 0) return@launch
            val delta = chapterStoryDelta(list)
            val next = list.getOrNull(idx + delta)
            val prev = list.getOrNull(idx - delta)
            if (next != null) {
                val result = manga.loadPages(comic, next)
                if (result is LoadResult.Ok) nextCache = next to result.value
            }
            if (prev != null) {
                val result = manga.loadPages(comic, prev)
                if (result is LoadResult.Ok) prevCache = prev to result.value
            }
        }
    }
}
