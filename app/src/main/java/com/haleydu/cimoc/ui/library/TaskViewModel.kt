package com.haleydu.cimoc.ui.library
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Download
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val taskManager: TaskManager,
    private val comicManager: ComicManager,
    private val sourceManager: SourceManager
) : ViewModel() {

    private val app = context.applicationContext as App
    var comic: Comic? = null
        private set

    fun enabledSourceTypes(): IntArray {
        val list = sourceManager.listEnable()
        return IntArray(list.size) { list[it].type }
    }

    data class LoadResult(val list: List<Task>, val isLocal: Boolean)

    private val _loadSuccess = MutableSharedFlow<LoadResult>(extraBufferCapacity = 1)
    val loadSuccess: SharedFlow<LoadResult> = _loadSuccess

    private val _loadFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFail: SharedFlow<Unit> = _loadFail

    private val _deleteSuccess = MutableSharedFlow<List<Long>>(extraBufferCapacity = 1)
    val deleteSuccess: SharedFlow<List<Long>> = _deleteSuccess

    private val _deleteFail = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteFail: SharedFlow<Unit> = _deleteFail

    private fun updateTaskList(list: List<Task>) {
        val current = comic ?: return
        for (task in list) {
            val state = if (task.isFinish) Task.STATE_FINISH else Task.STATE_PAUSE
            task.cid = current.cid
            task.source = current.source
            task.state = state
        }
    }

    fun load(id: Long, asc: Boolean) {
        comic = comicManager.load(id)
        viewModelScope.launch {
            try {
                val current = comic ?: throw IllegalStateException()
                val list = withContext(Dispatchers.IO) {
                    val tasks = taskManager.list(id)
                    updateTaskList(tasks)
                    if (!current.local) {
                        val sList = Download.getComicIndex(
                            app.contentResolver,
                            app.documentFile,
                            current,
                            sourceManager.getParser(current.source).title
                        )
                        if (sList != null) {
                            tasks.sortWith { lhs, rhs ->
                                if (asc) sList.indexOf(rhs.path) - sList.indexOf(lhs.path)
                                else sList.indexOf(lhs.path) - sList.indexOf(rhs.path)
                            }
                        }
                    } else {
                        tasks.sortWith { lhs, rhs ->
                            if (asc) lhs.title.compareTo(rhs.title)
                            else rhs.title.compareTo(lhs.title)
                        }
                    }
                    tasks
                }
                _loadSuccess.emit(LoadResult(list, current.local))
            } catch (e: Exception) {
                _loadFail.emit(Unit)
            }
        }
    }

    fun deleteTask(list: List<Chapter>, isEmpty: Boolean) {
        val current = comic ?: return
        val id = current.id
        viewModelScope.launch {
            try {
                val ids = withContext(Dispatchers.IO) {
                    deleteFromDatabase(list, isEmpty)
                    if (!current.local) {
                        if (isEmpty) {
                            Download.delete(
                                app.documentFile,
                                current,
                                sourceManager.getParser(current.source).title
                            )
                        } else {
                            Download.delete(
                                app.documentFile,
                                current,
                                list,
                                sourceManager.getParser(current.source).title
                            )
                        }
                    }
                    val result = ArrayList<Long>(list.size)
                    for (chapter in list) {
                        result.add(chapter.tid)
                    }
                    result
                }
                if (isEmpty) {
                    AppEventBus.post(AppEvent(AppEvent.EVENT_DOWNLOAD_REMOVE, id))
                }
                _deleteSuccess.emit(ids)
            } catch (e: Exception) {
                _deleteFail.emit(Unit)
            }
        }
    }

    private fun deleteFromDatabase(list: List<Chapter>, isEmpty: Boolean) {
        val current = comic ?: return
        comicManager.runInTx {
            for (chapter in list) {
                taskManager.delete(chapter.tid)
            }
            if (isEmpty) {
                current.download = null
                comicManager.updateOrDelete(current)
                Download.delete(
                    app.documentFile,
                    current,
                    sourceManager.getParser(current.source).title
                )
            }
        }
    }

    fun updateLast(path: String): Long {
        val current = comic ?: return -1
        if (current.favorite != null) {
            current.favorite = System.currentTimeMillis()
        }
        current.history = System.currentTimeMillis()
        if (path != current.last) {
            current.last = path
            current.page = 1
        }
        comicManager.update(current)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_READ, MiniComic(current), false))
        return current.id
    }

    fun refreshLast() {
        val current = comic ?: return
        val loaded = comicManager.load(current.id) ?: return
        current.page = loaded.page
        current.last = loaded.last
    }
}
