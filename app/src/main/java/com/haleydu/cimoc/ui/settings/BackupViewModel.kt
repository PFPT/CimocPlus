package com.haleydu.cimoc.ui.activity

import android.content.Context
import android.util.Pair
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Backup
import com.haleydu.cimoc.manager.ComicManager
import com.haleydu.cimoc.manager.PreferenceManager
import com.haleydu.cimoc.manager.TagManager
import com.haleydu.cimoc.manager.TagRefManager
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.model.TagRef
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.LinkedList
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val comicManager: ComicManager,
    private val tagManager: TagManager,
    private val tagRefManager: TagRefManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val app = context.applicationContext as App

    sealed class Event {
        data class ComicFiles(val files: Array<String>) : Event()
        data class TagFiles(val files: Array<String>) : Event()
        data class SettingsFiles(val files: Array<String>) : Event()
        data class ClearFiles(val files: Array<String>) : Event()
        object FileLoadFail : Event()
        data class SaveSuccess(val size: Int) : Event()
        object SaveFail : Event()
        object RestoreSuccess : Event()
        object RestoreFail : Event()
        object ClearSuccess : Event()
        object ClearFail : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events

    fun loadComicFile() {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { Backup.loadFavorite(app.documentFile) }
                _events.emit(Event.ComicFiles(file))
            } catch (e: Exception) {
                _events.emit(Event.FileLoadFail)
            }
        }
    }

    fun loadTagFile() {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { Backup.loadTag(app.documentFile) }
                _events.emit(Event.TagFiles(file))
            } catch (e: Exception) {
                _events.emit(Event.FileLoadFail)
            }
        }
    }

    fun loadSettingsFile() {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { Backup.loadSettings(app.documentFile) }
                _events.emit(Event.SettingsFiles(file))
            } catch (e: Exception) {
                _events.emit(Event.FileLoadFail)
            }
        }
    }

    fun loadClearBackupFile() {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { Backup.loadClearBackup(app.documentFile) }
                _events.emit(Event.ClearFiles(file))
            } catch (e: Exception) {
                _events.emit(Event.FileLoadFail)
            }
        }
    }

    fun saveComic() {
        viewModelScope.launch {
            try {
                val size = withContext(Dispatchers.IO) {
                    Backup.saveComic(
                        app.contentResolver,
                        app.documentFile,
                        comicManager.listFavoriteOrHistory()
                    )
                }
                onSaveResult(size)
            } catch (e: Exception) {
                _events.emit(Event.SaveFail)
            }
        }
    }

    fun saveTag() {
        viewModelScope.launch {
            try {
                val size = withContext(Dispatchers.IO) { groupAndSaveComicByTag() }
                onSaveResult(size)
            } catch (e: Exception) {
                _events.emit(Event.SaveFail)
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val size = withContext(Dispatchers.IO) {
                    Backup.saveSetting(
                        app.contentResolver,
                        app.documentFile,
                        preferenceManager.all
                    )
                }
                onSaveResult(size)
            } catch (e: Exception) {
                _events.emit(Event.SaveFail)
            }
        }
    }

    private suspend fun onSaveResult(size: Int) {
        if (size == -1) {
            _events.emit(Event.SaveFail)
        } else {
            _events.emit(Event.SaveSuccess(size))
        }
    }

    fun restoreComic(filename: String?) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val list = Backup.restoreComic(app.contentResolver, app.documentFile, filename)
                    filterAndPostComic(list)
                }
                _events.emit(Event.RestoreSuccess)
            } catch (e: Exception) {
                _events.emit(Event.RestoreFail)
            }
        }
    }

    fun restoreTag(filename: String?) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    updateAndPostTag(Backup.restoreTag(app.contentResolver, app.documentFile, filename))
                }
                _events.emit(Event.RestoreSuccess)
            } catch (e: Exception) {
                _events.emit(Event.RestoreFail)
            }
        }
    }

    fun restoreSetting(filename: String?) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Backup.restoreSetting(app.contentResolver, app.documentFile, filename)
                }
                _events.emit(Event.RestoreSuccess)
            } catch (e: Exception) {
                _events.emit(Event.RestoreFail)
            }
        }
    }

    fun clearBackup() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { Backup.clearBackup(app.documentFile) }
                _events.emit(Event.ClearSuccess)
            } catch (e: Exception) {
                _events.emit(Event.ClearFail)
            }
        }
    }

    private fun setTagsId(list: List<Pair<Tag, List<Comic>>>): List<Tag> {
        val tags = LinkedList<Tag>()
        tagRefManager.runInTx {
            for (pair in list) {
                val tag = tagManager.load(pair.first.title)
                if (tag == null) {
                    tagManager.insert(pair.first)
                    tags.add(pair.first)
                } else {
                    pair.first.id = tag.id
                }
            }
        }
        return tags
    }

    private fun updateAndPostTag(list: List<Pair<Tag, List<Comic>>>) {
        val tags = setTagsId(list)
        for (pair in list) {
            filterAndPostComic(pair.second)
        }
        tagRefManager.runInTx {
            for (pair in list) {
                val tid = pair.first.id
                for (comic in pair.second) {
                    val ref = tagRefManager.load(tid, comic.id)
                    if (ref == null) {
                        tagRefManager.insert(TagRef(null, tid, comic.id))
                    }
                }
            }
        }
        AppEventBus.post(AppEvent(AppEvent.EVENT_TAG_RESTORE, tags))
    }

    private fun groupAndSaveComicByTag(): Int {
        val list = LinkedList<Pair<Tag, List<Comic>>>()
        comicManager.runInTx {
            for (tag in tagManager.list()) {
                val comics = LinkedList<Comic>()
                for (ref in tagRefManager.listByTag(tag.id)) {
                    comics.add(comicManager.load(ref.cid))
                }
                list.add(Pair.create(tag, comics))
            }
        }
        return Backup.saveTag(app.contentResolver, app.documentFile, list)
    }

    private fun filterAndPostComic(list: List<Comic>) {
        val favorite = LinkedList<Comic>()
        val history = LinkedList<Comic>()
        comicManager.runInTx {
            for (comic in list) {
                val temp = comicManager.load(comic.source, comic.cid)
                if (temp == null) {
                    comicManager.insert(comic)
                    if (comic.history != null) {
                        history.add(comic)
                    }
                    if (comic.favorite != null) {
                        favorite.add(comic)
                    }
                } else {
                    if (temp.favorite == null || temp.history == null) {
                        if (temp.favorite == null && comic.favorite != null) {
                            temp.favorite = comic.favorite
                            favorite.add(comic)
                        }
                        if (temp.history == null && comic.history != null) {
                            temp.history = comic.history
                            if (temp.last == null) {
                                temp.last = comic.last
                                temp.page = comic.page
                                temp.chapter = comic.chapter
                            }
                            history.add(comic)
                        }
                        comicManager.update(temp)
                    }
                    comic.id = temp.id
                }
            }
        }
        postComic(favorite, history)
    }

    private fun postComic(favorite: List<Comic>, history: List<Comic>) {
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_FAVORITE_RESTORE, convertToMiniComic(favorite)))
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_HISTORY_RESTORE, convertToMiniComic(history)))
    }

    private fun convertToMiniComic(list: List<Comic>): List<MiniComic> {
        val result = ArrayList<MiniComic>(list.size)
        for (comic in list) {
            result.add(MiniComic(comic))
        }
        return result
    }
}
