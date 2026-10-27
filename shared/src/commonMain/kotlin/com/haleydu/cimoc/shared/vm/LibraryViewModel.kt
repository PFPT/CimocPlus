package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.TagIds
import com.haleydu.cimoc.shared.model.TagItem
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import com.haleydu.cimoc.shared.repo.TagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val comics: ComicRepository,
    private val sources: SourceRepository,
    private val manga: MangaRepository,
    private val tags: TagRepository
) {
    enum class Tab { Favorite, History, Download, Local }

    data class UiState(
        val tab: Tab = Tab.Favorite,
        val items: List<ComicItem> = emptyList(),
        val loading: Boolean = false,
        val ended: Boolean = false,
        val titles: Map<Int, String> = emptyMap(),
        val tags: List<TagItem> = emptyList(),
        val tagId: Long = TagIds.ALL,
        val checking: Boolean = false,
        val checkMessage: String? = null
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(tab: Tab = _ui.value.tab, tagId: Long = _ui.value.tagId) {
        _ui.update {
            it.copy(
                tab = tab,
                tagId = if (tab == Tab.Favorite) tagId else TagIds.ALL,
                items = emptyList(),
                ended = false,
                loading = false
            )
        }
        if (tab == Tab.Favorite) loadTags()
        loadMore()
    }

    fun resume() {
        val state = _ui.value
        if (state.items.isEmpty()) {
            load(state.tab, state.tagId)
            return
        }
        if (state.loading) return
        _ui.update { it.copy(loading = true) }
        val tab = state.tab
        val tagId = state.tagId
        scope.launch {
            val page = runCatching { fetchPage(tab, tagId, 0) }.getOrNull()
            _ui.update {
                if (it.tab != tab || it.tagId != tagId) return@update it
                if (page == null) return@update it.copy(loading = false)
                if (page.isEmpty()) {
                    return@update it.copy(items = emptyList(), loading = false, ended = true, titles = emptyMap())
                }
                val ids = page.mapNotNull { comic -> comic.id }.toSet()
                val tail = it.items.drop(page.size).filter { comic ->
                    val id = comic.id
                    id == null || id !in ids
                }
                val merged = page + tail
                it.copy(
                    items = merged,
                    loading = false,
                    ended = if (tail.isEmpty()) page.size < PAGE else it.ended,
                    titles = titlesFor(merged)
                )
            }
        }
    }

    fun selectTag(id: Long) {
        load(Tab.Favorite, id)
    }

    fun checkFavorites() {
        if (_ui.value.checking) return
        _ui.update { it.copy(checking = true, checkMessage = null) }
        scope.launch {
            val count = runCatching { manga.checkFavorites() }.getOrNull()
            val message = when (count) {
                null -> "检查更新失败"
                0 -> "已是最新"
                else -> "发现 ${count} 部更新"
            }
            _ui.update { it.copy(checking = false, checkMessage = message) }
            load(Tab.Favorite, _ui.value.tagId)
        }
    }

    fun consumeCheckMessage() {
        _ui.update { it.copy(checkMessage = null) }
    }

    fun loadMore() {
        val state = _ui.value
        if (state.loading || state.ended) return
        _ui.update { it.copy(loading = true) }
        val offset = state.items.size
        val tagId = state.tagId
        scope.launch {
            val page = runCatching { fetchPage(state.tab, tagId, offset) }.getOrDefault(emptyList())
            _ui.update {
                if (it.tab != state.tab || it.tagId != tagId) return@update it
                val merged = it.items + page
                it.copy(
                    items = merged,
                    loading = false,
                    ended = page.size < PAGE,
                    titles = titlesFor(merged)
                )
            }
        }
    }

    fun unfavorite(id: Long?) {
        id ?: return
        scope.launch {
            runCatching { comics.unfavorite(id) }
            load(Tab.Favorite, _ui.value.tagId)
        }
    }

    fun deleteHistory(id: Long?) {
        id ?: return
        scope.launch {
            runCatching { comics.deleteHistory(id) }
            load(Tab.History)
        }
    }

    private suspend fun fetchPage(tab: Tab, tagId: Long, offset: Int): List<ComicItem> {
        return when (tab) {
            Tab.Favorite -> when (tagId) {
                TagIds.ALL -> comics.listFavoritePage(offset, PAGE)
                TagIds.CONTINUE -> comics.listContinuePage(offset, PAGE)
                TagIds.FINISH -> comics.listFinishPage(offset, PAGE)
                else -> comics.listFavoriteByTagPage(tagId, offset, PAGE)
            }
            Tab.History -> comics.listHistoryPage(offset, PAGE)
            Tab.Download -> comics.listDownloadPage(offset, PAGE)
            Tab.Local -> comics.listLocalPage(offset, PAGE)
        }
    }

    private fun titlesFor(comics: List<ComicItem>): Map<Int, String> {
        val titles = HashMap<Int, String>()
        for (comic in comics) {
            if (comic.source !in titles) {
                titles[comic.source] = sources.sourceTitle(comic.source)
            }
        }
        return titles
    }

    private fun loadTags() {
        scope.launch {
            val list = runCatching { tags.list() }.getOrDefault(emptyList())
            _ui.update { it.copy(tags = list) }
        }
    }

    companion object {
        const val PAGE = 40
    }
}
