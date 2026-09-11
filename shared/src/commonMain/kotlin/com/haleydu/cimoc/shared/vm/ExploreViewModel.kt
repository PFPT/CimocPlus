package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.ExploreFilter
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.repo.ExploreRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val explore: ExploreRepository,
    private val sources: SourceRepository
) {
    data class UiState(
        val source: Int = -1,
        val title: String = "",
        val items: List<ComicItem> = emptyList(),
        val page: Int = 0,
        val loading: Boolean = false,
        val ended: Boolean = false,
        val error: String? = null,
        val filters: List<ExploreFilter> = emptyList(),
        val selected: Map<Int, String> = emptyMap()
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun open(type: Int) {
        _ui.update {
            it.copy(
                source = type,
                title = sources.sourceTitle(type),
                items = emptyList(),
                page = 0,
                ended = false,
                error = null,
                loading = true,
                filters = emptyList(),
                selected = emptyMap()
            )
        }
        scope.launch {
            val filters = runCatching { explore.loadFilters(type) }.getOrDefault(emptyList())
            if (_ui.value.source != type) return@launch
            val selected = filters.associate { filter ->
                filter.attr to (filter.options.firstOrNull()?.second ?: "")
            }
            _ui.update {
                it.copy(
                    filters = filters,
                    selected = selected,
                    loading = false
                )
            }
            loadMore()
        }
    }

    fun setFilter(attr: Int, value: String) {
        val state = _ui.value
        if (state.selected[attr] == value) return
        _ui.update {
            it.copy(
                selected = it.selected + (attr to value),
                items = emptyList(),
                page = 0,
                ended = false,
                loading = false,
                error = null
            )
        }
        loadMore()
    }

    fun loadMore() {
        val state = _ui.value
        if (state.loading || state.ended || state.source < 0) return
        val next = state.page + 1
        val source = state.source
        val selected = state.selected
        val args = filterArgs(state)
        _ui.update { it.copy(loading = true, error = null) }
        scope.launch {
            when (val result = explore.loadPage(source, next, args)) {
                is LoadResult.Ok -> _ui.update {
                    if (it.source != source || it.selected != selected) return@update it
                    val merged = it.items + result.value
                    it.copy(
                        items = merged,
                        page = next,
                        loading = false,
                        ended = result.value.isEmpty(),
                        error = if (merged.isEmpty()) "暂无内容" else null
                    )
                }
                is LoadResult.Err -> _ui.update {
                    if (it.source != source || it.selected != selected) return@update it
                    it.copy(loading = false, error = result.message, ended = it.items.isEmpty())
                }
            }
        }
    }

    private fun filterArgs(state: UiState): List<String> {
        if (state.filters.isEmpty()) return emptyList()
        val max = state.filters.maxOf { it.attr }
        return (0..max).map { state.selected[it] ?: "" }
    }
}
