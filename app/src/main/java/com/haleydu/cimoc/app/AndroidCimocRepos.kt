package com.haleydu.cimoc.app

import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.core.MangaService
import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.data.ComicManager
import com.haleydu.cimoc.data.ImageUrlManager
import com.haleydu.cimoc.data.SourceCatalogRefresher
import com.haleydu.cimoc.data.SourceHealthManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.SourceItem
import com.haleydu.cimoc.shared.repo.CimocRepositories
import com.haleydu.cimoc.shared.repo.ComicRepository
import com.haleydu.cimoc.shared.repo.MangaRepository
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
    comicManager: ComicManager,
    chapterManager: ChapterManager,
    imageUrlManager: ImageUrlManager,
    sourceManager: SourceManager,
    mangaService: MangaService,
    sourceHealthManager: SourceHealthManager,
    sourceCatalogRefresher: SourceCatalogRefresher
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
    return CimocRepositories(comics, sources, manga)
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
            try {
                val parser = sourceManager.getParser(item.source)
                val entity = Chapter(chapter.title, chapter.path)
                entity.id = chapter.id
                entity.sourceComic = chapter.sourceComic
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
