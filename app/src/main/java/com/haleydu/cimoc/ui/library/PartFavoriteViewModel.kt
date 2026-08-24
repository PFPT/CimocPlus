package com.haleydu.cimoc.ui.activity

import androidx.collection.LongSparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.manager.ComicManager
import com.haleydu.cimoc.manager.SourceManager
import com.haleydu.cimoc.manager.TagManager
import com.haleydu.cimoc.manager.TagRefManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.TagRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PartFavoriteViewModel @Inject constructor(
    private val comicManager: ComicManager,
    private val tagRefManager: TagRefManager,
    private val sourceManager: SourceManager
) : ViewModel() {

    private var tagId: Long = -1
    private val savedComic = LongSparseArray<Comic>()

    private val _comics = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)
    val comics: SharedFlow<List<Any>> = _comics

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _titles = MutableSharedFlow<List<String>>(extraBufferCapacity = 1)
    val titles: SharedFlow<List<String>> = _titles

    private val _titleFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val titleFail: SharedFlow<Unit> = _titleFail

    private val _insertSuccess = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)
    val insertSuccess: SharedFlow<List<Any>> = _insertSuccess

    private val _insertFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val insertFail: SharedFlow<Unit> = _insertFail

    fun titleGetter(): SourceManager.TitleGetter = sourceManager.TitleGetter()

    private fun getList(id: Long): List<Comic> {
        return when (id) {
            TagManager.TAG_CONTINUE -> comicManager.listContinue()
            TagManager.TAG_FINISH -> comicManager.listFinish()
            else -> comicManager.listFavoriteByTag(id)
        }
    }

    fun load(id: Long) {
        tagId = id
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { MiniComic.listOf(getList(id)) }
                _comics.emit(list)
            } catch (e: Exception) {
                _loadFail.emit(Unit)
            }
        }
    }

    private fun buildIdList(list: List<Any>): List<Long> {
        val result = ArrayList<Long>(list.size)
        for (item in list) {
            result.add((item as MiniComic).id)
        }
        return result
    }

    fun loadComicTitle(list: List<Any>) {
        viewModelScope.launch {
            try {
                val titles = withContext(Dispatchers.IO) {
                    val result = ArrayList<String>()
                    for (comic in comicManager.listFavoriteNotIn(buildIdList(list))) {
                        savedComic.put(comic.id, comic)
                        result.add(comic.title)
                    }
                    result
                }
                _titles.emit(titles)
            } catch (e: Exception) {
                _titleFail.emit(Unit)
            }
        }
    }

    fun insert(check: BooleanArray?) {
        if (check != null && check.size == savedComic.size()) {
            val rList = ArrayList<TagRef>()
            val cList = ArrayList<Any>()
            for (i in check.indices) {
                if (check[i]) {
                    val comic = MiniComic(savedComic.valueAt(i))
                    rList.add(TagRef(null, tagId, comic.id))
                    cList.add(comic)
                }
            }
            tagRefManager.insertInTx(rList)
            _insertSuccess.tryEmit(cList)
        } else {
            _insertFail.tryEmit(Unit)
        }
        savedComic.clear()
    }

    fun delete(id: Long) {
        tagRefManager.delete(tagId, id)
    }

    fun loadComic(id: Long): Comic = comicManager.load(id)
}
