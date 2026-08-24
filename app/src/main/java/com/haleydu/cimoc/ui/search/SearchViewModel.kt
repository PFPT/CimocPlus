package com.haleydu.cimoc.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val mangaService: MangaService,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _sources = MutableSharedFlow<List<Source>>(replay = 1, extraBufferCapacity = 1)
    val sources: SharedFlow<List<Source>> = _sources

    private val _sourceFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sourceFail: SharedFlow<Unit> = _sourceFail

    private val _autoComplete = MutableSharedFlow<List<String>>(extraBufferCapacity = 1)
    val autoComplete: SharedFlow<List<String>> = _autoComplete

    private val _history = MutableStateFlow(readHistory())
    val history: StateFlow<List<String>> = _history

    var strictSearch: Boolean
        get() = preferenceManager.getBoolean(PreferenceManager.PREF_SEARCH_STRICT, true)
        set(value) = preferenceManager.putBoolean(PreferenceManager.PREF_SEARCH_STRICT, value)

    fun loadSource() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { sourceManager.listEnable() }
                _sources.emit(list)
            } catch (e: Exception) {
                _sourceFail.emit(Unit)
            }
        }
    }

    fun loadAutoComplete(keyword: String) {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { mangaService.loadAutoComplete(keyword) }
                _autoComplete.emit(list)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSource(type: Int): Source? = sourceManager.load(type)

    fun addHistory(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        val next = _history.value.toMutableList()
        next.remove(trimmed)
        next.add(0, trimmed)
        while (next.size > HISTORY_LIMIT) {
            next.removeAt(next.lastIndex)
        }
        writeHistory(next)
        _history.value = next
    }

    fun clearHistory() {
        writeHistory(emptyList())
        _history.value = emptyList()
    }

    private fun readHistory(): List<String> {
        val raw = preferenceManager.getString(PreferenceManager.PREF_SEARCH_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeHistory(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        preferenceManager.putString(PreferenceManager.PREF_SEARCH_HISTORY, array.toString())
    }

    companion object {
        private const val HISTORY_LIMIT = 20
    }
}
