package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PrefKeys
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RankViewModel(
    private val sources: SourceRepository,
    private val manga: MangaRepository,
    private val settings: SettingsRepository
) {
    data class UiState(
        val items: List<ComicItem> = emptyList(),
        val loading: Boolean = false,
        val opening: Boolean = false,
        val stale: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        val pendingOpen: ComicItem? = null
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var openJob: Job? = null
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        val cached = decode(settings.string(PrefKeys.RANK_CACHE))
        _ui.update {
            it.copy(
                items = cached,
                loading = true,
                stale = false,
                error = null,
                message = null
            )
        }
        scope.launch {
            when (val result = manga.loadRank()) {
                is LoadResult.Ok -> {
                    settings.setString(PrefKeys.RANK_CACHE, encode(result.value))
                    _ui.update {
                        it.copy(items = result.value, loading = false, stale = false, error = null)
                    }
                }
                is LoadResult.Err -> _ui.update {
                    if (it.items.isEmpty()) {
                        it.copy(loading = false, stale = false, error = "热度榜加载失败")
                    } else {
                        it.copy(loading = false, stale = true, error = null)
                    }
                }
            }
        }
    }

    fun open(item: ComicItem) {
        if (_ui.value.opening) return
        openJob?.cancel()
        _ui.update { it.copy(opening = true, message = null, pendingOpen = null) }
        openJob = scope.launch {
            val detail = runCatching { manga.loadDetail(item, recordHealth = false) }.getOrNull()
            if (detail is LoadResult.Ok) {
                _ui.update { it.copy(opening = false, pendingOpen = detail.value.first) }
                return@launch
            }
            val types = runCatching { sources.listEnable() }.getOrDefault(emptyList())
                .filter { !it.invalid && it.type != item.source }
                .map { it.type }
            val found = if (types.isEmpty() || item.title.isBlank()) {
                null
            } else {
                runCatching {
                    manga.search(types, item.title, 1, true, recordHealth = false).firstOrNull()
                }.getOrNull()
            }
            if (found == null) {
                _ui.update { it.copy(opening = false, message = "没有可阅读的图源") }
            } else {
                _ui.update { it.copy(opening = false, pendingOpen = found) }
            }
        }
    }

    fun consumeOpen() {
        _ui.update { it.copy(pendingOpen = null) }
    }

    fun clear() {
        openJob?.cancel()
        scope.cancel()
    }

    private fun encode(items: List<ComicItem>): String {
        return items.joinToString("\n") { comic ->
            listOf(comic.cid, comic.title, comic.cover, comic.author.orEmpty()).joinToString("\u001f")
        }
    }

    private fun decode(text: String): List<ComicItem> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence().mapNotNull { line ->
            val parts = line.split('\u001f')
            val cid = parts.getOrNull(0).orEmpty()
            val title = parts.getOrNull(1).orEmpty()
            if (cid.isEmpty() || title.isEmpty()) return@mapNotNull null
            ComicItem(
                source = TENCENT,
                cid = cid,
                title = title,
                cover = parts.getOrNull(2).orEmpty(),
                author = parts.getOrNull(3)?.takeIf { it.isNotEmpty() }
            )
        }.toList()
    }

    private companion object {
        const val TENCENT = 51
    }
}
