package com.haleydu.cimoc.app

import android.content.Context
import android.net.Uri
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Backup
import com.haleydu.cimoc.core.Download
import com.haleydu.cimoc.core.Local
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.saf.DocumentFile
import java.io.File
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.ImageUrlManager
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.data.SourceCatalogRefresher
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.data.SourceProbe
import com.haleydu.cimoc.data.SourceRuleManager
import com.haleydu.cimoc.data.TagManager
import com.haleydu.cimoc.data.TagRefManager
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.model.TagRef
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.parser.Category
import com.haleydu.cimoc.service.DownloadService
import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.DownloadTaskItem
import com.haleydu.cimoc.shared.model.ExploreFilter
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.SearchStats
import com.haleydu.cimoc.shared.model.SourceItem
import com.haleydu.cimoc.shared.model.TagItem
import com.haleydu.cimoc.shared.repo.CimocRepositories
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.DownloadRepository
import com.haleydu.cimoc.shared.repo.ExploreRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SettingsRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import com.haleydu.cimoc.shared.repo.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

fun createCimocRepositories(
    context: Context,
    comicManager: ComicManager,
    chapterManager: ChapterManager,
    imageUrlManager: ImageUrlManager,
    sourceManager: SourceManager,
    mangaService: MangaService,
    sourceHealthManager: SourceHealthManager,
    sourceProbe: SourceProbe,
    sourceRuleManager: SourceRuleManager,
    sourceCatalogRefresher: SourceCatalogRefresher,
    taskManager: TaskManager,
    preferenceManager: PreferenceManager,
    tagManager: TagManager,
    tagRefManager: TagRefManager
): CimocRepositories {
    val comics = AndroidComicRepository(comicManager)
    val sources = AndroidSourceRepository(sourceManager, sourceHealthManager, sourceProbe, sourceRuleManager)
    val manga = AndroidMangaRepository(
        comicManager,
        chapterManager,
        imageUrlManager,
        sourceManager,
        mangaService,
        sourceHealthManager,
        sourceCatalogRefresher
    )
    val explore = AndroidExploreRepository(sourceManager, mangaService, sourceHealthManager)
    val downloads = AndroidDownloadRepository(context, comicManager, chapterManager, taskManager)
    val settings = AndroidSettingsRepository(preferenceManager, comicManager)
    val tags = AndroidTagRepository(tagManager, tagRefManager)
    return CimocRepositories(comics, sources, manga, explore, downloads, settings, tags)
}

