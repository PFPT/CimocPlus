package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SourceHitsViewModel(
    private val sources: SourceRepository,
    private val manga: MangaRepository
) {
    data class UiState(
        val title: String = "",
        val items: List<ComicItem> = emptyList(),
        val titles: Map<Int, String> = emptyMap(),
        val loading: Boolean = false,
        val error: String? = null,
        val empty: Boolean = false
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var searchJob: Job? = null
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun search(title: String, excludeSource: Int, excludeCid: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _ui.update { it.copy(error = "没有可搜索的标题", loading = false) }
            return
        }
        searchJob?.cancel()
        _ui.update {
            it.copy(title = trimmed, loading = true, items = emptyList(), error = null, empty = false)
        }
        searchJob = scope.launch {
            val enabled = runCatching { sources.listEnable() }.getOrDefault(emptyList())
            val titles = enabled.associate { it.type to it.title }
            _ui.update { it.copy(titles = titles) }
            if (enabled.isEmpty()) {
                _ui.update { it.copy(loading = false, error = "没有启用的图源") }
                return@launch
            }
            val collected = LinkedHashMap<String, ComicItem>()
            try {
                withContext(Dispatchers.Default) {
                    manga.search(enabled.map { it.type }, trimmed, 1, false).collect { comic ->
                        if (comic.source == excludeSource && comic.cid == excludeCid) return@collect
                        if (comic.title.trim() != trimmed) return@collect
                        collected["${comic.source}_${comic.cid}"] = comic
                        _ui.update { it.copy(items = collected.values.toList(), loading = true) }
                    }
                }
            } catch (_: Exception) {
            }
            _ui.update {
                it.copy(
                    loading = false,
                    items = collected.values.toList(),
                    empty = collected.isEmpty(),
                    error = if (collected.isEmpty()) "没有找到同名漫画" else null
                )
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        scope.cancel()
    }
}
