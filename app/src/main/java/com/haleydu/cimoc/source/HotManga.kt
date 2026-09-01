package com.haleydu.cimoc.source

import com.google.common.collect.Lists
import com.haleydu.cimoc.core.Manga.Companion.getResponseBody
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.JsonIterator
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import taobe.tec.jcc.JChineseConvertor
import java.util.LinkedList

class HotManga(
    source: Source?,
    private val httpClient: OkHttpClient,
    private val sourceConfigManager: SourceConfigManager
) : MangaParser() {

    init {
        init(source, null)
    }

    private fun apiRequest(path: String): Request {
        val hosts = sourceConfigManager.getHotMangaHosts()
        val primary = hosts[0] + path
        val builder = Request.Builder()
            .url(primary)
            .addHeader("User-Agent", "COPY/3.0.0")
            .addHeader("Accept", "application/json")
            .addHeader("version", sourceConfigManager.getCopyVersion())
            .addHeader("platform", "1")
            .addHeader("webp", "1")
            .addHeader("region", "1")
            .addHeader("Referer", webBase() + "/")
        if (hosts.size > 1) {
            val fallback = StringBuilder()
            for (i in 1 until hosts.size) {
                if (fallback.isNotEmpty()) {
                    fallback.append(',')
                }
                fallback.append(hosts[i]).append(path)
            }
            builder.header("X-Cimoc-Fallback", fallback.toString())
        }
        return builder.build()
    }

    private fun webBase(): String {
        val hosts = sourceConfigManager.getHotMangaHosts()
        return if (hosts.isEmpty()) "https://www.relamanhua.com" else hosts[0]
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page == 1) {
            val path = StringUtils.format("/api/v3/search/comic?platform=1&limit=30&offset=0&q=%s", keyword)
            return apiRequest(path)
        }
        return null
    }

    override fun getUrl(cid: String): String {
        return webBase() + "/comic/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("relamanhua.com", "/comic/(\\w.+)"))
        filter.add(UrlFilter("manga2020.com", "/comic/(\\w.+)"))
        filter.add(UrlFilter("relam.cn", "/comic/(\\w.+)"))
        filter.add(UrlFilter("relamanhua.xyz", "/comic/(\\w.+)"))
        filter.add(UrlFilter("hotmangasg.com", "/comic/(\\w.+)"))
    }

    @Throws(JSONException::class)
    override fun getSearchIterator(html: String, page: Int): SearchIterator? {
        try {
            val jsonObject = JSONObject(html)
            return object : JsonIterator(jsonObject.getJSONObject("results").getJSONArray("list")) {
                override fun parse(`object`: JSONObject): Comic? {
                    return try {
                        val jChineseConvertor = JChineseConvertor.getInstance()
                        val cid = `object`.getString("path_word")
                        val title = jChineseConvertor.t2s(`object`.getString("name"))
                        val cover = `object`.getString("cover")
                        val author = `object`.getJSONArray("author").getJSONObject(0).getString("name").trim()
                        Comic(TYPE, cid, title, cover, null, author)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun getInfoRequest(cid: String): Request {
        return apiRequest("/api/v3/comic2/".plus(cid))
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        try {
            val comicInfo = JSONObject(html).getJSONObject("results")
            val body = comicInfo.getJSONObject("comic")
            val cover = body.getString("cover")
            val intro = body.getString("brief")
            val title = body.getString("name")
            val update = body.getString("datetime_updated")
            val author = (body.getJSONArray("author").get(0) as JSONObject).getString("name")
            val finish = body.getJSONObject("status").getInt("value") != 0
            val group = comicInfo.getJSONObject("groups")
            comic.note = group
            comic.setInfo(title, cover, update, intro, author, finish)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return comic
    }

    override fun getChapterRequest(html: String, cid: String): Request {
        return apiRequest(String.format("/api/v3/comic/%s/group/default/chapters?limit=500&offset=0", cid))
    }

    @Throws(JSONException::class)
    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var jsonObject = JSONObject(html)
        var array = jsonObject.getJSONObject("results").getJSONArray("list")
        for (i in 0 until array.length()) {
            val title = array.getJSONObject(i).getString("name")
            val path = array.getJSONObject(i).getString("uuid")
            list.add(Chapter((sourceComic.toString() + "000" + i).toLong(), sourceComic, title, path, "默认"))
        }
        try {
            val groups = comic.note as JSONObject
            val keys = groups.keys()
            var htmlBody = html
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == "default") continue
                val path_word = groups.getJSONObject(key).getString("path_word")
                val PathName = groups.getJSONObject(key).getString("name")
                htmlBody = getResponseBody(httpClient, apiRequest(String.format("/api/v3/comic/%s/group/%s/chapters?limit=500&offset=0", comic.cid, path_word)))
                jsonObject = JSONObject(htmlBody)
                array = jsonObject.getJSONObject("results").getJSONArray("list")
                for (i in 0 until array.length()) {
                    val title = array.getJSONObject(i).getString("name")
                    val path = array.getJSONObject(i).getString("uuid")
                    list.add(Chapter((sourceComic.toString() + "000" + i).toLong(), sourceComic, title, path, PathName))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Lists.reverse(list)
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        return apiRequest(StringUtils.format("/api/v3/comic/%s/chapter/%s", cid, path))
    }

    @Throws(JSONException::class)
    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val jsonObject = JSONObject(html)
        val array = jsonObject.getJSONObject("results").getJSONObject("chapter").getJSONArray("contents")
        try {
            for (i in 0 until array.length()) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                val url = array.getJSONObject(i).getString("url").replace("m_read", "kb_m_read_large")
                list.add(ImageUrl(id, comicChapter, i + 1, url, false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        try {
            val comicInfo = JSONObject(html).getJSONObject("results")
            val body = comicInfo.getJSONObject("comic")
            return body.getString("datetime_updated")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return ""
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", webBase() + "/", "User-Agent", "COPY/3.0.0")
    }

    companion object {
        const val TYPE = 102
        const val DEFAULT_TITLE = "热辣漫画"
        const val website = "https://www.relamanhua.com/"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
