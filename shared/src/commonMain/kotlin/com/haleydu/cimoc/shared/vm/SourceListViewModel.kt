package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.ImportPreview
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
        val loading: Boolean = false,
        val importing: Boolean = false,
        val message: String? = null,
        val preview: ImportPreview? = null,
        val staged: String? = null
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        sources.warm()
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

    fun stage(input: String) {
        val text = input.trim()
        if (text.isEmpty()) {
            _ui.update { it.copy(message = "请输入脚本或地址") }
            return
        }
        _ui.update { it.copy(importing = true, message = null, preview = null, staged = null) }
        scope.launch {
            val result = runCatching { sources.previewImport(text) }
            if (result.isSuccess) {
                _ui.update { it.copy(importing = false, preview = result.getOrThrow(), staged = text) }
            } else {
                _ui.update { it.copy(importing = false, message = "这不像图源脚本，没有保存") }
            }
        }
    }

    fun confirmImport() {
        val text = _ui.value.staged ?: return
        _ui.update { it.copy(importing = true, preview = null) }
        scope.launch {
            val result = runCatching { sources.import(text) }
            if (result.isSuccess) {
                _ui.update { it.copy(importing = false, message = "已引入", staged = null) }
                load()
            } else {
                _ui.update { it.copy(importing = false, message = "引入失败", staged = null) }
            }
        }
    }

    fun dismissPreview() {
        _ui.update { it.copy(preview = null, staged = null) }
    }

    fun remove(item: SourceItem) {
        if (!item.custom) return
        scope.launch {
            val result = runCatching { sources.removeCustom(item.type) }
            if (result.isSuccess) {
                _ui.update { it.copy(message = "已删除") }
                load()
            } else {
                _ui.update { it.copy(message = "删除失败") }
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
