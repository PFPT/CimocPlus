package com.haleydu.cimoc.ui.explore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceCatalogRefresher
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category
import com.haleydu.cimoc.parser.Parser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val mangaService: MangaService,
    private val preferenceManager: PreferenceManager,
    private val sourceHealthManager: SourceHealthManager,
    private val sourceCatalogRefresher: SourceCatalogRefresher
) : ViewModel() {

    enum class Error {
        NETWORK, PARSE, EMPTY
    }

    private val _sources = MutableStateFlow<List<Source>>(emptyList())
    val sources: StateFlow<List<Source>> = _sources

    private val _currentType = MutableStateFlow(-1)
    val currentType: StateFlow<Int> = _currentType

    private val _comics = MutableStateFlow<List<Any>>(emptyList())
    val comics: StateFlow<List<Any>> = _comics

    private val _unsupported = MutableStateFlow(false)
    val unsupported: StateFlow<Boolean> = _unsupported

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableSharedFlow<Error>(extraBufferCapacity = 1)
    val error: SharedFlow<Error> = _error

    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    private var page = 0
    private var format: String? = null
    private var job: Job? = null

    fun titleGetter(): SourceManager.TitleGetter = sourceManager.TitleGetter()

    fun headerGetter(): SourceManager.HeaderGetter = sourceManager.HeaderGetter()

    fun parser(): Parser? {
        val type = _currentType.value
        if (type < 0) return null
        return sourceManager.getParser(type)
    }

    fun category(): Category? = parser()?.category

    fun setup(initialType: Int) {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                sourceHealthManager.sortSources(sourceManager.listEnable())
            }
            val saved = preferenceManager.getInt(PreferenceManager.PREF_EXPLORE_SOURCE, -1)
            val type = when {
                initialType >= 0 && list.any { it.type == initialType } -> initialType
                list.any { it.type == saved } -> saved
                else -> list.firstOrNull()?.type ?: -1
            }
            selectSource(type)
            _sources.value = list
        }
    }

    fun selectSource(type: Int) {
        job?.cancel()
        _currentType.value = type
        page = 0
        _ended.value = false
        format = null
        _comics.value = emptyList()
        if (type >= 0) {
            preferenceManager.putInt(PreferenceManager.PREF_EXPLORE_SOURCE, type)
        }
        val category = if (type < 0) null else sourceManager.getParser(type).category
        _unsupported.value = category == null
        _loading.value = false
    }

    fun applyFilters(args: Array<String?>) {
        val type = _currentType.value
        if (type < 0) return
        val category = sourceManager.getParser(type).category
        if (category == null) {
            _unsupported.value = true
            _comics.value = emptyList()
            _loading.value = false
            _ended.value = false
            return
        }
        _unsupported.value = false
        format = category.getFormat(*args)
        page = 0
        _ended.value = false
        _comics.value = emptyList()
        load(reset = true)
    }

    fun loadMore() {
        if (_ended.value || _unsupported.value || _loading.value || format == null) return
        if (job?.isActive == true) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val fmt = format ?: return
        val type = _currentType.value
        if (type < 0) return
        job?.cancel()
        job = viewModelScope.launch {
            val current = coroutineContext[Job]
            _loading.value = true
            val start = android.os.SystemClock.elapsedRealtime()
            try {
                val nextPage = page + 1
                val list = withContext(Dispatchers.IO) {
                    mangaService.getCategoryComic(sourceManager.getParser(type), fmt, nextPage)
                }
                if (list.isEmpty()) {
                    _ended.value = true
                    if (reset) {
                        noteFailure(type, SourceHealthManager.KIND_EMPTY)
                        _error.emit(Error.EMPTY)
                    }
                } else {
                    sourceHealthManager.recordSuccess(
                        type,
                        android.os.SystemClock.elapsedRealtime() - start
                    )
                    page = nextPage
                    val mapped = MiniComic.listOf(list)
                    _comics.value = if (reset) mapped else _comics.value + mapped
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Manga.NetworkErrorException) {
                noteFailure(type, SourceHealthManager.KIND_NETWORK)
                if (reset) {
                    _error.emit(Error.NETWORK)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                noteFailure(type, SourceHealthManager.KIND_PARSE)
                if (reset) {
                    _error.emit(Error.PARSE)
                }
            } finally {
                if (job === current) {
                    _loading.value = false
                }
            }
        }
    }

    private fun noteFailure(type: Int, kind: String) {
        sourceHealthManager.recordFailure(type, kind)
        viewModelScope.launch(Dispatchers.IO) {
            sourceCatalogRefresher.refreshAfterFailure(type)
        }
    }
}
