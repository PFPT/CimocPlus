package com.haleydu.cimoc.ui.library
import android.content.Context
import androidx.collection.LongSparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Download
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.utils.ComicUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val comicManager: ComicManager,
    private val taskManager: TaskManager,
    private val sourceManager: SourceManager
) : ViewModel() {

    private val app = context.applicationContext as App

    private val _comics = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)
    val comics: SharedFlow<List<Any>> = _comics

    private val _deleteSuccess = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val deleteSuccess: SharedFlow<Long> = _deleteSuccess

    private val _tasks = MutableSharedFlow<ArrayList<Task>>(extraBufferCapacity = 1)
    val tasks: SharedFlow<ArrayList<Task>> = _tasks

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _fail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fail: SharedFlow<Unit> = _fail

    fun load() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    MiniComic.listOf(comicManager.listDownloadOrdered())
                }
                _comics.emit(list)
            } catch (e: Exception) {
                _loadFail.emit(Unit)
            }
        }
    }

    fun loadComic(id: Long): Comic = comicManager.load(id)

    fun deleteComic(id: Long) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val comic = comicManager.callInTx<Comic> {
                        val loaded = comicManager.load(id)
                        taskManager.deleteByComicId(id)
                        loaded.download = null
                        comicManager.updateOrDelete(loaded)
                        loaded
                    }
                    Download.delete(
                        app.documentFile,
                        comic,
                        sourceManager.getParser(comic.source).title
                    )
                    id
                }
                _deleteSuccess.emit(result)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }

    fun loadTask() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    val pending = ArrayList<Task>()
                    for (task in taskManager.list()) {
                        if (!task.isFinish) {
                            pending.add(task)
                        }
                    }
                    val array: LongSparseArray<Comic> = ComicUtils.buildComicMap(comicManager.listDownload())
                    for (task in pending) {
                        val comic = array.get(task.key)
                        if (comic != null) {
                            task.source = comic.source
                            task.cid = comic.cid
                        }
                        task.state = Task.STATE_WAIT
                    }
                    pending
                }
                _tasks.emit(list)
            } catch (e: Exception) {
                _fail.emit(Unit)
            }
        }
    }
}
