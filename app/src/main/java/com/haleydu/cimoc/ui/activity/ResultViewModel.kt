package com.haleydu.cimoc.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.manager.SourceManager
import com.haleydu.cimoc.model.Comic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val mangaService: MangaService
) : ViewModel() {

    private val _searchSuccess = MutableSharedFlow<Comic>(extraBufferCapacity = 8)
    val searchSuccess: SharedFlow<Comic> = _searchSuccess

    private val _searchError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val searchError: SharedFlow<Unit> = _searchError

    private val _loadSuccess = MutableSharedFlow<List<Comic>>(extraBufferCapacity = 1)
    val loadSuccess: SharedFlow<List<Comic>> = _loadSuccess

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private var keyword: String = ""
    private var keywordTemp: String? = null
    private var strictSearch: Boolean = true
    private var comicTitleTemp = ""
    private var error = 0
    private var stateArray: Array<State> = emptyArray()

    fun titleGetter(): SourceManager.TitleGetter = sourceManager.TitleGetter()

    fun headerGetter(): SourceManager.HeaderGetter = sourceManager.HeaderGetter()

    fun setup(source: IntArray?, keyword: String?, strictSearch: Boolean) {
        this.keyword = keyword ?: ""
        this.strictSearch = strictSearch
        if (source != null) {
            initStateArray(source)
        } else {
            initStateArray(loadSource())
        }
    }

    private fun initStateArray(source: IntArray) {
        stateArray = Array(source.size) { i ->
            State(source = source[i], page = 0, state = STATE_NULL)
        }
    }

    private fun loadSource(): IntArray {
        val list = sourceManager.listEnable()
        return IntArray(list.size) { list[it].type }
    }

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
                _loadSuccess.emit(list)
                first.state = STATE_NULL
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                if (first.page == 1) {
                    _loadFail.emit(Unit)
                }
            }
        }
    }

    fun loadSearch() {
        if (stateArray.isEmpty()) {
            _searchError.tryEmit(Unit)
            return
        }
        for (obj in stateArray) {
            if (obj.state == STATE_NULL) {
                val parser = sourceManager.getParser(obj.source)
                obj.state = STATE_DOING
                viewModelScope.launch {
                    try {
                        mangaService.getSearchResult(parser, keyword, ++obj.page, strictSearch).collect { comic ->
                            _searchSuccess.emit(comic)
                        }
                        obj.state = STATE_NULL
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (obj.page == 1) {
                            obj.state = STATE_DONE
                            if (++error == stateArray.size) {
                                _searchError.emit(Unit)
                            }
                        }
                    }
                }
            }
        }
    }

    private class State(var source: Int, var page: Int, var state: Int)

    companion object {
        private const val STATE_NULL = 0
        private const val STATE_DOING = 1
        private const val STATE_DONE = 3
    }
}
