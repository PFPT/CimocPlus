package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.DownloadTaskItem
import com.haleydu.cimoc.shared.repo.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DownloadTasksViewModel(
    private val downloads: DownloadRepository
) {
    data class UiState(
        val items: List<DownloadTaskItem> = emptyList(),
        val loading: Boolean = true,
        val selecting: Boolean = false,
        val selected: Set<Long> = emptySet()
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        scope.launch {
            downloads.observeTasks().collect { list ->
                _ui.update { state ->
                    val ids = list.map { it.id }.toSet()
                    state.copy(
                        items = list,
                        loading = false,
                        selected = state.selected.filter { it in ids }.toSet()
                    )
                }
            }
        }
    }

    fun pause(id: Long) {
        scope.launch { runCatching { downloads.pause(id) } }
    }

    fun retry(id: Long) {
        scope.launch { runCatching { downloads.retry(id) } }
    }

    fun delete(id: Long) {
        scope.launch { runCatching { downloads.delete(id) } }
    }

    fun enterSelect() {
        _ui.update { it.copy(selecting = true, selected = emptySet()) }
    }

    fun exitSelect() {
        _ui.update { it.copy(selecting = false, selected = emptySet()) }
    }

    fun toggleSelect(id: Long) {
        _ui.update {
            val next = if (id in it.selected) it.selected - id else it.selected + id
            it.copy(selected = next)
        }
    }

    fun selectAll() {
        _ui.update { it.copy(selected = it.items.map { task -> task.id }.toSet()) }
    }

    fun clearSelection() {
        _ui.update { it.copy(selected = emptySet()) }
    }

    fun deleteSelected() {
        val ids = _ui.value.selected.toList()
        if (ids.isEmpty()) return
        scope.launch {
            runCatching { downloads.delete(ids) }
            _ui.update { it.copy(selecting = false, selected = emptySet()) }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
