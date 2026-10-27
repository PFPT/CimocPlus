package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.PrefKeys
import com.haleydu.cimoc.shared.model.SearchStats
import com.haleydu.cimoc.shared.model.SourceItem
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SettingsRepository
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
    private val manga: MangaRepository,
    private val settings: SettingsRepository
) {
    data class UiState(
        val keyword: String = "",
        val loading: Boolean = false,
        val items: List<ComicItem> = emptyList(),
        val enabledSources: List<SourceItem> = emptyList(),
        val selectedTypes: Set<Int> = emptySet(),
        val page: Int = 1,
        val ended: Boolean = false,
        val error: String? = null,
        val empty: Boolean = false,
        val strict: Boolean = false,
        val history: List<String> = emptyList(),
        val failedSources: Int = 0,
        val pendingOpen: ComicItem? = null
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var searchJob: Job? = null
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui
    private val _items = MutableStateFlow<List<ComicItem>>(emptyList())
    val items: StateFlow<List<ComicItem>> = _items

    init {
        _ui.update { it.copy(strict = settings.bool(PrefKeys.SEARCH_STRICT), history = readHistory()) }
    }

    fun loadSources() {
        scope.launch {
            val list = runCatching { sources.listEnable() }.getOrDefault(emptyList())
            _ui.update {
                val enabled = list.map { source -> source.type }.toSet()
                val selected = if (it.selectedTypes.isEmpty()) {
                    enabled
                } else {
                    it.selectedTypes.filter { type -> type in enabled }.toSet().ifEmpty { enabled }
                }
                it.copy(
                    enabledSources = list,
                    selectedTypes = selected,
                    strict = settings.bool(PrefKeys.SEARCH_STRICT),
                    history = readHistory()
                )
            }
        }
    }

    fun toggleSource(type: Int) {
        _ui.update {
            val next = if (it.selectedTypes.contains(type)) it.selectedTypes - type else it.selectedTypes + type
            it.copy(selectedTypes = next)
        }
        research()
    }

    fun selectAll() {
        _ui.update { it.copy(selectedTypes = it.enabledSources.map { source -> source.type }.toSet()) }
        research()
    }

    fun clearSources() {
        _ui.update { it.copy(selectedTypes = emptySet()) }
        research()
    }

    fun toggleStrict() {
        val next = !_ui.value.strict
        settings.setBool(PrefKeys.SEARCH_STRICT, next)
        _ui.update { it.copy(strict = next) }
        research()
    }

    fun consumeOpen() {
        _ui.update { it.copy(pendingOpen = null) }
    }

    fun search(keyword: String, page: Int = 1) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            _ui.update { it.copy(error = "请输入关键字") }
            return
        }
        searchJob?.cancel()
        if (isUrl(trimmed)) {
            _ui.update {
                it.copy(
                    keyword = trimmed,
                    loading = true,
                    error = null,
                    pendingOpen = null,
                    failedSources = 0,
                    ended = true
                )
            }
            searchJob = scope.launch { openUrl(trimmed) }
            return
        }
        if (page == 1) _items.value = emptyList()
        _ui.update {
            it.copy(
                keyword = trimmed,
                loading = true,
                items = if (page == 1) emptyList() else _items.value,
                page = page,
                ended = false,
                error = null,
                empty = false,
                failedSources = 0,
                pendingOpen = null
            )
        }
        searchJob = scope.launch { runSearch(trimmed, page, append = page > 1) }
    }

    fun searchMore() {
        val state = _ui.value
        if (state.loading || state.ended || state.keyword.isEmpty()) return
        search(state.keyword, state.page + 1)
    }

    private suspend fun openUrl(url: String) {
        val matched = runCatching { manga.matchUrl(url) }.getOrNull()
        if (matched == null) {
            _ui.update { it.copy(loading = false, error = "无法识别该链接") }
            return
        }
        rememberHistory(url)
        _ui.update { it.copy(loading = false, pendingOpen = matched, error = null) }
    }

    private suspend fun runSearch(keyword: String, page: Int, append: Boolean) {
        val types = _ui.value.selectedTypes.toList()
        if (types.isEmpty()) {
            _ui.update { it.copy(loading = false, error = "未选择图源") }
            return
        }
        val collected = LinkedHashMap<String, ComicItem>()
        if (append) {
            _items.value.forEach { collected["${it.source}_${it.cid}"] = it }
        }
        val before = collected.size
        val stats = SearchStats()
        val strict = _ui.value.strict
        try {
            withContext(Dispatchers.Default) {
                var lastPublish = 0L
                var sincePublish = 0
                manga.search(types, keyword, page, strict, stats).collect { comic ->
                    val key = "${comic.source}_${comic.cid}"
                    collected[key] = comic
                    sincePublish++
                    val now = System.currentTimeMillis()
                    if (lastPublish == 0L || sincePublish >= 12 || now - lastPublish >= 300L) {
                        lastPublish = now
                        sincePublish = 0
                        _items.value = collected.values.toList()
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (stats.failed == 0) stats.failed = types.size
        }
        val results = collected.values.toList()
        val failed = stats.failed
        val allFailed = failed >= types.size
        _items.value = results
        if (page == 1 && !(results.isEmpty() && allFailed)) {
            rememberHistory(keyword)
        }
        _ui.update {
            it.copy(
                loading = false,
                items = results,
                page = page,
                ended = !allFailed && collected.size == before,
                empty = collected.isEmpty(),
                failedSources = if (!allFailed && failed > 0) failed else 0,
                error = if (allFailed) "搜索失败，请稍后重试" else null
            )
        }
    }

    private fun research() {
        val keyword = _ui.value.keyword
        if (keyword.isEmpty()) return
        search(keyword)
    }

    private fun readHistory(): List<String> {
        return settings.string(PrefKeys.SEARCH_HISTORY)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(HISTORY_LIMIT)
            .toList()
    }

    private fun rememberHistory(keyword: String) {
        val next = (listOf(keyword) + readHistory().filter { it != keyword }).take(HISTORY_LIMIT)
        settings.setString(PrefKeys.SEARCH_HISTORY, next.joinToString("\n"))
        _ui.update { it.copy(history = next) }
    }

    fun clear() {
        searchJob?.cancel()
        scope.cancel()
    }

    private companion object {
        const val HISTORY_LIMIT = 8

        fun isUrl(text: String): Boolean {
            val lower = text.lowercase()
            return lower.startsWith("http://") || lower.startsWith("https://")
        }
    }
}
