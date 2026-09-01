package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.SourceItem
import com.haleydu.cimoc.shared.repo.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceListViewModel(
    private val sources: SourceRepository
) {
    data class UiState(
        val items: List<SourceItem> = emptyList(),
        val loading: Boolean = false
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        _ui.update { it.copy(loading = true) }
        scope.launch {
            val list = runCatching { sources.listAll() }.getOrDefault(emptyList())
            _ui.update { it.copy(items = list, loading = false) }
        }
    }

    fun toggle(item: SourceItem) {
        scope.launch {
            runCatching { sources.setEnabled(item.type, !item.enable) }
            load()
        }
    }

    fun clear() {
        scope.cancel()
    }
}
