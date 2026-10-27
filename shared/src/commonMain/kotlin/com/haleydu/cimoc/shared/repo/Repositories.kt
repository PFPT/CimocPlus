package com.haleydu.cimoc.shared.repo

import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.DownloadTaskItem
import com.haleydu.cimoc.shared.model.ExploreFilter
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.SearchStats
import com.haleydu.cimoc.shared.model.SourceItem
import com.haleydu.cimoc.shared.model.TagItem
import kotlinx.coroutines.flow.Flow

interface ComicRepository {
    suspend fun listFavorite(): List<ComicItem>
    suspend fun listHistory(): List<ComicItem>
    suspend fun listDownload(): List<ComicItem>
    suspend fun listLocal(): List<ComicItem>
    suspend fun listFavoritePage(offset: Int, limit: Int): List<ComicItem>
    suspend fun listHistoryPage(offset: Int, limit: Int): List<ComicItem>
    suspend fun listDownloadPage(offset: Int, limit: Int): List<ComicItem>
    suspend fun listLocalPage(offset: Int, limit: Int): List<ComicItem>
    suspend fun listContinuePage(offset: Int, limit: Int): List<ComicItem>
    suspend fun listFinishPage(offset: Int, limit: Int): List<ComicItem>
    suspend fun listFavoriteByTagPage(tid: Long, offset: Int, limit: Int): List<ComicItem>
    suspend fun load(id: Long): ComicItem?
    suspend fun loadOrCreate(source: Int, cid: String): ComicItem
    suspend fun favorite(item: ComicItem)
    suspend fun unfavorite(id: Long)
    suspend fun deleteHistory(id: Long)
    suspend fun clearHighlight(id: Long)
}

interface SourceRepository {
    suspend fun listAll(): List<SourceItem>
    suspend fun listEnable(): List<SourceItem>
    suspend fun setEnabled(type: Int, enable: Boolean)
    fun sourceTitle(type: Int): String
    suspend fun homepage(type: Int): String
    fun warm()
    suspend fun import(input: String): Int
    suspend fun removeCustom(type: Int)
}

interface MangaRepository {
    fun search(
        sources: List<Int>,
        keyword: String,
        page: Int,
        strict: Boolean,
        stats: SearchStats? = null,
        recordHealth: Boolean = true
    ): Flow<ComicItem>
    suspend fun loadRank(): LoadResult<List<ComicItem>>
    suspend fun loadDetail(item: ComicItem, recordHealth: Boolean = true): LoadResult<Pair<ComicItem, List<ChapterItem>>>
    suspend fun loadPages(item: ComicItem, chapter: ChapterItem): LoadResult<List<PageImage>>
    suspend fun resolveLazy(source: Int, url: String): String?
    fun imageHeaders(source: Int): Map<String, String>
    suspend fun markRead(item: ComicItem, path: String): Long
    suspend fun saveProgress(comicId: Long, page: Int)
    suspend fun checkFavorites(): Int
    suspend fun matchUrl(url: String): ComicItem?
}

interface ExploreRepository {
    suspend fun loadFilters(source: Int): List<ExploreFilter>
    suspend fun loadPage(source: Int, page: Int, args: List<String> = emptyList()): LoadResult<List<ComicItem>>
}

interface DownloadRepository {
    suspend fun enqueue(item: ComicItem, chapters: List<ChapterItem>): LoadResult<Int>
    fun observeTasks(): Flow<List<DownloadTaskItem>>
    suspend fun pause(id: Long)
    suspend fun retry(id: Long)
    suspend fun delete(id: Long)
    suspend fun delete(ids: List<Long>)
}

interface TagRepository {
    suspend fun list(): List<TagItem>
    suspend fun create(title: String): TagItem?
    suspend fun delete(id: Long)
    suspend fun tagsOf(comicId: Long): List<TagItem>
    suspend fun setTags(comicId: Long, ids: List<Long>)
}

interface SettingsRepository {
    fun bool(key: String, default: Boolean = false): Boolean
    fun setBool(key: String, value: Boolean)
    fun int(key: String, default: Int = 0): Int
    fun setInt(key: String, value: Int)
    fun string(key: String, default: String = ""): String
    fun setString(key: String, value: String)
    suspend fun backupFavorites(): LoadResult<Int>
    suspend fun restoreFavorites(filename: String): LoadResult<Unit>
    suspend fun listBackupFiles(): List<String>
}

data class CimocRepositories(
    val comics: ComicRepository,
    val sources: SourceRepository,
    val manga: MangaRepository,
    val explore: ExploreRepository,
    val downloads: DownloadRepository,
    val settings: SettingsRepository,
    val tags: TagRepository
)
