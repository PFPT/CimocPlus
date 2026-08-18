package com.haleydu.cimoc.core

import com.haleydu.cimoc.manager.SourceManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.parser.Parser
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Random
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaService @Inject constructor(
    private val httpClient: OkHttpClient
) {

    private fun indexOfIgnoreCase(str: String, search: String): Boolean {
        return str.lowercase().indexOf(search.lowercase()) != -1
    }

    fun getSearchResult(parser: Parser, keyword: String, page: Int, strictSearch: Boolean): Flow<Comic> = flow {
        val request = parser.getSearchRequest(keyword, page) ?: return@flow
        val random = Random()
        val html = Manga.getResponseBody(httpClient, request)
        val iterator = parser.getSearchIterator(html, page)
        if (iterator == null || iterator.empty()) {
            throw Exception()
        }
        while (iterator.hasNext()) {
            val comic = iterator.next()
            if (comic != null && (indexOfIgnoreCase(comic.title ?: "", keyword)
                        || indexOfIgnoreCase(comic.author ?: "", keyword)
                        || !strictSearch)
            ) {
                emit(comic)
                Thread.sleep(random.nextInt(200).toLong())
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getComicInfo(parser: Parser, comic: Comic): List<Chapter> {
        comic.url = parser.getUrl(comic.cid)
        var request = parser.getInfoRequest(comic.cid)
        var html = Manga.getResponseBody(httpClient, request)
        val newComic = parser.parseInfo(html, comic)
        AppEventBus.post(AppEvent(AppEvent.EVENT_COMIC_UPDATE_INFO, newComic))
        request = parser.getChapterRequest(html, comic.cid)
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
        val request = parser.getCategoryRequest(format, page)
        val html = Manga.getResponseBody(httpClient, request)
        val list = parser.parseCategory(html, page)
        if (list.isEmpty()) {
            throw Exception()
        }
        return list
    }

    suspend fun getChapterImage(chapter: Chapter, parser: Parser, cid: String, path: String): List<ImageUrl> =
        withContext(Dispatchers.IO) {
            val request = parser.getImagesRequest(cid, path)
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
        val request = parser.getLazyRequest(url)
        try {
            parser.parseLazy(Manga.getResponseBody(httpClient, request), url)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    fun checkUpdate(manager: SourceManager, list: List<Comic>): Flow<Comic?> = flow {
        val client = OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build()
        for (comic in list) {
            try {
                val parser = manager.getParser(comic.source)
                val request = parser.getCheckRequest(comic.cid)
                val update = parser.parseCheck(Manga.getResponseBody(client, request))
                if (comic.update != null && update != null && comic.update != update) {
                    comic.favorite = System.currentTimeMillis()
                    comic.update = update
                    comic.highlight = true
                    emit(comic)
                    continue
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
}
