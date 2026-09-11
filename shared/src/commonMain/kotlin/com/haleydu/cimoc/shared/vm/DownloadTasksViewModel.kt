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
        val loading: Boolean = true
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        scope.launch {
            downloads.observeTasks().collect { list ->
                _ui.update { it.copy(items = list, loading = false) }
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

    fun clear() {
        scope.cancel()
    }
}