private class AndroidComicRepository(
    private val comicManager: ComicManager
) : ComicRepository {
    override suspend fun listFavorite(): List<ComicItem> = withContext(Dispatchers.IO) {
        comicManager.listFavoriteOrdered().orEmpty().map { it.toItem() }
    }

    override suspend fun listHistory(): List<ComicItem> = withContext(Dispatchers.IO) {
        comicManager.listHistory().orEmpty().map { it.toItem() }
    }

    override suspend fun listDownload(): List<ComicItem> = withContext(Dispatchers.IO) {
        comicManager.listDownloadOrdered().orEmpty().map { it.toItem() }
    }

    override suspend fun listLocal(): List<ComicItem> = withContext(Dispatchers.IO) {
        comicManager.listLocal().orEmpty().map { it.toItem() }
    }

    override suspend fun listFavoritePage(offset: Int, limit: Int): List<ComicItem> =
        withContext(Dispatchers.IO) {
            comicManager.listFavoritePage(offset, limit).map { it.toItem() }
        }

    override suspend fun listHistoryPage(offset: Int, limit: Int): List<ComicItem> =
        withContext(Dispatchers.IO) {
            comicManager.listHistoryPage(offset, limit).map { it.toItem() }
        }

    override suspend fun listDownloadPage(offset: Int, limit: Int): List<ComicItem> =
        withContext(Dispatchers.IO) {
            comicManager.listDownloadPage(offset, limit).map { it.toItem() }
        }

    override suspend fun listLocalPage(offset: Int, limit: Int): List<ComicItem> =
        withContext(Dispatchers.IO) {
            comicManager.listLocalPage(offset, limit).map { it.toItem() }
        }

    override suspend fun listContinuePage(offset: Int, limit: Int): List<ComicItem> = withContext(Dispatchers.IO) {
        comicManager.listContinuePage(offset, limit).map { it.toItem() }
    }

    override suspend fun listFinishPage(offset: Int, limit: Int): List<ComicItem> = withContext(Dispatchers.IO) {
        comicManager.listFinishPage(offset, limit).map { it.toItem() }
    }

    override suspend fun listFavoriteByTagPage(tid: Long, offset: Int, limit: Int): List<ComicItem> =
        withContext(Dispatchers.IO) {
            comicManager.listFavoriteByTagPage(tid, offset, limit).map { it.toItem() }
        }

    override suspend fun load(id: Long): ComicItem? = withContext(Dispatchers.IO) {
        comicManager.load(id)?.toItem()
    }

    override suspend fun loadOrCreate(source: Int, cid: String): ComicItem = withContext(Dispatchers.IO) {
        comicManager.loadOrCreate(source, cid.ifEmpty { "" }).toItem()
    }

    override suspend fun favorite(item: ComicItem) = withContext(Dispatchers.IO) {
        val comic = resolve(item)
        comic.favorite = System.currentTimeMillis()
        comicManager.updateOrInsert(comic)
    }

    override suspend fun unfavorite(id: Long) = withContext(Dispatchers.IO) {
        val comic = comicManager.load(id) ?: return@withContext
        comic.favorite = null
        comicManager.updateOrDelete(comic)
    }

    override suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        val comic = comicManager.load(id) ?: return@withContext
        comic.history = null
        comicManager.updateOrDelete(comic)
    }

    override suspend fun clearHighlight(id: Long) = withContext(Dispatchers.IO) {
        val comic = comicManager.load(id) ?: return@withContext
        if (!comic.highlight) return@withContext
        comic.highlight = false
        comicManager.update(comic)
    }

    private fun resolve(item: ComicItem): Comic {
        val id = item.id
        val existing = if (id != null && id > 0) comicManager.load(id) else null
        val comic = existing ?: comicManager.loadOrCreate(item.source, item.cid)
        if (comic.title.isNullOrEmpty()) comic.title = item.title
        if (comic.cover.isNullOrEmpty()) comic.cover = item.cover
        if (comic.author.isNullOrEmpty()) comic.author = item.author
        return comic
    }
}

private class AndroidSourceRepository(
    private val sourceManager: SourceManager,
    private val sourceHealthManager: SourceHealthManager,
    private val sourceProbe: SourceProbe,
    private val sourceRuleManager: SourceRuleManager
) : SourceRepository {
    override suspend fun listAll(): List<SourceItem> = withContext(Dispatchers.IO) {
        val invalid = sourceHealthManager.invalidTypes()
        sourceManager.list().orEmpty().map { it.toItem(invalid.contains(it.type)) }
    }

    override suspend fun listEnable(): List<SourceItem> = withContext(Dispatchers.IO) {
        val invalid = sourceHealthManager.invalidTypes()
        sourceHealthManager.sortSources(sourceManager.listEnable().orEmpty())
            .map { it.toItem(invalid.contains(it.type)) }
    }

    override suspend fun setEnabled(type: Int, enable: Boolean) = withContext(Dispatchers.IO) {
        val source = sourceManager.load(type) ?: return@withContext
        source.enable = enable
        sourceManager.update(source)
    }

    override fun sourceTitle(type: Int): String {
        return sourceManager.titleOf(type)
    }

    override suspend fun homepage(type: Int): String = withContext(Dispatchers.IO) {
        val url = sourceProbe.homepage(type)
        if (url.startsWith("http")) url else ""
    }

    override fun warm() {
        sourceProbe.probeAsync()
    }

    override suspend fun import(input: String): Int = withContext(Dispatchers.IO) {
        sourceRuleManager.import(input)
    }

    override suspend fun removeCustom(type: Int) = withContext(Dispatchers.IO) {
        sourceRuleManager.deleteCustom(type)
    }
}

