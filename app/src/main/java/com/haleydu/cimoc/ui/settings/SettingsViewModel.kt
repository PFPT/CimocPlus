package com.haleydu.cimoc.ui.settings
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.drawee.backends.pipeline.Fresco
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Download
import com.haleydu.cimoc.core.Storage
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.saf.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val comicManager: ComicManager,
    private val taskManager: TaskManager
) : ViewModel() {

    sealed class Event {
        object MoveSuccess : Event()
        object ExecuteSuccess : Event()
        object ExecuteFail : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events

    private val app: App
        get() = context.applicationContext as App

    fun clearCache() {
        Fresco.getImagePipeline().clearDiskCaches()
    }

    fun moveFiles(dst: DocumentFile?) {
        val dest = dst ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Storage.moveRootDir(
                    app.contentResolver,
                    app.documentFile,
                    dest
                ) { msg -> AppEventBus.post(AppEvent(AppEvent.EVENT_DIALOG_PROGRESS, msg)) }
                _events.emit(Event.MoveSuccess)
            } catch (e: Exception) {
                _events.emit(Event.ExecuteFail)
            }
        }
    }

    fun scanTask() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scanned = Download.scan(app.contentResolver, app.documentFile)
                for (pair in scanned) {
                    val existing = comicManager.load(pair.first.source, pair.first.cid)
                    val comic: Comic
                    if (existing == null) {
                        comicManager.insert(pair.first)
                        updateKey(pair.first.id, pair.second)
                        taskManager.insertInTx(pair.second)
                        comic = pair.first
                    } else {
                        existing.download = System.currentTimeMillis()
                        comicManager.update(existing)
                        updateKey(existing.id, pair.second)
                        taskManager.insertIfNotExist(pair.second)
                        comic = existing
                    }
                    AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_INSERT, MiniComic(comic)))
                    AppEventBus.post(AppEvent(AppEvent.EVENT_DIALOG_PROGRESS, comic.title))
                }
                _events.emit(Event.ExecuteSuccess)
            } catch (e: Exception) {
                _events.emit(Event.ExecuteFail)
            }
        }
    }

    private fun updateKey(key: Long?, list: List<Task>) {
        val id = key ?: return
        for (task in list) {
            task.key = id
        }
    }
}
