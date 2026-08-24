package com.haleydu.cimoc.ui.explore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.SourceRuleManager
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.script.JsConsole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SourceViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val sourceRuleManager: SourceRuleManager,
    private val sourceHealthManager: SourceHealthManager,
    private val jsConsole: JsConsole
) : ViewModel() {

    private val _sources = MutableSharedFlow<List<Source>>(extraBufferCapacity = 1)
    val sources: SharedFlow<List<Source>> = _sources

    private val _invalidTypes = MutableStateFlow<Set<Int>>(emptySet())
    val invalidTypes: StateFlow<Set<Int>> = _invalidTypes

    private val _fail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fail: SharedFlow<Unit> = _fail

    private val _importSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val importSuccess: SharedFlow<Unit> = _importSuccess

    private val _importFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val importFail: SharedFlow<Unit> = _importFail

    fun load() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { sourceManager.list() }
                _invalidTypes.value = sourceHealthManager.invalidTypes()
                _sources.emit(list)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }

    fun update(source: Source) {
        sourceManager.update(source)
    }

    fun importScript(input: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { sourceRuleManager.import(input) }
                _importSuccess.emit(Unit)
                val list = withContext(Dispatchers.IO) { sourceManager.list() }
                _invalidTypes.value = sourceHealthManager.invalidTypes()
                _sources.emit(list)
            } catch (e: Exception) {
                _importFail.emit(Unit)
            }
        }
    }

    fun logs(): String = jsConsole.dump()

    fun hasCategory(type: Int): Boolean = sourceManager.getParser(type).category != null
}
