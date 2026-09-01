package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val comics: ComicRepository,
    private val sources: SourceRepository
) {
    enum class Tab { Favorite, History }

    data class UiState(
        val tab: Tab = Tab.Favorite,
        val items: List<ComicItem> = emptyList(),
        val loading: Boolean = false,
        val titles: Map<Int, String> = emptyMap()
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(tab: Tab = _ui.value.tab) {
        _ui.update { it.copy(tab = tab, loading = true) }
        scope.launch {
            val list = runCatching {
                if (tab == Tab.Favorite) comics.listFavorite() else comics.listHistory()
            }.getOrDefault(emptyList())
            val titles = list.associate { it.source to sources.sourceTitle(it.source) }
            _ui.update { it.copy(items = list, loading = false, titles = titles) }
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

    fun clear() {
        scope.cancel()
    }
}
