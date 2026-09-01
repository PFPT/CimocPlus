package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.SourceItem
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

class SearchViewModel(
    private val sources: SourceRepository,
    private val manga: MangaRepository
) {
    data class UiState(
        val keyword: String = "",
        val loading: Boolean = false,
        val items: List<ComicItem> = emptyList(),
        val enabledSources: List<SourceItem> = emptyList(),
        val page: Int = 1,
        val ended: Boolean = false,
        val error: String? = null,
        val empty: Boolean = false
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var searchJob: Job? = null
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun loadSources() {
        scope.launch {
            val list = runCatching { sources.listEnable() }.getOrDefault(emptyList())
            _ui.update { it.copy(enabledSources = list) }
        }
    }

    fun search(keyword: String, page: Int = 1) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            _ui.update { it.copy(error = "请输入关键字") }
            return
        }
        searchJob?.cancel()
        _ui.update {
            it.copy(
                keyword = trimmed,
                loading = true,
                items = if (page == 1) emptyList() else it.items,
                page = page,
                ended = false,
                error = null,
                empty = false
            )
        }
        searchJob = scope.launch { runSearch(trimmed, page, append = page > 1) }
    }

    fun searchMore() {
        val state = _ui.value
        if (state.loading || state.ended || state.keyword.isEmpty()) return
        search(state.keyword, state.page + 1)
    }

    private suspend fun runSearch(keyword: String, page: Int, append: Boolean) {
        val types = _ui.value.enabledSources.map { it.type }
        if (types.isEmpty()) {
            _ui.update { it.copy(loading = false, error = "未选择图源") }
            return
        }
        val collected = LinkedHashMap<String, ComicItem>()
        if (append) {
            _ui.value.items.forEach { collected["${it.source}_${it.cid}"] = it }
        }
        val before = collected.size
        var failed = 0
        try {
            withContext(Dispatchers.Default) {
                manga.search(types, keyword, page, false).collect { comic ->
                    val key = "${comic.source}_${comic.cid}"
                    collected[key] = comic
                    _ui.update { it.copy(items = collected.values.toList(), loading = true) }
                }
            }
        } catch (_: Exception) {
            failed++
        }
        _ui.update {
            it.copy(
                loading = false,
                items = collected.values.toList(),
                page = page,
                ended = collected.size == before,
                empty = collected.isEmpty(),
                error = if (collected.isEmpty() && failed > 0) "搜索失败，请稍后重试" else null
            )
        }
    }

    fun clear() {
        searchJob?.cancel()
        scope.cancel()
    }
}
