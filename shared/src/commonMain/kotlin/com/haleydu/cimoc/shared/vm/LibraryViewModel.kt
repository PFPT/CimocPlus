package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val comics: ComicRepository,
    private val sources: SourceRepository
) {
    enum class Tab { Favorite, History, Download, Local }

    data class UiState(
        val tab: Tab = Tab.Favorite,
        val items: List<ComicItem> = emptyList(),
        val loading: Boolean = false,
        val ended: Boolean = false,
        val titles: Map<Int, String> = emptyMap()
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(tab: Tab = _ui.value.tab) {
        _ui.update { it.copy(tab = tab, items = emptyList(), ended = false, loading = false) }
        loadMore()
    }

    fun loadMore() {
        val state = _ui.value
        if (state.loading || state.ended) return
        _ui.update { it.copy(loading = true) }
        val offset = state.items.size
        scope.launch {
            val page = runCatching {
                when (state.tab) {
                    Tab.Favorite -> comics.listFavoritePage(offset, PAGE)
                    Tab.History -> comics.listHistoryPage(offset, PAGE)
                    Tab.Download -> comics.listDownloadPage(offset, PAGE)
                    Tab.Local -> comics.listLocalPage(offset, PAGE)
                }
            }.getOrDefault(emptyList())
            _ui.update {
                if (it.tab != state.tab) return@update it
                val merged = it.items + page
                it.copy(
                    items = merged,
                    loading = false,
                    ended = page.size < PAGE,
                    titles = merged.associate { comic -> comic.source to sources.sourceTitle(comic.source) }
                )
            }
        }
    }

    fun unfavorite(id: Long?) {
        id ?: return
        scope.launch {
            runCatching { comics.unfavorite(id) }
            load(Tab.Favorite)
        }
    }

    fun deleteHistory(id: Long?) {
        id ?: return
        scope.launch {
            runCatching { comics.deleteHistory(id) }
            load(Tab.History)
        }
    }

    companion object {
        const val PAGE = 40
    }
}
