package com.haleydu.cimoc.core

import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.data.SourceManager
import com.haleydu.cimoc.source.Tencent
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.parser.Parser
import com.haleydu.cimoc.script.JsMangaParser
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val sourceConfigManager: SourceConfigManager
) {

    private fun indexOfIgnoreCase(str: String, search: String): Boolean {
        return str.lowercase().indexOf(search.lowercase()) != -1
    }

    fun getSearchResult(parser: Parser, keyword: String, page: Int, strictSearch: Boolean): Flow<Comic> = flow {
        if (parser is JsMangaParser) {
            val list = parser.search(keyword, page)
            if (list.isEmpty()) {
                return@flow
            }
            for (comic in list) {
                if (indexOfIgnoreCase(comic.title ?: "", keyword)
                    || indexOfIgnoreCase(comic.author ?: "", keyword)
                    || !strictSearch
                ) {
                    emit(comic)
                }
            }
            return@flow
        }
        val request = Manga.applyHeaders(parser.getSearchRequest(keyword, page), parser.getHeader()) ?: return@flow
        val html = Manga.getResponseBody(httpClient, request)
        val iterator = parser.getSearchIterator(html, page)
        if (iterator == null || iterator.empty()) {
            return@flow
        }
        while (iterator.hasNext()) {
            val comic = iterator.next()
            if (comic != null && (indexOfIgnoreCase(comic.title ?: "", keyword)
                        || indexOfIgnoreCase(comic.author ?: "", keyword)
                        || !strictSearch)
            ) {
                emit(comic)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getComicInfo(parser: Parser, comic: Comic): List<Chapter> {
        if (parser is JsMangaParser) {
            comic.url = parser.getUrl(comic.cid)
            parser.fillInfo(comic)
            AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_UPDATE_INFO, comic))
            val sourceComic = (comic.source.toString() + "000" + (comic.id ?: "00")).toLong()
            val list = parser.chapterList(comic, sourceComic)
            if (list.isEmpty()) {
                throw Manga.ParseErrorException()
            }
            return list
        }
        comic.url = parser.getUrl(comic.cid)
        var request = Manga.applyHeaders(parser.getInfoRequest(comic.cid), parser.getHeader())
        if (request == null) {
            throw Manga.ParseErrorException()
        }
        var html = Manga.getResponseBody(httpClient, request)
        val newComic = parser.parseInfo(html, comic)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_UPDATE_INFO, newComic))
        request = Manga.applyHeaders(parser.getChapterRequest(html, comic.cid), parser.getHeader())
        if (request != null) {
            html = Manga.getResponseBody(httpClient, request)
        }
        val sourceComic = (comic.source.toString() + "000" + (comic.id ?: "00")).toLong()
        val list = parser.parseChapter(html, comic, sourceComic) ?: parser.parseChapter(html)
        if (list.isNullOrEmpty()) {
            throw Manga.ParseErrorException()
        }
        return list
    }

    fun getCategoryComic(parser: Parser, format: String, page: Int): List<Comic> {
        if (parser is JsMangaParser) {
            return parser.category(format, page)
        }
        val request = Manga.applyHeaders(parser.getCategoryRequest(format, page), parser.getHeader())
        val html = Manga.getResponseBody(httpClient, request)
        return parser.parseCategory(html, page) ?: emptyList()
    }

    suspend fun getChapterImage(chapter: Chapter, parser: Parser, cid: String, path: String): List<ImageUrl> =
        withContext(Dispatchers.IO) {
            if (parser is JsMangaParser) {
                val jsList = parser.imageList(cid, path, chapter)
                if (jsList.isEmpty()) {
                    throw Manga.ParseErrorException()
                }
                for (imageUrl in jsList) {
                    imageUrl.chapter = path
                }
                return@withContext jsList
            }
            val request = Manga.applyHeaders(parser.getImagesRequest(cid, path), parser.getHeader())
            val html = Manga.getResponseBody(httpClient, request)
            var list = parser.parseImages(html, chapter)
            if (list == null || list.isEmpty()) {
                list = parser.parseImages(html)
            }
            if (list == null || list.isEmpty()) {
                throw Manga.ParseErrorException()
            }
            for (imageUrl in list) {
                imageUrl.chapter = path
            }
            list
        }

    suspend fun loadLazyUrl(parser: Parser, url: String): String? = withContext(Dispatchers.IO) {
        val request = Manga.applyHeaders(parser.getLazyRequest(url), parser.getHeader())
        try {
            parser.parseLazy(Manga.getResponseBody(httpClient, request), url)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadTencentRank(): List<Comic> {
        val host = sourceConfigManager.firstUrl("https://m.ac.qq.com", "TENCENTM", "TENCENT")
        val request = sourceConfigManager.hostPathRequest(
            "/rank/zhenxiang",
            "https://m.ac.qq.com",
            "TENCENTM",
            "TENCENT"
        ).newBuilder()
            .header("Referer", host)
            .header("User-Agent", TENCENT_UA)
            .build()
        val html = Manga.getResponseBody(httpClient, request)
        return parseTencentHotRank(html)
    }

    private fun parseTencentHotRank(html: String): List<Comic> {
        val doc = Jsoup.parse(html)
        val list = doc.getElementById("list_hot") ?: throw Manga.ParseErrorException()
        val top = doc.select(".top3-box[data-type=hot] .top3-box-item")
        val rows = list.select(".comic-item")
        if (top.isEmpty() && rows.isEmpty()) throw Manga.ParseErrorException()
        val orderedTop = top.sortedBy { element ->
            val name = element.className()
            when {
                name.contains("top3-box-item1") -> 1
                name.contains("top3-box-item2") -> 2
                name.contains("top3-box-item3") -> 3
                else -> 9
            }
        }
        val comics = ArrayList<Comic>()
        val seen = HashSet<String>()
        fun add(element: Element) {
            val href = element.selectFirst("a.comic-link")?.attr("href").orEmpty()
            val marker = "/comic/index/id/"
            val index = href.indexOf(marker)
            if (index < 0) return
            val cid = href.substring(index + marker.length).substringBefore('/').substringBefore('?')
            if (cid.isEmpty() || !seen.add(cid)) return
            val title = element.selectFirst(".comic-title")?.text()?.trim().orEmpty()
            if (title.isEmpty()) return
            var cover = element.selectFirst("img")?.attr("src").orEmpty()
            if (cover.startsWith("//")) cover = "https:$cover"
            comics.add(Comic(Tencent.TYPE, cid, title, cover, null, null))
        }
        orderedTop.forEach { add(it) }
        rows.forEach { add(it) }
        if (comics.isEmpty()) throw Manga.ParseErrorException()
        return comics
    }

    suspend fun loadAutoComplete(keyword: String): List<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("http://m.ac.qq.com/search/smart?word=$keyword")
            .build()
        val jsonString = Manga.getResponseBody(httpClient, request)
        val jsonObject = JSONObject(jsonString)
        val array = jsonObject.getJSONArray("data")
        val list = ArrayList<String>()
        for (i in 0 until array.length()) {
            list.add(array.getJSONObject(i).getString("title"))
        }
        list
    }

    fun checkUpdate(manager: SourceManager, list: List<Comic>): Flow<Comic?> = channelFlow {
        val client = OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build()
        val gate = Semaphore(4)
        coroutineScope {
            list.map { comic ->
                async {
                    gate.acquire()
                    try {
                        val updated = try {
                            val parser = manager.getParser(comic.source)
                            val update = if (parser is JsMangaParser) {
                                parser.checkUpdate(comic.cid)
                            } else {
                                val request = Manga.applyHeaders(parser.getCheckRequest(comic.cid), parser.getHeader())
                                parser.parseCheck(Manga.getResponseBody(client, request))
                            }
                            if (comic.update != null && update != null && comic.update != update) {
                                comic.favorite = System.currentTimeMillis()
                                comic.update = update
                                comic.highlight = true
                                comic
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                            null
                        }
                        send(updated)
                    } finally {
                        gate.release()
                    }
                }
            }.awaitAll()
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val TENCENT_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
