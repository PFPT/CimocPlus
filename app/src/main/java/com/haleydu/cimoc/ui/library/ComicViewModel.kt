package com.haleydu.cimoc.ui.library
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.data.TagManager
import com.haleydu.cimoc.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ComicViewModel @Inject constructor(
    private val tagManager: TagManager
) : ViewModel() {

    private val _tags = MutableSharedFlow<List<Tag>>(extraBufferCapacity = 1)
    val tags: SharedFlow<List<Tag>> = _tags

    private val _fail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fail: SharedFlow<Unit> = _fail

    fun loadTag() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { tagManager.list() }
                _tags.emit(list)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }
}
