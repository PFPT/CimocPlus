package com.haleydu.cimoc.ui.search
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.Comic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val mangaService: MangaService,
    private val sourceHealthManager: SourceHealthManager
) : ViewModel() {

    data class SearchGroup(
        val key: String,
        val comics: List<Comic>
    ) {
        val primary: Comic get() = comics.first()
    }

    data class SearchState(
        val items: List<SearchGroup> = emptyList(),
        val filters: List<Int> = emptyList()
    )

    sealed class SearchIntent {
        object LoadMore : SearchIntent()
        data class Filter(val source: Int) : SearchIntent()
    }

    fun dispatch(intent: SearchIntent) {
        when (intent) {
            SearchIntent.LoadMore -> loadSearch()
            is SearchIntent.Filter -> setFilter(intent.source)
        }
    }

    private val _items = MutableStateFlow<List<SearchGroup>>(emptyList())
    val items: StateFlow<List<SearchGroup>> = _items

    private val _filterSources = MutableStateFlow<List<Int>>(emptyList())
    val filterSources: StateFlow<List<Int>> = _filterSources

    private val _searchError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val searchError: SharedFlow<Unit> = _searchError

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _loadNetworkError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadNetworkError: SharedFlow<Unit> = _loadNetworkError

    private val _loadEmpty = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadEmpty: SharedFlow<Unit> = _loadEmpty

    private val groups = LinkedHashMap<String, MutableList<Comic>>()
    private val sourceTypes = LinkedHashSet<Int>()
    private var filter = FILTER_ALL
    private var keyword: String = ""
    private var keywordTemp: String? = null
    private var strictSearch: Boolean = true
    private var comicTitleTemp = ""
    private var error = 0
    private var stateArray: Array<State> = emptyArray()
    private var searchJob: Job? = null

    fun titleGetter(): SourceManager.TitleGetter = sourceManager.TitleGetter()

    fun headerGetter(): SourceManager.HeaderGetter = sourceManager.HeaderGetter()

    fun setup(source: IntArray?, keyword: String?, strictSearch: Boolean) {
        searchJob?.cancel()
        searchJob = null
        groups.clear()
        sourceTypes.clear()
        filter = FILTER_ALL
        error = 0
        _items.value = emptyList()
        _filterSources.value = emptyList()
        this.keyword = keyword ?: ""
        this.strictSearch = strictSearch
        if (source == null || source.isEmpty()) {
            initStateArray(loadSource())
        } else {
            initStateArray(source)
        }
    }

    fun setFilter(source: Int) {
        if (filter == source) return
        filter = source
        publish()
    }

    private fun initStateArray(source: IntArray) {
        val sorted = sourceHealthManager.sortTypes(source.toList())
        stateArray = Array(sorted.size) { i ->
            State(source = sorted[i], page = 0, state = STATE_NULL)
        }
    }

    private fun loadSource(): IntArray {
        val list = sourceHealthManager.sortSources(sourceManager.listEnable())
        return IntArray(list.size) { list[it].type }
    }

    fun isInvalid(source: Int): Boolean = sourceHealthManager.isInvalid(source)

    fun loadCategory() {
        if (stateArray.isEmpty()) return
        val first = stateArray[0]
        if (first.state != STATE_NULL) return
        val parser = sourceManager.getParser(first.source)
        first.state = STATE_DOING
        if (first.page == 0) {
            if (parser.title == "扑飞漫画") {
                keywordTemp = keyword
                keyword = keyword.replace("_%d", "")
            }
        } else {
            if (parser.title == "扑飞漫画") {
                keyword = keywordTemp ?: keyword
            }
        }
        viewModelScope.launch {
            val start = android.os.SystemClock.elapsedRealtime()
            try {
                val list = withContext(Dispatchers.IO) {
                    ArrayList(mangaService.getCategoryComic(parser, keyword, ++first.page))
                }
                if (comicTitleTemp.isNotEmpty() && list.isNotEmpty() && comicTitleTemp == list[0].title) {
                    list.clear()
                }
                if (list.isNotEmpty()) {
                    comicTitleTemp = list[0].title
                }
                if (list.isEmpty()) {
                    if (first.page == 1) {
                        sourceHealthManager.recordFailure(first.source, SourceHealthManager.KIND_EMPTY)
                        _loadEmpty.emit(Unit)
                    }
                } else {
                    sourceHealthManager.recordSuccess(
                        first.source,
                        android.os.SystemClock.elapsedRealtime() - start
                    )
                    addComics(list)
                }
                first.state = STATE_NULL
            } catch (e: CancellationException) {
                throw e
            } catch (e: Manga.NetworkErrorException) {
                e.printStackTrace()
                sourceHealthManager.recordFailure(first.source, SourceHealthManager.KIND_NETWORK)
                if (first.page == 1) {
                    _loadNetworkError.emit(Unit)
                }
                first.state = STATE_NULL
            } catch (e: Exception) {
                e.printStackTrace()
                sourceHealthManager.recordFailure(first.source, SourceHealthManager.KIND_PARSE)
                if (first.page == 1) {
                    _loadFail.emit(Unit)
                }
                first.state = STATE_NULL
            }
        }
    }

    fun loadSearch() {
        if (searchJob?.isActive == true) {
            return
        }
        if (stateArray.isEmpty()) {
            _searchError.tryEmit(Unit)
            return
        }
        val pending = stateArray.filter { it.state == STATE_NULL }
        if (pending.isEmpty()) return
        pending.forEach { it.state = STATE_DOING }
        searchJob = viewModelScope.launch {
            try {
                pending.map { obj ->
                    flow<Comic> {
                        val parser = sourceManager.getParser(obj.source)
                        val start = android.os.SystemClock.elapsedRealtime()
                        var emitted = false
                        try {
                            val page = obj.page + 1
                            withTimeout(SOURCE_TIMEOUT_MS) {
                                mangaService.getSearchResult(parser, keyword, page, strictSearch)
                                    .collect { comic ->
                                        emitted = true
                                        emit(comic)
                                    }
                            }
                            obj.page = page
                            sourceHealthManager.recordSuccess(
                                obj.source,
                                android.os.SystemClock.elapsedRealtime() - start
                            )
                            obj.state = if (page == 1 && !emitted) STATE_DONE else STATE_NULL
                        } catch (e: TimeoutCancellationException) {
                            sourceHealthManager.recordFailure(obj.source, SourceHealthManager.KIND_NETWORK)
                            if (obj.page == 0) {
                                obj.state = STATE_DONE
                                error++
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Manga.NetworkErrorException) {
                            e.printStackTrace()
                            sourceHealthManager.recordFailure(obj.source, SourceHealthManager.KIND_NETWORK)
                            if (obj.page == 0) {
                                obj.state = STATE_DONE
                                error++
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            sourceHealthManager.recordFailure(obj.source, SourceHealthManager.KIND_PARSE)
                            if (obj.page == 0) {
                                obj.state = STATE_DONE
                                error++
                            }
                        }
                    }
                }.merge().collect { comic ->
                    addComic(comic)
                }
                pending.forEach { obj ->
                    if (obj.state == STATE_DOING) obj.state = STATE_NULL
                }
                if (groups.isEmpty()) {
                    _searchError.emit(Unit)
                }
            } catch (e: CancellationException) {
                pending.forEach { obj ->
                    if (obj.state == STATE_DOING) obj.state = STATE_NULL
                }
                throw e
            }
        }
    }

    private fun addComic(comic: Comic) {
        addComics(listOf(comic))
    }

    private fun addComics(list: List<Comic>) {
        var changed = false
        for (comic in list) {
            val key = normalize(comic.title ?: "")
            if (key.isEmpty()) continue
            val bucket = groups.getOrPut(key) { mutableListOf() }
            if (bucket.none { it.source == comic.source && it.cid == comic.cid }) {
                bucket.add(comic)
                sourceTypes.add(comic.source)
                changed = true
            }
        }
        if (changed) publish()
    }

    private fun publish() {
        val snapshot = groups.map { (key, comics) -> SearchGroup(key, comics.toList()) }
        _items.value = if (filter == FILTER_ALL) {
            snapshot
        } else {
            snapshot.filter { group -> group.comics.any { it.source == filter } }
        }
        _filterSources.value = sourceHealthManager.sortTypes(sourceTypes)
    }

    private class State(var source: Int, var page: Int, var state: Int)

    companion object {
        const val FILTER_ALL = -1
        private const val STATE_NULL = 0
        private const val STATE_DOING = 1
        private const val STATE_DONE = 3
        private const val SOURCE_TIMEOUT_MS = 30000L

        fun normalize(title: String): String {
            return title.lowercase()
                .replace(Regex("[\\s　]+"), "")
                .replace(Regex("[\\p{Punct}~～!！？?·・]"), "")
        }
    }
}
