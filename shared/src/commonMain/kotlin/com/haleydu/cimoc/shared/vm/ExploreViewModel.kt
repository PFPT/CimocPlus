package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
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
        val error: String? = null
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
                loading = false
            )
        }
        loadMore()
    }

    fun loadMore() {
        val state = _ui.value
        if (state.loading || state.ended || state.source < 0) return
        val next = state.page + 1
        _ui.update { it.copy(loading = true, error = null) }
        scope.launch {
            when (val result = explore.loadPage(state.source, next)) {
                is LoadResult.Ok -> _ui.update {
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
                    it.copy(loading = false, error = result.message, ended = it.items.isEmpty())
                }
            }
        }
    }
}
