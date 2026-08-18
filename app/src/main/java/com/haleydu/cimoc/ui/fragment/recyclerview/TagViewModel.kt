package com.haleydu.cimoc.ui.fragment.recyclerview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.manager.TagManager
import com.haleydu.cimoc.manager.TagRefManager
import com.haleydu.cimoc.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagManager: TagManager,
    private val tagRefManager: TagRefManager
) : ViewModel() {

    private val _tags = MutableSharedFlow<List<Tag>>(extraBufferCapacity = 1)
    val tags: SharedFlow<List<Tag>> = _tags

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _deleteSuccess = MutableSharedFlow<Tag>(extraBufferCapacity = 1)
    val deleteSuccess: SharedFlow<Tag> = _deleteSuccess

    private val _deleteFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteFail: SharedFlow<Unit> = _deleteFail

    fun load() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { tagManager.list() }
                _tags.emit(list)
            } catch (e: Exception) {
                _loadFail.emit(Unit)
            }
        }
    }

    fun insert(tag: Tag) {
        tagManager.insert(tag)
    }

    fun delete(tag: Tag) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    tagRefManager.deleteByTag(tag.id)
                    tagManager.delete(tag)
                }
                _deleteSuccess.emit(tag)
            } catch (e: Exception) {
                _deleteFail.emit(Unit)
            }
        }
    }
}
