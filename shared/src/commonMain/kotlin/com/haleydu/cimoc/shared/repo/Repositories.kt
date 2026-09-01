package com.haleydu.cimoc.shared.repo

import com.haleydu.cimoc.shared.model.ChapterItem
import com.haleydu.cimoc.shared.model.ComicItem
import com.haleydu.cimoc.shared.model.LoadResult
import com.haleydu.cimoc.shared.model.PageImage
import com.haleydu.cimoc.shared.model.SourceItem
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
    suspend fun load(id: Long): ComicItem?
    suspend fun loadOrCreate(source: Int, cid: String): ComicItem
    suspend fun favorite(item: ComicItem)
    suspend fun unfavorite(id: Long)
    suspend fun deleteHistory(id: Long)
}

interface SourceRepository {
    suspend fun listAll(): List<SourceItem>
    suspend fun listEnable(): List<SourceItem>
    suspend fun setEnabled(type: Int, enable: Boolean)
    fun sourceTitle(type: Int): String
}

interface MangaRepository {
    fun search(sources: List<Int>, keyword: String, page: Int, strict: Boolean): Flow<ComicItem>
    suspend fun loadDetail(item: ComicItem): LoadResult<Pair<ComicItem, List<ChapterItem>>>
    suspend fun loadPages(item: ComicItem, chapter: ChapterItem): LoadResult<List<PageImage>>
    suspend fun resolveLazy(source: Int, url: String): String?
    fun imageHeaders(source: Int): Map<String, String>
    suspend fun markRead(item: ComicItem, path: String): Long
    suspend fun saveProgress(comicId: Long, page: Int)
}

interface ExploreRepository {
    suspend fun loadPage(source: Int, page: Int): LoadResult<List<ComicItem>>
}

interface DownloadRepository {
    suspend fun enqueue(item: ComicItem, chapters: List<ChapterItem>): LoadResult<Int>
}

interface SettingsRepository {
    fun bool(key: String, default: Boolean = false): Boolean
    fun setBool(key: String, value: Boolean)
    fun int(key: String, default: Int = 0): Int
    fun setInt(key: String, value: Int)
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
    val settings: SettingsRepository
)