private class AndroidMangaRepository(
    private val comicManager: ComicManager,
    private val chapterManager: ChapterManager,
    private val imageUrlManager: ImageUrlManager,
    private val sourceManager: SourceManager,
    private val mangaService: MangaService,
    private val sourceHealthManager: SourceHealthManager,
    private val sourceCatalogRefresher: SourceCatalogRefresher
) : MangaRepository {
    override fun search(
        sources: List<Int>,
        keyword: String,
        page: Int,
        strict: Boolean,
        stats: SearchStats?,
        recordHealth: Boolean
    ): Flow<ComicItem> = channelFlow {
        val failed = AtomicInteger()
        coroutineScope {
            val gate = Semaphore(6)
            val ordered = sourceHealthManager.sortTypes(sources)
            ordered.map { type ->
                async(Dispatchers.IO) {
                    gate.acquire()
                    try {
                        val parser = sourceManager.getParser(type)
                        mangaService.getSearchResult(parser, keyword, page, strict)
                            .catch {
                                failed.incrementAndGet()
                            }
                            .collect { send(it.toItem()) }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        failed.incrementAndGet()
                        if (recordHealth) {
                            sourceHealthManager.recordFailure(type, SourceHealthManager.KIND_PARSE)
                        }
                    } finally {
                        gate.release()
                    }
                }
            }.awaitAll()
        }
        stats?.failed = failed.get()
    }

    override suspend fun loadRank(): LoadResult<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val list = mangaService.loadTencentRank()
            if (list.isEmpty()) LoadResult.Err("热度榜加载失败") else LoadResult.Ok(list.map { it.toItem() })
        } catch (_: Exception) {
            LoadResult.Err("热度榜加载失败")
        }
    }

    override suspend fun loadDetail(
        item: ComicItem,
        recordHealth: Boolean
    ): LoadResult<Pair<ComicItem, List<ChapterItem>>> =
        withContext(Dispatchers.IO) {
            val id = item.id
            val comic = if (id != null && id > 0) {
                comicManager.load(id) ?: comicManager.loadOrCreate(item.source, item.cid)
            } else {
                comicManager.loadOrCreate(item.source, item.cid)
            }
            if (!item.title.isNullOrEmpty() && comic.title.isNullOrEmpty()) comic.title = item.title
            if (!item.cover.isNullOrEmpty() && comic.cover.isNullOrEmpty()) comic.cover = item.cover
            if (!item.author.isNullOrEmpty() && comic.author.isNullOrEmpty()) comic.author = item.author
            val start = android.os.SystemClock.elapsedRealtime()
            try {
                if (comic.id == null || comic.id == 0L) {
                    comic.id = null
                    comicManager.updateOrInsert(comic)
                }
                val parser = sourceManager.getParser(comic.source)
                val chapters = mangaService.getComicInfo(parser, comic)
                chapterManager.updateOrInsert(chapters)
                if (recordHealth) {
                    sourceHealthManager.recordSuccess(
                        comic.source,
                        android.os.SystemClock.elapsedRealtime() - start
                    )
                }
                LoadResult.Ok(comic.toItem() to chapters.map { it.toItem() })
            } catch (e: Manga.NetworkErrorException) {
                if (recordHealth) {
                    sourceHealthManager.recordFailure(comic.source, SourceHealthManager.KIND_NETWORK)
                    sourceCatalogRefresher.refreshAfterFailure(comic.source)
                }
                offlineDetail(comic) ?: LoadResult.Err("网络错误", network = true)
            } catch (e: Manga.ParseErrorException) {
                if (recordHealth) {
                    sourceHealthManager.recordFailure(comic.source, SourceHealthManager.KIND_PARSE)
                    sourceCatalogRefresher.refreshAfterFailure(comic.source)
                }
                offlineDetail(comic) ?: LoadResult.Err("解析失败")
            } catch (e: Exception) {
                if (recordHealth) {
                    sourceHealthManager.recordFailure(comic.source, SourceHealthManager.KIND_PARSE)
                }
                offlineDetail(comic) ?: LoadResult.Err(e.message ?: "加载失败")
            }
        }

    override suspend fun loadPages(item: ComicItem, chapter: ChapterItem): LoadResult<List<PageImage>> =
        withContext(Dispatchers.IO) {
            val comic = if (item.id != null && item.id!! > 0) comicManager.load(item.id!!) else null
            val entity = Chapter(chapter.title, chapter.path).apply {
                id = chapter.id
                sourceComic = chapter.sourceComic
                count = if (chapter.count > 0) chapter.count else Int.MAX_VALUE
                complete = chapter.complete
            }
            val offline = loadOfflinePages(item, comic, entity)
            if (!offline.isNullOrEmpty()) {
                runCatching { imageUrlManager.insertOrReplace(offline) }
                return@withContext LoadResult.Ok(offline.map { it.toItem() })
            }
            try {
                val parser = sourceManager.getParser(item.source)
                val list = mangaService.getChapterImage(entity, parser, item.cid, chapter.path)
                imageUrlManager.insertOrReplace(list)
                val pages = list.map { it.toItem() }
                if (pages.isEmpty()) LoadResult.Err("暂无图片") else LoadResult.Ok(pages)
            } catch (e: Manga.NetworkErrorException) {
                sourceHealthManager.recordFailure(item.source, SourceHealthManager.KIND_NETWORK)
                fallbackPages(chapter) ?: LoadResult.Err("网络错误", network = true)
            } catch (e: Exception) {
                sourceHealthManager.recordFailure(item.source, SourceHealthManager.KIND_PARSE)
                fallbackPages(chapter) ?: LoadResult.Err("解析失败")
            }
        }

    private fun offlineDetail(comic: Comic): LoadResult.Ok<Pair<ComicItem, List<ChapterItem>>>? {
        val id = comic.id ?: return null
        val sourceComic = (comic.source.toString() + "000" + id).toLongOrNull() ?: return null
        val chapters = chapterManager.getListChapter(sourceComic)
        if (chapters.isEmpty()) return null
        if (comic.download == null && chapters.none { it.complete }) return null
        return LoadResult.Ok(comic.toItem() to chapters.map { it.toItem() })
    }

    private fun loadOfflinePages(item: ComicItem, comic: Comic?, chapter: Chapter): List<com.haleydu.cimoc.model.ImageUrl>? {
        val app = App.getApp()
        val isLocal = item.local || comic?.local == true
        if (isLocal) {
            val uri = chapter.path?.takeIf { it.isNotEmpty() } ?: item.cid
            val dir = openLocalDir(app, uri)
            return runCatching { Local.images(dir, chapter) }.getOrNull()?.takeIf { it.isNotEmpty() }
        }
        val title = sourceManager.titleOf(item.source)
        val entity = comic ?: Comic(item.source, item.cid, item.title, item.cover, null, item.author)
        val hasDir = runCatching { Download.getChapterDir(app.documentFile, entity, chapter, title) }.getOrNull() != null
        if (chapter.complete || comic?.download != null || hasDir) {
            return runCatching { Download.images(app.documentFile, entity, chapter, title) }.getOrNull()?.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun openLocalDir(context: android.content.Context, uri: String): DocumentFile? {
        if (uri.isEmpty()) return null
        return runCatching {
            when {
                uri.startsWith("content") -> DocumentFile.fromSubTreeUri(context, Uri.parse(uri))
                uri.startsWith("file") -> {
                    val path = Uri.parse(uri).path ?: return@runCatching null
                    DocumentFile.fromFile(File(path))
                }
                else -> DocumentFile.fromFile(File(uri))
            }
        }.getOrNull()
    }

    private fun fallbackPages(chapter: ChapterItem): LoadResult.Ok<List<PageImage>>? {
        val cached = imageUrlManager.getListImageUrl(chapter.id).orEmpty().map { it.toItem() }
        return if (cached.isNotEmpty()) LoadResult.Ok(cached) else null
    }

    override suspend fun resolveLazy(source: Int, url: String): String? = withContext(Dispatchers.IO) {
        runCatching { mangaService.loadLazyUrl(sourceManager.getParser(source), url) }.getOrNull()
    }

    override suspend fun matchUrl(url: String): ComicItem? = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return@withContext null
        if (uri.host.isNullOrEmpty()) return@withContext null
        for (source in sourceManager.list()) {
            val parser = runCatching { sourceManager.getParser(source.type) }.getOrNull() ?: continue
            val here = runCatching { parser.isHere(uri) }.getOrDefault(false)
            if (!here) continue
            val cid = runCatching { parser.getComicId(uri) }.getOrNull()?.takeIf { it.isNotEmpty() } ?: continue
            return@withContext ComicItem(source = source.type, cid = cid, title = "")
        }
        null
    }

    override fun imageHeaders(source: Int): Map<String, String> {
        return runCatching {
            val headers = sourceManager.getParser(source).getHeader() ?: return emptyMap()
            buildMap {
                for (i in 0 until headers.size) {
                    put(headers.name(i), headers.value(i))
                }
            }
        }.getOrDefault(emptyMap())
    }

    override suspend fun markRead(item: ComicItem, path: String): Long = withContext(Dispatchers.IO) {
        val id = item.id
        val comic = if (id != null && id > 0) {
            comicManager.load(id) ?: comicManager.loadOrCreate(item.source, item.cid)
        } else {
            comicManager.loadOrCreate(item.source, item.cid)
        }
        if (comic.favorite != null) comic.favorite = System.currentTimeMillis()
        comic.history = System.currentTimeMillis()
        if (path != comic.last) {
            comic.last = path
            comic.page = 1
        }
        if (comic.id == null || comic.id == 0L) comic.id = null
        comicManager.updateOrInsert(comic)
        comic.id ?: -1L
    }

    override suspend fun saveProgress(comicId: Long, page: Int) = withContext(Dispatchers.IO) {
        val comic = comicManager.load(comicId) ?: return@withContext
        comic.page = page
        comicManager.update(comic)
    }

    override suspend fun checkFavorites(): Int = withContext(Dispatchers.IO) {
        var count = 0
        var offset = 0
        while (true) {
            val batch = comicManager.listFavoritePage(offset, FAVORITE_PAGE)
            if (batch.isEmpty()) break
            mangaService.checkUpdate(sourceManager, batch).collect { comic ->
                if (comic != null) {
                    comicManager.update(comic)
                    count++
                }
            }
            offset += batch.size
            if (batch.size < FAVORITE_PAGE) break
        }
        count
    }

    companion object {
        private const val FAVORITE_PAGE = 40
    }
}

