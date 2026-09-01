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
import com.haleydu.cimoc.data.TaskManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.service.DownloadService
import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.SourceItem
import com.haleydu.cimoc.shared.repo.CimocRepositories
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.DownloadRepository
import com.haleydu.cimoc.shared.repo.ExploreRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
import com.haleydu.cimoc.shared.repo.SettingsRepository
import com.haleydu.cimoc.shared.repo.SourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext

fun createCimocRepositories(
    context: Context,
    comicManager: ComicManager,
    chapterManager: ChapterManager,
    imageUrlManager: ImageUrlManager,
    sourceManager: SourceManager,
    mangaService: MangaService,
    sourceHealthManager: SourceHealthManager,
    sourceCatalogRefresher: SourceCatalogRefresher,
    taskManager: TaskManager,
    preferenceManager: PreferenceManager
): CimocRepositories {
    val comics = AndroidComicRepository(comicManager)
    val sources = AndroidSourceRepository(sourceManager, sourceHealthManager)
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
    return CimocRepositories(comics, sources, manga, explore, downloads, settings)
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
        listFavorite().drop(offset).take(limit)

    override suspend fun listHistoryPage(offset: Int, limit: Int): List<ComicItem> =
        listHistory().drop(offset).take(limit)

    override suspend fun listDownloadPage(offset: Int, limit: Int): List<ComicItem> =
        listDownload().drop(offset).take(limit)

    override suspend fun listLocalPage(offset: Int, limit: Int): List<ComicItem> =
        listLocal().drop(offset).take(limit)

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
    private val sourceHealthManager: SourceHealthManager
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
        return runCatching { sourceManager.getParser(type).getTitle() }.getOrDefault("")
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
    override fun search(sources: List<Int>, keyword: String, page: Int, strict: Boolean): Flow<ComicItem> = channelFlow {
        coroutineScope {
            val ordered = sourceHealthManager.sortTypes(sources)
            ordered.map { type ->
                async(Dispatchers.IO) {
                    try {
                        val parser = sourceManager.getParser(type)
                        mangaService.getSearchResult(parser, keyword, page, strict)
                            .catch { }
                            .collect { send(it.toItem()) }
                    } catch (_: Exception) {
                        sourceHealthManager.recordFailure(type, SourceHealthManager.KIND_PARSE)
                    }
                }
            }.awaitAll()
        }
    }

    override suspend fun loadDetail(item: ComicItem): LoadResult<Pair<ComicItem, List<ChapterItem>>> =
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
                sourceHealthManager.recordSuccess(
                    comic.source,
                    android.os.SystemClock.elapsedRealtime() - start
                )
                LoadResult.Ok(comic.toItem() to chapters.map { it.toItem() })
            } catch (e: Manga.NetworkErrorException) {
                sourceHealthManager.recordFailure(comic.source, SourceHealthManager.KIND_NETWORK)
                sourceCatalogRefresher.refreshAfterFailure(comic.source)
                LoadResult.Err("网络错误", network = true)
            } catch (e: Manga.ParseErrorException) {
                sourceHealthManager.recordFailure(comic.source, SourceHealthManager.KIND_PARSE)
                sourceCatalogRefresher.refreshAfterFailure(comic.source)
                LoadResult.Err("解析失败")
            } catch (e: Exception) {
                sourceHealthManager.recordFailure(comic.source, SourceHealthManager.KIND_PARSE)
                LoadResult.Err(e.message ?: "加载失败")
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

    private fun loadOfflinePages(item: ComicItem, comic: Comic?, chapter: Chapter): List<com.haleydu.cimoc.model.ImageUrl>? {
        val app = App.getApp()
        val isLocal = item.local || comic?.local == true
        if (isLocal) {
            val uri = chapter.path?.takeIf { it.isNotEmpty() } ?: item.cid
            val dir = openLocalDir(app, uri)
            return runCatching { Local.images(dir, chapter) }.getOrNull()?.takeIf { it.isNotEmpty() }
        }
        val title = runCatching { sourceManager.getParser(item.source).getTitle() }.getOrDefault("")
        val entity = comic ?: Comic(item.source, item.cid, item.title, item.cover, null, item.author)
        val downloaded = chapter.complete || comic?.download != null
        val hasDir = runCatching { Download.getChapterDir(app.documentFile, entity, chapter, title) }.getOrNull() != null
        if (downloaded || hasDir) {
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
}

private class AndroidExploreRepository(
    private val sourceManager: SourceManager,
    private val mangaService: MangaService,
    private val sourceHealthManager: SourceHealthManager
) : ExploreRepository {
    override suspend fun loadPage(source: Int, page: Int): LoadResult<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val parser = sourceManager.getParser(source)
            val category = parser.getCategory()
            val format = if (category != null) {
                category.getFormat("")
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
                for (chapter in chapters) {
                    if (chapter.path.isEmpty()) continue
                    val task = Task(null, id, chapter.path, chapter.title, 0, 0)
                    task.source = comic.source
                    task.cid = comic.cid
                    task.state = Task.STATE_WAIT
                    taskManager.insert(task)
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
    invalid = invalid
)
