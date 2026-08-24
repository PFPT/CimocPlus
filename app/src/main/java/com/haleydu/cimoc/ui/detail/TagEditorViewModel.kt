package com.haleydu.cimoc.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.manager.TagManager
import com.haleydu.cimoc.manager.TagRefManager
import com.haleydu.cimoc.misc.Switcher
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.model.TagRef
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TagEditorViewModel @Inject constructor(
    private val tagManager: TagManager,
    private val tagRefManager: TagRefManager
) : ViewModel() {

    private var comicId: Long = -1
    private val tagSet = HashSet<Long>()

    private val _tags = MutableSharedFlow<List<Switcher<Tag>>>(extraBufferCapacity = 1)
    val tags: SharedFlow<List<Switcher<Tag>>> = _tags

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _updateSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updateSuccess: SharedFlow<Unit> = _updateSuccess

    private val _updateFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updateFail: SharedFlow<Unit> = _updateFail

    fun load(id: Long) {
        comicId = id
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val tags = tagManager.list()
                    for (ref in tagRefManager.listByComic(comicId)) {
                        tagSet.add(ref.tid)
                    }
                    val list = ArrayList<Switcher<Tag>>(tags.size)
                    for (tag in tags) {
                        list.add(Switcher(tag, tagSet.contains(tag.id)))
                    }
                    list
                }
                _tags.emit(result)
            } catch (e: Exception) {
                _loadFail.emit(Unit)
            }
        }
    }

    private fun updateInTx(list: List<Long>) {
        tagRefManager.runInTx {
            for (id in list) {
                if (!tagSet.contains(id)) {
                    tagRefManager.insert(TagRef(null, id, comicId))
                }
            }
            tagSet.removeAll(list.toSet())
            for (id in tagSet) {
                tagRefManager.delete(id, comicId)
            }
        }
    }

    fun updateRef(list: List<Long>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    updateInTx(list)
                    tagSet.clear()
                    tagSet.addAll(list)
                }
                _updateSuccess.emit(Unit)
                AppEventBus.post(AppEvent(AppEvent.EVENT_TAG_UPDATE, comicId, list))
            } catch (e: Exception) {
                _updateFail.emit(Unit)
            }
        }
    }
}
