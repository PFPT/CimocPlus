package com.haleydu.cimoc.ui.reader

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Download
import com.haleydu.cimoc.core.Local
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.core.Storage
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.ImageUrlManager
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.utils.StringUtils
import com.haleydu.cimoc.utils.pictureUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import java.io.File
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val comicManager: ComicManager,
    private val chapterManager: ChapterManager,
    private val sourceManager: SourceManager,
    private val imageUrlManager: ImageUrlManager,
    private val mangaService: MangaService,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val app = context.applicationContext as App
    private val savedState = savedStateHandle

    private var readerChapterManger: ReaderChapterManger? = null
    private var comic: Comic? = null
    private var currentChapter: Chapter? = null
    private var lastInitList: List<ImageUrl>? = null

    private var isShowNext = true
    private var isShowPrev = true
    private var count = 0
    private var status = LOAD_INIT
    private var loadNextSilent = false
    private var progressJob: Job? = null
    private var pendingPage = -1

    sealed class Event {
        object ParseError : Event()
        object NextLoadNone : Event()
        object PrevLoadNone : Event()
        object NextLoading : Event()
        object PrevLoading : Event()
        data class NextLoadSuccess(val list: List<ImageUrl>, val silent: Boolean = false) : Event()
        data class PrevLoadSuccess(val list: List<ImageUrl>) : Event()
        data class InitLoadSuccess(
            val list: List<ImageUrl>,
            val progress: Int,
            val source: Int,
            val local: Boolean
        ) : Event()
        data class ChapterChange(val chapter: Chapter) : Event()
        data class ImageLoadSuccess(val id: Long, val url: String) : Event()
        data class ImageLoadFail(val id: Long) : Event()
        data class PictureSaveSuccess(val uri: Uri) : Event()
        object PictureSaveFail : Event()
        data class PicturePaging(val image: ImageUrl, val tiles: Int = 2) : Event()
        object ScrollToStart : Event()
    }

    data class ReaderUiState(
        val page: Int = 1,
        val max: Int = 1,
        val title: String = ""
    )

    sealed class ReaderIntent {
        data class PageChanged(val page: Int) : ReaderIntent()
        object FlushProgress : ReaderIntent()
    }

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState

    fun dispatch(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.PageChanged -> updateComic(intent.page)
            ReaderIntent.FlushProgress -> flushComic()
        }
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
    val events: SharedFlow<Event> = _events

    init {
        viewModelScope.launch {
            AppEventBus.observe(AppEvent.EVENT_PICTURE_PAGING).collect { event ->
                val image = event.data as? ImageUrl ?: return@collect
                val tiles = (event.getData(1) as? Number)?.toInt() ?: 2
                _events.emit(Event.PicturePaging(image, tiles))
            }
        }
    }

    fun comicTitle(): String? = comic?.title

    fun parserHeader(list: List<ImageUrl>): Headers? {
        val current = comic ?: return null
        if (current.local) {
            return null
        }
        return sourceManager.getParser(current.source).getHeader(list)
    }

    fun lazyLoad(imageUrl: ImageUrl) {
        val current = comic ?: return
        viewModelScope.launch {
            try {
                val url = withContext(Dispatchers.IO) {
                    mangaService.loadLazyUrl(sourceManager.getParser(current.source), imageUrl.url)
                }
                val id = imageUrl.id ?: return@launch
                if (url == null) {
                    _events.emit(Event.ImageLoadFail(id))
                } else {
                    _events.emit(Event.ImageLoadSuccess(id, url))
                }
            } catch (e: Exception) {
                val id = imageUrl.id ?: return@launch
                _events.emit(Event.ImageLoadFail(id))
            }
        }
    }

    fun loadInit(id: Long, array: Array<Chapter>) {
        if (comic != null && readerChapterManger != null) {
            val current = comic ?: return
            val list = lastInitList
            if (list != null) {
                currentChapter?.let { _events.tryEmit(Event.ChapterChange(it)) }
                _events.tryEmit(
                    Event.InitLoadSuccess(
                        list,
                        current.page ?: 1,
                        current.source,
                        current.local
                    )
                )
            }
            return
        }
        val comicId = savedState.get<Long>(KEY_COMIC_ID) ?: id
        savedState[KEY_COMIC_ID] = comicId
        comic = comicManager.load(comicId)
        val current = comic
        if (current == null) {
            _events.tryEmit(Event.ParseError)
            return
        }
        val path = savedState.get<String>(KEY_CHAPTER_PATH) ?: current.last
        val page = savedState.get<Int>(KEY_PAGE)
        if (page != null) {
            current.page = page
        }
        val chapters = if (array.isNotEmpty()) {
            array
        } else {
            val sourceComic = (current.source.toString() + "000" + current.id).toLong()
            chapterManager.getListChapter(sourceComic).toTypedArray()
        }
        if (chapters.isEmpty()) {
            _events.tryEmit(Event.ParseError)
            return
        }
        for (i in chapters.indices) {
            if (chapters[i].path == path) {
                readerChapterManger = ReaderChapterManger(chapters, i)
                images(chapters[i])
                return
            }
        }
        readerChapterManger = ReaderChapterManger(chapters, 0)
        images(chapters[0])
    }

    @JvmOverloads
    fun loadNext(silent: Boolean = false) {
        if (status == LOAD_NULL && isShowNext) {
            val chapter = readerChapterManger?.nextChapterToLoad
            if (chapter != null) {
                status = LOAD_NEXT
                loadNextSilent = silent
                images(chapter)
                if (!silent) {
                    _events.tryEmit(Event.NextLoading)
                }
            } else {
                if (!silent) {
                    isShowNext = false
                    _events.tryEmit(Event.NextLoadNone)
                }
            }
        }
    }

    fun ensurePreload() {
        if (!preferenceManager.getBoolean(PreferenceManager.PREF_READER_PRELOAD, false)) {
            return
        }
        if (status != LOAD_NULL) {
            return
        }
        val preloadCount = preferenceManager.getInt(PreferenceManager.PREF_READER_PRELOAD_COUNT, 1)
            .coerceIn(1, 5)
        val ahead = readerChapterManger?.loadedAhead() ?: 0
        if (ahead < preloadCount) {
            loadNext(silent = true)
        }
    }

    fun loadPrev() {
        if (status == LOAD_NULL && isShowPrev) {
            val chapter = readerChapterManger?.prevChapterToLoad
            if (chapter != null) {
                status = LOAD_PREV
                images(chapter)
                _events.tryEmit(Event.PrevLoading)
            } else {
                isShowPrev = false
                _events.tryEmit(Event.PrevLoadNone)
            }
        }
    }

    fun toNextChapter() {
        val chapter = readerChapterManger?.nextChapter()
        if (chapter != null) {
            updateChapter(chapter, true)
            ensurePreload()
        }
    }

    fun toPrevChapter() {
        val chapter = readerChapterManger?.prevChapter()
        if (chapter != null) {
            updateChapter(chapter, false)
        }
    }

    fun chapters(): List<Chapter> = readerChapterManger?.chapters() ?: emptyList()

    fun currentChapter(): Chapter? = currentChapter

    fun jumpToChapter(path: String) {
        val manager = readerChapterManger ?: return
        val array = manager.array
        val index = array.indexOfFirst { it.path == path }
        if (index < 0) {
            return
        }
        if (currentChapter?.path == path) {
            _events.tryEmit(Event.ScrollToStart)
            return
        }
        if (status != LOAD_NULL) {
            return
        }
        isShowNext = true
        isShowPrev = true
        count = 0
        status = LOAD_INIT
        val current = comic ?: return
        current.page = 1
        savedState[KEY_PAGE] = 1
        savedState[KEY_CHAPTER_PATH] = path
        readerChapterManger = ReaderChapterManger(array, index)
        images(array[index])
    }

    fun savePicture(inputStream: InputStream, url: String, title: String, page: Int) {
        val current = comic ?: return
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    Storage.savePicture(
                        app.contentResolver,
                        app.documentFile,
                        inputStream,
                        buildPictureName(current.title, title, page, url)
                    )
                }
                _events.emit(Event.PictureSaveSuccess(uri))
            } catch (e: Exception) {
                _events.emit(Event.PictureSaveFail)
            }
        }
    }

    fun updateComic(page: Int) {
        if (status == LOAD_INIT) {
            return
        }
        pendingPage = page
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            delay(PROGRESS_DEBOUNCE_MS)
            persistProgress(page)
        }
    }

    fun flushComic() {
        progressJob?.cancel()
        val page = pendingPage
        if (page > 0) {
            persistProgress(page)
        }
    }

    private fun persistProgress(page: Int) {
        val current = comic ?: return
        current.page = page
        savedState[KEY_PAGE] = page
        current.last?.let { savedState[KEY_CHAPTER_PATH] = it }
        comicManager.update(current)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_UPDATE, current.id))
        _uiState.value = _uiState.value.copy(page = page, title = current.chapter ?: "")
    }

    fun switchNight() {
        AppEventBus.post(AppEvent(AppEvent.EVENT_SWITCH_NIGHT))
    }

    private suspend fun loadImages(chapter: Chapter): List<ImageUrl> {
        val current = comic ?: return emptyList()
        if (current.local) {
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DocumentFile.fromSubTreeUri(app, Uri.parse(chapter.path))
            } else {
                DocumentFile.fromFile(File(Uri.parse(chapter.path).path ?: chapter.path))
            }
            return Local.images(dir, chapter)
        }
        if (chapter.isComplete) {
            return Download.images(
                app.documentFile,
                current,
                chapter,
                sourceManager.getParser(current.source).title
            )
        }
        return mangaService.getChapterImage(
            chapter,
            sourceManager.getParser(current.source),
            current.cid ?: "",
            chapter.path
        )
    }

    private fun updateChapter(chapter: Chapter, isNext: Boolean) {
        val current = comic ?: return
        currentChapter = chapter
        _events.tryEmit(Event.ChapterChange(chapter))
        current.last = chapter.path
        current.chapter = chapter.title
        current.page = if (isNext) 1 else chapter.count
        savedState[KEY_CHAPTER_PATH] = chapter.path
        savedState[KEY_PAGE] = current.page
        comicManager.update(current)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_UPDATE, current.id))
    }

    private fun buildPictureName(comicTitle: String?, title: String, page: Int, url: String): String {
        var suffix = StringUtils.split(url, "\\.", -1)
        var suffixOriginal = suffix.split("\\?")[0].lowercase()
        if (!pictureUtils.isPictureFormat(suffixOriginal)) {
            suffixOriginal = "jpg"
        }
        suffix = suffixOriginal
        return StringUtils.format(
            "%s_%s_%03d.%s",
            StringUtils.filter(comicTitle),
            StringUtils.filter(title),
            page,
            suffix
        )
    }

    private fun images(chapter: Chapter) {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { loadImages(chapter) }
                imageUrlManager.insertOrReplace(list)
                handleImagesSuccess(list)
            } catch (e: Exception) {
                val recovered = handleImagesFallback()
                if (!recovered) {
                    _events.emit(Event.ParseError)
                }
                if (status != LOAD_INIT) {
                    count++
                    if (count < 2) {
                        status = LOAD_NULL
                    }
                }
            }
        }
    }

    private fun handleImagesSuccess(list: List<ImageUrl>) {
        val manager = readerChapterManger ?: return
        val currentComic = comic ?: return
        val finished = status
        when (status) {
            LOAD_INIT -> {
                val current = manager.moveNext()
                current.count = list.size
                currentChapter = current
                lastInitList = list
                savedState[KEY_CHAPTER_PATH] = current.path
                if (current.title != currentComic.title) {
                    currentComic.chapter = current.title
                    comicManager.update(currentComic)
                }
                _events.tryEmit(Event.ChapterChange(current))
                _events.tryEmit(
                    Event.InitLoadSuccess(
                        list,
                        currentComic.page ?: 1,
                        currentComic.source,
                        currentComic.local
                    )
                )
            }
            LOAD_PREV -> {
                val current = manager.movePrev()
                current.count = list.size
                _events.tryEmit(Event.PrevLoadSuccess(list))
            }
            LOAD_NEXT -> {
                val current = manager.moveNext()
                current.count = list.size
                val silent = loadNextSilent
                loadNextSilent = false
                _events.tryEmit(Event.NextLoadSuccess(list, silent))
            }
        }
        status = LOAD_NULL
        if (finished == LOAD_INIT || finished == LOAD_NEXT) {
            ensurePreload()
        }
    }

    private fun handleImagesFallback(): Boolean {
        val manager = readerChapterManger ?: return false
        val currentComic = comic ?: return false
        val finished = status
        var recovered = false
        when (status) {
            LOAD_INIT -> {
                val current = manager.moveNext()
                val list = imageUrlManager.getListImageUrl(current.id)
                if (!list.isNullOrEmpty()) {
                    recovered = true
                    current.count = list.size
                    currentChapter = current
                    lastInitList = list
                    savedState[KEY_CHAPTER_PATH] = current.path
                    if (current.title != currentComic.title) {
                        currentComic.chapter = current.title
                        comicManager.update(currentComic)
                    }
                    _events.tryEmit(Event.ChapterChange(current))
                    _events.tryEmit(
                        Event.InitLoadSuccess(
                            list,
                            currentComic.page ?: 1,
                            currentComic.source,
                            currentComic.local
                        )
                    )
                }
            }
            LOAD_PREV -> {
                val current = manager.movePrev()
                val list = imageUrlManager.getListImageUrl(current.id)
                if (!list.isNullOrEmpty()) {
                    recovered = true
                    current.count = list.size
                    _events.tryEmit(Event.PrevLoadSuccess(list))
                }
            }
            LOAD_NEXT -> {
                val current = manager.moveNext()
                val list = imageUrlManager.getListImageUrl(current.id)
                if (!list.isNullOrEmpty()) {
                    recovered = true
                    current.count = list.size
                    val silent = loadNextSilent
                    loadNextSilent = false
                    _events.tryEmit(Event.NextLoadSuccess(list, silent))
                }
            }
        }
        status = LOAD_NULL
        if (finished == LOAD_INIT || finished == LOAD_NEXT) {
            ensurePreload()
        }
        return recovered
    }

    private class ReaderChapterManger(val array: Array<Chapter>, index: Int) {
        private val nextDelta = storyNextDelta(array)
        private val prevDelta = -nextDelta
        private var index = index
        private var next = index
        private var prev = index + prevDelta

        fun chapters(): List<Chapter> = array.toList()

        fun loadedAhead(): Int {
            val ahead = if (nextDelta > 0) {
                next - index - 1
            } else {
                index - next - 1
            }
            return ahead.coerceAtLeast(0)
        }

        val prevChapterToLoad: Chapter?
            get() = array.getOrNull(prev)

        val nextChapterToLoad: Chapter?
            get() = array.getOrNull(next)

        fun prevChapter(): Chapter? {
            val candidate = index + prevDelta
            if (isLoaded(index, prev, candidate, prevDelta)) {
                index = candidate
                return array[index]
            }
            return null
        }

        fun nextChapter(): Chapter? {
            val candidate = index + nextDelta
            if (isLoaded(index, next, candidate, nextDelta)) {
                index = candidate
                return array[index]
            }
            return null
        }

        fun movePrev(): Chapter {
            val chapter = array[prev]
            prev += prevDelta
            return chapter
        }

        fun moveNext(): Chapter {
            val chapter = array[next]
            next += nextDelta
            return chapter
        }

        companion object {
            private fun chapterNumber(title: String?): Int? {
                if (title.isNullOrEmpty()) {
                    return null
                }
                val match = Regex("""(\d+)""").find(title) ?: return null
                return match.groupValues[1].toIntOrNull()
            }

            private fun storyNextDelta(array: Array<Chapter>): Int {
                if (array.size < 2) {
                    return 1
                }
                val first = chapterNumber(array.first().title)
                val last = chapterNumber(array.last().title)
                if (first != null && last != null && first != last) {
                    return if (first > last) -1 else 1
                }
                return 1
            }

            private fun isLoaded(from: Int, toExclusive: Int, candidate: Int, delta: Int): Boolean {
                return if (delta > 0) {
                    candidate > from && candidate < toExclusive
                } else {
                    candidate < from && candidate > toExclusive
                }
            }
        }
    }

    companion object {
        private const val LOAD_NULL = 0
        private const val LOAD_INIT = 1
        private const val LOAD_PREV = 2
        private const val LOAD_NEXT = 3
        private const val KEY_COMIC_ID = "comicId"
        private const val KEY_CHAPTER_PATH = "chapterPath"
        private const val KEY_PAGE = "page"
        private const val PROGRESS_DEBOUNCE_MS = 600L
    }
}
