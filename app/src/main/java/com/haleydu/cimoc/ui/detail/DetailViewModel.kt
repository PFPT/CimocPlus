package com.haleydu.cimoc.ui.detail
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Backup
import com.haleydu.cimoc.core.Download
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.TagRefManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.utils.interpretationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val comicManager: ComicManager,
    private val chapterManager: ChapterManager,
    private val taskManager: TaskManager,
    private val tagRefManager: TagRefManager,
    private val sourceManager: SourceManager,
    private val mangaService: MangaService,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val app = context.applicationContext as App
    var comic: Comic? = null
        private set

    data class UiState(
        val comic: Comic? = null,
        val chapters: List<Chapter> = emptyList(),
        val last: String? = null,
        val chaptersReady: Boolean = false
    )

    sealed class Event {
        object ParseError : Event()
        object NetworkError : Event()
        object NetworkLoadSuccess : Event()
        data class TaskAddSuccess(val list: ArrayList<Task>) : Event()
        object TaskAddFail : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events

    private var rawChapters: List<Chapter> = emptyList()
    private var userReversed = false

    val isAscend: Boolean
        get() = userReversed

    fun parserHeader(): Headers? {
        val current = comic ?: return null
        return sourceManager.getParser(current.source).header
    }

    fun enabledSourceTypes(): IntArray {
        val list = sourceManager.listEnable()
        return IntArray(list.size) { list[it].type }
    }

    fun load(
        id: Long,
        source: Int,
        cid: String?,
        title: String? = null,
        cover: String? = null,
        author: String? = null
    ) {
        comic = if (id == -1L) {
            comicManager.loadOrCreate(source, cid)
        } else {
            comicManager.load(id)
        }
        val current = comic ?: return
        if (!title.isNullOrEmpty() && current.title.isNullOrEmpty()) {
            current.title = title
        }
        if (!cover.isNullOrEmpty() && current.cover.isNullOrEmpty()) {
            current.cover = cover
        }
        if (!author.isNullOrEmpty() && current.author.isNullOrEmpty()) {
            current.author = author
        }
        userReversed = preferenceManager.getBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, false)
        publishComic(current)
        cancelHighlight()
        preLoad()
        loadChapters()
    }

    fun toggleReverse() {
        userReversed = !userReversed
        preferenceManager.putBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, userReversed)
        _uiState.update { it.copy(chapters = displayedChapters()) }
    }

    private fun displayedChapters(raw: List<Chapter> = rawChapters): List<Chapter> {
        if (raw.size < 2) return raw
        return if (isAscending(raw) == userReversed) raw else raw.reversed()
    }

    private fun isAscending(list: List<Chapter>): Boolean {
        val first = chapterNumber(list.first().title)
        val last = chapterNumber(list.last().title)
        if (first != null && last != null && first != last) {
            return first < last
        }
        val current = comic ?: return false
        return interpretationUtils.isReverseOrder(current)
    }

    private fun chapterNumber(title: String?): Int? {
        if (title.isNullOrEmpty()) return null
        return Regex("""(\d+)""").find(title)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun publishComic(current: Comic) {
        _uiState.update {
            it.copy(comic = current, last = current.last)
        }
    }

    private fun publishChapters(raw: List<Chapter>, ready: Boolean = true) {
        rawChapters = raw.toList()
        val current = comic
        _uiState.update {
            it.copy(
                comic = current,
                chapters = displayedChapters(rawChapters),
                last = current?.last,
                chaptersReady = ready || raw.isNotEmpty()
            )
        }
    }

    private fun updateChapterList(list: List<Chapter>) {
        val current = comic ?: return
        val map = HashMap<String, Task>()
        for (task in taskManager.list(current.id)) {
            map[task.path] = task
        }
        if (map.isNotEmpty()) {
            for (chapter in list) {
                val task = map[chapter.path]
                if (task != null) {
                    chapter.download = true
                    chapter.count = task.progress
                    chapter.complete = task.isFinish
                    chapterManager.update(chapter)
                }
            }
        }
    }

    private fun preLoad() {
        val current = comic ?: return
        if (current.id == null) {
            return
        }
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    val chapters = chapterManager.getListChapter(
                        (current.source.toString() + "000" + current.id).toLong()
                    )
                    if (current.id != null && chapters.isNotEmpty()) {
                        updateChapterList(chapters)
                    }
                    chapters
                }
                if (list.isNotEmpty() && rawChapters.isEmpty()) {
                    publishChapters(list)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadChapters() {
        val current = comic ?: return
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    if (current.id == null || current.id == 0L) {
                        current.id = null
                        comicManager.updateOrInsert(current)
                    }
                    val chapters = mangaService.getComicInfo(sourceManager.getParser(current.source), current)
                    if (current.id != null) {
                        updateChapterList(chapters)
                    }
                    chapters
                }
                chapterManager.insertOrReplace(list)
                publishChapters(list)
                _events.emit(Event.NetworkLoadSuccess)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Manga.NetworkErrorException) {
                publishComic(current)
                _events.emit(Event.NetworkError)
            } catch (e: Manga.ParseErrorException) {
                publishComic(current)
                _events.emit(Event.ParseError)
            } catch (e: Exception) {
                e.printStackTrace()
                publishComic(current)
            }
        }
    }

    private fun cancelHighlight() {
        val current = comic ?: return
        if (current.highlight) {
            current.highlight = false
            current.favorite = System.currentTimeMillis()
            comicManager.update(current)
            AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_CANCEL_HIGHLIGHT, MiniComic(current)))
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
        if (current.id == null || current.id == 0L) {
            current.id = null
        }
        comicManager.updateOrInsert(current)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_READ, MiniComic(current)))
        _uiState.update { it.copy(comic = current, last = current.last) }
        return current.id ?: -1
    }

    fun backup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Backup.saveComicAuto(
                    app.contentResolver,
                    app.documentFile,
                    comicManager.listFavoriteOrHistory()
                )
            } catch (_: Exception) {
            }
        }
    }

    fun favoriteComic() {
        val current = comic ?: return
        current.favorite = System.currentTimeMillis()
        comicManager.updateOrInsert(current)
        publishComic(current)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_FAVORITE, MiniComic(current)))
    }

    fun unfavoriteComic() {
        val current = comic ?: return
        val id = current.id
        current.favorite = null
        tagRefManager.deleteByComic(id)
        comicManager.updateOrDelete(current)
        publishComic(current)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_UNFAVORITE, id))
    }

    private fun getTaskList(list: List<Chapter>): ArrayList<Task> {
        val current = comic ?: return ArrayList()
        val result = ArrayList<Task>(list.size)
        for (chapter in list) {
            val task = Task(null, -1, chapter.path, chapter.title, 0, 0)
            task.source = current.source
            task.cid = current.cid
            task.state = Task.STATE_WAIT
            result.add(task)
        }
        return result
    }

    fun addTask(cList: List<Chapter>, dList: List<Chapter>) {
        val current = comic ?: return
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val tasks = getTaskList(dList)
                    current.download = System.currentTimeMillis()
                    comicManager.runInTx {
                        comicManager.updateOrInsert(current)
                        for (task in tasks) {
                            task.key = current.id
                            taskManager.insert(task)
                        }
                    }
                    Download.updateComicIndex(
                        app.contentResolver,
                        app.documentFile,
                        cList,
                        current
                    )
                    tasks
                }
                AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_INSERT, MiniComic(current), result))
                _events.emit(Event.TaskAddSuccess(result))
            } catch (e: Exception) {
                _events.emit(Event.TaskAddFail)
            }
        }
    }

    fun refreshFromUpdate() {
        syncOrderFromPref()
        val current = comic ?: return
        if (current.id != null) {
            val loaded = comicManager.load(current.id) ?: return
            current.page = loaded.page
            current.last = loaded.last
            current.chapter = loaded.chapter
            _uiState.update { it.copy(comic = current, last = current.last) }
        }
    }

    private fun syncOrderFromPref() {
        val ascend = preferenceManager.getBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, false)
        if (userReversed == ascend) return
        userReversed = ascend
        if (rawChapters.isNotEmpty()) {
            _uiState.update { it.copy(chapters = displayedChapters()) }
        }
    }

    fun applyUpdateInfo(incoming: Comic) {
        if (comic?.id != null) {
            comicManager.insertOrReplace(incoming)
            comic = comicManager.load(incoming.id)
            val current = comic ?: return
            publishComic(current)
        }
    }
}