private class AndroidExploreRepository(
    private val sourceManager: SourceManager,
    private val mangaService: MangaService,
    private val sourceHealthManager: SourceHealthManager
) : ExploreRepository {
    override suspend fun loadFilters(source: Int): List<ExploreFilter> = withContext(Dispatchers.IO) {
        val parser = runCatching { sourceManager.getParser(source) }.getOrNull() ?: return@withContext emptyList()
        val category = parser.getCategory() ?: return@withContext emptyList()
        val groups = listOf(
            Category.CATEGORY_SUBJECT to "题材",
            Category.CATEGORY_AREA to "地区",
            Category.CATEGORY_PROGRESS to "进度",
            Category.CATEGORY_ORDER to "排序"
        )
        groups.mapNotNull { (attr, title) ->
            if (!category.hasAttribute(attr)) return@mapNotNull null
            val options = category.getAttrList(attr)?.map { option ->
                (option.first ?: "") to (option.second ?: "")
            }.orEmpty()
            if (options.isEmpty()) null else ExploreFilter(attr, title, options)
        }
    }

    override suspend fun loadPage(source: Int, page: Int, args: List<String>): LoadResult<List<ComicItem>> =
        withContext(Dispatchers.IO) {
            try {
                val parser = sourceManager.getParser(source)
                val category = parser.getCategory()
                val format = if (category != null) {
                    if (args.isEmpty()) category.getFormat("") else category.getFormat(*args.toTypedArray())
                } else {
                    parser.getUrl("")
                }
                if (format.isNullOrEmpty() || (!format.contains("http") && !format.contains("%"))) {
                    return@withContext LoadResult.Err("该图源不支持浏览")
                }
                val list = mangaService.getCategoryComic(parser, format, page)
                if (list.isEmpty()) LoadResult.Err("暂无内容") else LoadResult.Ok(list.map { it.toItem() })
            } catch (e: Manga.NetworkErrorException) {
                sourceHealthManager.recordFailure(source, SourceHealthManager.KIND_NETWORK)
                LoadResult.Err("网络错误", network = true)
            } catch (e: Exception) {
                sourceHealthManager.recordFailure(source, SourceHealthManager.KIND_PARSE)
                LoadResult.Err(e.message ?: "加载失败")
            }
        }
}

