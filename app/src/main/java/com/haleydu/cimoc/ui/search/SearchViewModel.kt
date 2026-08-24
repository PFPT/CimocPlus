package com.haleydu.cimoc.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.manager.SourceManager
import com.haleydu.cimoc.model.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val mangaService: MangaService
) : ViewModel() {

    private val _sources = MutableSharedFlow<List<Source>>(extraBufferCapacity = 1)
    val sources: SharedFlow<List<Source>> = _sources

    private val _sourceFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sourceFail: SharedFlow<Unit> = _sourceFail

    private val _autoComplete = MutableSharedFlow<List<String>>(extraBufferCapacity = 1)
    val autoComplete: SharedFlow<List<String>> = _autoComplete

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
}
