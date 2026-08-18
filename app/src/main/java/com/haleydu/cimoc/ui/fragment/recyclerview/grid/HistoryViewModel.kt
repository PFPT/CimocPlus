package com.haleydu.cimoc.ui.fragment.recyclerview.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.manager.ComicManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val comicManager: ComicManager
) : ViewModel() {

    private val _comics = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)
    val comics: SharedFlow<List<Any>> = _comics

    private val _clearSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val clearSuccess: SharedFlow<Unit> = _clearSuccess

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _fail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fail: SharedFlow<Unit> = _fail

    fun load() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    MiniComic.listOf(comicManager.listHistory())
                }
                _comics.emit(list)
            } catch (e: Exception) {
                _loadFail.emit(Unit)
            }
        }
    }

    fun loadComic(id: Long): Comic = comicManager.load(id)

    fun delete(id: Long) {
        val comic = comicManager.load(id)
        comic.history = null
        comicManager.updateOrDelete(comic)
    }

    fun clear() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    comicManager.runInTx {
                        for (comic in comicManager.listHistory()) {
                            comic.history = null
                            comicManager.updateOrDelete(comic)
                        }
                    }
                }
                _clearSuccess.emit(Unit)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }
}
