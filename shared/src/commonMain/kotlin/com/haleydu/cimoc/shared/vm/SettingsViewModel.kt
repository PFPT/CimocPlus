package com.haleydu.cimoc.shared.vm

import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PrefKeys
import com.haleydu.cimoc.shared.model.ReaderMode
import com.haleydu.cimoc.shared.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository
) {
    data class UiState(
        val night: Boolean = false,
        val readerMode: Int = ReaderMode.STREAM,
        val volumeKeys: Boolean = false,
        val preload: Boolean = false,
        val auto: Boolean = false,
        val autoInterval: Int = 4,
        val whiteEdge: Boolean = false,
        val keepBright: Boolean = true,
        val hideNav: Boolean = true,
        val reverse: Boolean = false,
        val vertical: Boolean = false,
        val invert: Boolean = false,
        val gray: Boolean = false,
        val message: String? = null,
        val backupFiles: List<String> = emptyList()
    )

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        _ui.update {
            it.copy(
                night = settings.bool(PrefKeys.NIGHT),
                readerMode = settings.int(PrefKeys.READER_MODE, ReaderMode.STREAM),
                volumeKeys = settings.bool(PrefKeys.READER_VOLUME),
                preload = settings.bool(PrefKeys.READER_PRELOAD),
                auto = settings.bool(PrefKeys.READER_AUTO),
                autoInterval = settings.int(PrefKeys.READER_AUTO_INTERVAL, 4).coerceIn(1, 15),
                whiteEdge = settings.bool(PrefKeys.READER_WHITE_EDGE),
                keepBright = settings.bool(PrefKeys.READER_KEEP_BRIGHT, true),
                hideNav = settings.bool(PrefKeys.READER_HIDE_NAV, true),
                reverse = settings.bool(PrefKeys.READER_REVERSE),
                vertical = settings.bool(PrefKeys.READER_VERTICAL),
                invert = settings.bool(PrefKeys.READER_COLOR_INVERT),
                gray = settings.bool(PrefKeys.READER_COLOR_GRAY),
                message = null
            )
        }
        scope.launch {
            val files = runCatching { settings.listBackupFiles() }.getOrDefault(emptyList())
            _ui.update { it.copy(backupFiles = files) }
        }
    }

    fun setNight(value: Boolean) {
        settings.setBool(PrefKeys.NIGHT, value)
        _ui.update { it.copy(night = value) }
    }

    fun setReaderMode(mode: Int) {
        settings.setInt(PrefKeys.READER_MODE, mode)
        _ui.update { it.copy(readerMode = mode) }
    }

    fun setVolumeKeys(value: Boolean) {
        settings.setBool(PrefKeys.READER_VOLUME, value)
        _ui.update { it.copy(volumeKeys = value) }
    }

    fun setPreload(value: Boolean) {
        settings.setBool(PrefKeys.READER_PRELOAD, value)
        _ui.update { it.copy(preload = value) }
    }

    fun setAuto(value: Boolean) {
        settings.setBool(PrefKeys.READER_AUTO, value)
        _ui.update { it.copy(auto = value) }
    }

    fun setAutoInterval(value: Int) {
        val interval = value.coerceIn(1, 15)
        settings.setInt(PrefKeys.READER_AUTO_INTERVAL, interval)
        _ui.update { it.copy(autoInterval = interval) }
    }

    fun setWhiteEdge(value: Boolean) {
        settings.setBool(PrefKeys.READER_WHITE_EDGE, value)
        _ui.update { it.copy(whiteEdge = value) }
    }

    fun setKeepBright(value: Boolean) {
        settings.setBool(PrefKeys.READER_KEEP_BRIGHT, value)
        _ui.update { it.copy(keepBright = value) }
    }

    fun setHideNav(value: Boolean) {
        settings.setBool(PrefKeys.READER_HIDE_NAV, value)
        _ui.update { it.copy(hideNav = value) }
    }

    fun setReverse(value: Boolean) {
        settings.setBool(PrefKeys.READER_REVERSE, value)
        _ui.update { it.copy(reverse = value) }
    }

    fun setVertical(value: Boolean) {
        settings.setBool(PrefKeys.READER_VERTICAL, value)
        _ui.update { it.copy(vertical = value) }
    }

    fun setInvert(value: Boolean) {
        settings.setBool(PrefKeys.READER_COLOR_INVERT, value)
        _ui.update { it.copy(invert = value) }
    }

    fun setGray(value: Boolean) {
        settings.setBool(PrefKeys.READER_COLOR_GRAY, value)
        _ui.update { it.copy(gray = value) }
    }

    fun backup() {
        scope.launch {
            when (val result = settings.backupFavorites()) {
                is LoadResult.Ok -> {
                    val files = runCatching { settings.listBackupFiles() }.getOrDefault(emptyList())
                    _ui.update { it.copy(message = "已备份 ${result.value} 部", backupFiles = files) }
                }
                is LoadResult.Err -> _ui.update { it.copy(message = result.message) }
            }
        }
    }

    fun restore(name: String) {
        scope.launch {
            when (val result = settings.restoreFavorites(name)) {
                is LoadResult.Ok -> _ui.update { it.copy(message = "恢复成功") }
                is LoadResult.Err -> _ui.update { it.copy(message = result.message) }
            }
        }
    }

    fun consumeMessage() {
        _ui.update { it.copy(message = null) }
    }
}