private class AndroidDownloadRepository(
    private val context: Context,
    private val comicManager: ComicManager,
    private val chapterManager: ChapterManager,
    private val taskManager: TaskManager
) : DownloadRepository {
    private val runtime = ConcurrentHashMap<Long, Int>()

    override suspend fun enqueue(item: ComicItem, chapters: List<ChapterItem>): LoadResult<Int> =
        withContext(Dispatchers.IO) {
            try {
                val comicId = item.id
                val comic = if (comicId != null && comicId > 0) {
                    comicManager.load(comicId) ?: comicManager.loadOrCreate(item.source, item.cid)
                } else {
                    comicManager.loadOrCreate(item.source, item.cid)
                }
                comic.download = System.currentTimeMillis()
                comicManager.updateOrInsert(comic)
                val id = comic.id ?: return@withContext LoadResult.Err("无法保存漫画")
                val tasks = ArrayList<Task>()
                val sourceComic = (comic.source.toString() + "000" + id).toLongOrNull()
                for (chapter in chapters) {
                    if (chapter.path.isEmpty()) continue
                    val existing = taskManager.load(id, chapter.path)
                    val task = if (existing != null) {
                        existing
                    } else {
                        Task(null, id, chapter.path, chapter.title, 0, 0).also { taskManager.insert(it) }
                    }
                    task.source = comic.source
                    task.cid = comic.cid
                    if (task.isFinish) {
                        if (sourceComic != null) chapterManager.markDownloaded(sourceComic, chapter.path, task.max)
                        continue
                    }
                    task.state = Task.STATE_WAIT
                    val taskId = task.id ?: continue
                    runtime[taskId] = Task.STATE_WAIT
                    tasks.add(task)
                }
                runCatching {
                    val app = context.applicationContext as App
                    Download.updateComicIndex(
                        app.contentResolver,
                        app.documentFile,
                        chapters.map { Chapter(it.title, it.path) },
                        comic
                    )
                }
                if (tasks.isNotEmpty()) {
                    val intent = DownloadService.createIntent(context, tasks)
                    DownloadService.start(context, intent)
                }
                LoadResult.Ok(tasks.size)
            } catch (e: Exception) {
                LoadResult.Err(e.message ?: "加入下载失败")
            }
        }

    override fun observeTasks(): Flow<List<DownloadTaskItem>> = callbackFlow {
        fun snapshot(): List<DownloadTaskItem> {
            val tasks = taskManager.list()
            DownloadService.syncTaskState(tasks)
            for (task in tasks) {
                val id = task.id ?: continue
                if (task.state != Task.STATE_FINISH) runtime[id] = task.state
            }
            return tasks.mapNotNull { task ->
                val id = task.id ?: return@mapNotNull null
                val state = when {
                    task.isFinish -> DownloadTaskItem.STATE_FINISH
                    runtime.containsKey(id) -> runtime.getValue(id)
                    else -> DownloadTaskItem.STATE_WAIT
                }
                DownloadTaskItem(id, task.key, task.title, task.progress, task.max, state)
            }
        }
        var cached = snapshot()
        trySend(cached)
        val job = launch {
            AppEventBus.events.collect { event ->
                when (event.type) {
                    AppEvent.EVENT_TASK_STATE_CHANGE -> {
                        val state = event.getData(0) as? Int
                        val id = event.getData(1) as? Long
                        if (id != null && state != null) runtime[id] = state
                        cached = snapshot()
                        trySend(cached)
                    }
                    AppEvent.EVENT_TASK_PROCESS -> {
                        val id = event.getData(0) as? Long ?: return@collect
                        val progress = event.getData(1) as? Int
                        val max = event.getData(2) as? Int
                        runtime.putIfAbsent(id, DownloadTaskItem.STATE_DOING)
                        val index = cached.indexOfFirst { it.id == id }
                        if (index < 0 || progress == null) {
                            cached = snapshot()
                        } else {
                            val item = cached[index]
                            val nextMax = max ?: item.max
                            val state = if (nextMax != 0 && progress >= nextMax) {
                                DownloadTaskItem.STATE_FINISH
                            } else {
                                DownloadTaskItem.STATE_DOING
                            }
                            val next = cached.toMutableList()
                            next[index] = item.copy(progress = progress, max = nextMax, state = state)
                            cached = next
                        }
                        trySend(cached)
                    }
                    AppEvent.EVENT_DOWNLOAD_REMOVE -> {
                        val id = event.getData(0) as? Long
                        if (id != null) runtime.remove(id)
                        cached = snapshot()
                        trySend(cached)
                    }
                }
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun pause(id: Long) = withContext(Dispatchers.IO) {
        DownloadService.pauseTask(id)
        runtime[id] = Task.STATE_PAUSE
        AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_PAUSE, id))
    }

    override suspend fun retry(id: Long) = withContext(Dispatchers.IO) {
        val task = taskManager.list().firstOrNull { it.id == id } ?: return@withContext
        val comic = comicManager.load(task.key) ?: return@withContext
        task.source = comic.source
        task.cid = comic.cid
        task.state = Task.STATE_WAIT
        runtime[id] = Task.STATE_WAIT
        AppEventBus.post(AppEvent(AppEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_WAIT, id))
        val intent = DownloadService.createIntent(context, task)
        DownloadService.start(context, intent)
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        deleteOne(id)
    }

    override suspend fun delete(ids: List<Long>) = withContext(Dispatchers.IO) {
        for (id in ids) deleteOne(id)
    }

    private fun deleteOne(id: Long) {
        DownloadService.pauseTask(id)
        runtime.remove(id)
        taskManager.delete(id)
        AppEventBus.post(AppEvent(AppEvent.EVENT_DOWNLOAD_REMOVE, id))
    }
}

private class AndroidTagRepository(
    private val tagManager: TagManager,
    private val tagRefManager: TagRefManager
) : TagRepository {
    override suspend fun list(): List<TagItem> = withContext(Dispatchers.IO) {
        tagManager.list().mapNotNull { tag ->
            val id = tag.id ?: return@mapNotNull null
            TagItem(id, tag.title)
        }
    }

    override suspend fun create(title: String): TagItem? = withContext(Dispatchers.IO) {
        val name = title.trim()
        if (name.isEmpty()) return@withContext null
        val existing = tagManager.load(name)
        if (existing != null) {
            val id = existing.id ?: return@withContext null
            return@withContext TagItem(id, existing.title)
        }
        val tag = Tag(null, name)
        tagManager.insert(tag)
        val id = tag.id ?: return@withContext null
        TagItem(id, tag.title)
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        tagRefManager.deleteByTag(id)
        val tag = tagManager.list().firstOrNull { it.id == id } ?: return@withContext
        tagManager.delete(tag)
    }

    override suspend fun tagsOf(comicId: Long): List<TagItem> = withContext(Dispatchers.IO) {
        val all = tagManager.list().associateBy { it.id }
        tagRefManager.listByComic(comicId).mapNotNull { ref ->
            val tag = all[ref.tid] ?: return@mapNotNull null
            val id = tag.id ?: return@mapNotNull null
            TagItem(id, tag.title)
        }
    }

    override suspend fun setTags(comicId: Long, ids: List<Long>) = withContext(Dispatchers.IO) {
        tagRefManager.deleteByComic(comicId)
        if (ids.isEmpty()) return@withContext
        tagRefManager.insert(ids.map { TagRef(null, it, comicId) })
    }
}

private class AndroidSettingsRepository(
    private val preferenceManager: PreferenceManager,
    private val comicManager: ComicManager
) : SettingsRepository {
    override fun bool(key: String, default: Boolean): Boolean = preferenceManager.getBoolean(key, default)
    override fun setBool(key: String, value: Boolean) {
        preferenceManager.putBoolean(key, value)
    }
    override fun int(key: String, default: Int): Int = preferenceManager.getInt(key, default)
    override fun setInt(key: String, value: Int) {
        preferenceManager.putInt(key, value)
    }
    override fun string(key: String, default: String): String {
        return preferenceManager.getString(key, default) ?: default
    }
    override fun setString(key: String, value: String) {
        preferenceManager.putString(key, value)
    }

    override suspend fun backupFavorites(): LoadResult<Int> = withContext(Dispatchers.IO) {
        try {
            val app = App.getApp()
            val n = Backup.saveComic(app.contentResolver, app.documentFile, comicManager.listFavorite())
            if (n < 0) LoadResult.Err("备份失败") else LoadResult.Ok(n)
        } catch (e: Exception) {
            LoadResult.Err(e.message ?: "备份失败")
        }
    }

    override suspend fun restoreFavorites(filename: String): LoadResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val app = App.getApp()
            val list = Backup.restoreComic(app.contentResolver, app.documentFile, filename)
            if (list.isNullOrEmpty()) {
                LoadResult.Err("恢复失败")
            } else {
                for (comic in list) {
                    comicManager.updateOrInsert(comic)
                }
                LoadResult.Ok(Unit)
            }
        } catch (e: Exception) {
            LoadResult.Err(e.message ?: "恢复失败")
        }
    }

    override suspend fun listBackupFiles(): List<String> = withContext(Dispatchers.IO) {
        runCatching { Backup.loadFavorite(App.getApp().documentFile)?.toList().orEmpty() }
            .getOrDefault(emptyList())
    }
}

private fun Comic.toItem() = ComicItem(
    id = id,
    source = source,
    cid = cid ?: "",
    title = title ?: "",
    cover = cover ?: "",
    author = author,
    intro = intro,
    update = update,
    last = last,
    chapter = chapter,
    url = url,
    favorite = favorite != null,
    history = history,
    highlight = highlight,
    local = local,
    finish = finish == true,
    page = page ?: 1
)

private fun Chapter.toItem() = ChapterItem(
    id = id,
    sourceComic = sourceComic,
    title = title ?: "",
    path = path ?: "",
    count = count,
    complete = complete
)

private fun ImageUrl.toItem() = PageImage(
    id = id,
    chapterId = comicChapter,
    index = num,
    urls = urls?.toList().orEmpty(),
    lazy = lazy
)

private fun Source.toItem(invalid: Boolean) = SourceItem(
    id = id,
    type = type,
    title = title ?: "",
    enable = enable,
    invalid = invalid,
    custom = type >= SourceRuleManager.TYPE_JS_START
)
