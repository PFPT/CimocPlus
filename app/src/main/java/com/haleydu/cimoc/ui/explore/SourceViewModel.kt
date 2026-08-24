package com.haleydu.cimoc.ui.fragment.recyclerview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class SourceViewModel @Inject constructor(
    private val sourceManager: SourceManager
) : ViewModel() {

    private val _sources = MutableSharedFlow<List<Source>>(extraBufferCapacity = 1)
    val sources: SharedFlow<List<Source>> = _sources

    private val _fail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fail: SharedFlow<Unit> = _fail

    fun load() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { sourceManager.list() }
                _sources.emit(list)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }

    fun update(source: Source) {
        sourceManager.update(source)
    }

    fun hasCategory(type: Int): Boolean = sourceManager.getParser(type).category != null
}
