package com.haleydu.cimoc.source

import android.util.Pair
import com.google.common.collect.Lists
import com.haleydu.cimoc.core.Manga.Companion.getResponseBody
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_AREA
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.JsonIterator
import com.haleydu.cimoc.parser.MangaCategory
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

class CopyMH(
    source: Source?,
    private val sourceConfigManager: SourceConfigManager,
    private val httpClient: OkHttpClient
) : MangaParser() {

    init {
        init(source, Category())
    }

    private fun apiRequest(path: String): Request {
        val hosts = sourceConfigManager.getCopyApiHosts()
        val primary = hosts[0] + path
        val builder = Request.Builder()
            .url(primary)
            .addHeader("User-Agent", "COPY/3.0.0")
            .addHeader("Accept", "application/json")
            .addHeader("version", sourceConfigManager.getCopyVersion())
            .addHeader("platform", "1")
            .addHeader("webp", "1")
            .addHeader("region", "1")
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
        return sourceConfigManager.getCopyWebBase()
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        val path = StringUtils.format(
            "/api/v3/search/comic?platform=1&limit=30&offset=%d&q=%s",
            (page - 1) * 30, keyword
        )
        val hosts = sourceConfigManager.getCopyWebHosts()
        val primary = hosts[0] + path
        val builder = Request.Builder()
            .url(primary)
            .addHeader("User-Agent", "COPY/3.0.0")
            .addHeader("Accept", "application/json")
            .addHeader("version", sourceConfigManager.getCopyVersion())
            .addHeader("platform", "1")
            .addHeader("webp", "1")
            .addHeader("region", "1")
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

    override fun getUrl(cid: String): String {
        return webBase() + "/comic/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("copy3000.com", "\\w+", 0))
        filter.add(UrlFilter("2026copy.com", "\\w+", 0))
        filter.add(UrlFilter("2025copy.com", "\\w+", 0))
        filter.add(UrlFilter("mangacopy.com", "\\w+", 0))
        filter.add(UrlFilter("copymanga.com", "\\w+", 0))
        filter.add(UrlFilter("copy20.com", "\\w+", 0))
        filter.add(UrlFilter("copy2000.site", "\\w+", 0))
        filter.add(UrlFilter("copy2000.online", "\\w+", 0))
    }

    @Throws(JSONException::class)
    override fun getSearchIterator(html: String, page: Int): SearchIterator? {
        try {
            val jsonObject = JSONObject(html)
            return object : JsonIterator(jsonObject.getJSONObject("results").getJSONArray("list")) {
                override fun parse(`object`: JSONObject): Comic? {
                    try {
                        val jChineseConvertor = JChineseConvertor.getInstance()
                        val cid = `object`.getString("path_word")
                        val title = jChineseConvertor.t2s(`object`.getString("name"))
                        val cover = `object`.getString("cover")
                        val author = `object`.getJSONArray("author").getJSONObject(0).getString("name").trim()
                        return Comic(TYPE, cid, title, cover, null, author)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun getInfoRequest(cid: String): Request {
        return apiRequest("/api/v3/comic2/".plus(cid) + "?platform=1")
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
        return apiRequest(String.format("/api/v3/comic/%s/group/default/chapters?limit=500&offset=0&platform=1", cid))
    }

    @Throws(JSONException::class)
    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var jsonObject = JSONObject(html)
        var array = jsonObject.getJSONObject("results").getJSONArray("list")
        var index = 0
        for (i in 0 until array.length()) {
            val title = array.getJSONObject(i).getString("name")
            val path = array.getJSONObject(i).getString("uuid")
            list.add(Chapter((sourceComic.toString() + "000" + index++).toLong(), sourceComic, title, path, "默认"))
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
                htmlBody = getResponseBody(httpClient, apiRequest(String.format("/api/v3/comic/%s/group/%s/chapters?limit=500&offset=0&platform=1", comic.cid, path_word)))
                jsonObject = JSONObject(htmlBody)
                array = jsonObject.getJSONObject("results").getJSONArray("list")
                for (i in 0 until array.length()) {
                    val title = array.getJSONObject(i).getString("name")
                    val path = array.getJSONObject(i).getString("uuid")
                    list.add(Chapter((sourceComic.toString() + "000" + index++).toLong(), sourceComic, title, path, PathName))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Lists.reverse(list)
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        return apiRequest(StringUtils.format("/api/v3/comic/%s/chapter2/%s?platform=1", cid, path))
    }

    @Throws(JSONException::class)
    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val jsonObject = JSONObject(html)
        val array = jsonObject.getJSONObject("results").getJSONObject("chapter").getJSONArray("contents")
        for (i in 0 until array.length()) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            val url = array.getJSONObject(i).getString("url")
            list.add(ImageUrl(id, comicChapter, i + 1, url, false))
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

    override fun getCategoryRequest(format: String, page: Int): Request {
        val offset = Math.max(page - 1, 0) * 30
        val path = if (format.contains("%d")) StringUtils.format(format, offset) else format
        return apiRequest(path)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        try {
            val array = JSONObject(html).getJSONObject("results").getJSONArray("list")
            val convertor = JChineseConvertor.getInstance()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val cid = obj.getString("path_word")
                val title = convertor.t2s(obj.getString("name"))
                val cover = obj.getString("cover")
                var author: String? = null
                val authors = obj.optJSONArray("author")
                if (authors != null && authors.length() > 0) {
                    author = authors.getJSONObject(0).optString("name").trim()
                }
                val update = obj.optString("datetime_updated")
                list.add(Comic(TYPE, cid, title, cover, update, author))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            val theme = value(args, CATEGORY_SUBJECT)
            val top = value(args, CATEGORY_AREA)
            val status = value(args, CATEGORY_PROGRESS)
            var ordering = value(args, CATEGORY_ORDER)
            if (ordering.isEmpty()) {
                ordering = "-datetime_updated"
            }
            val builder = StringBuilder("/api/v3/comics?limit=30&offset=%d&platform=1&ordering=")
            builder.append(ordering)
            if (theme.isNotEmpty()) {
                builder.append("&theme=").append(theme)
            }
            if (top.isNotEmpty()) {
                builder.append("&top=").append(top)
            }
            if (status.isNotEmpty()) {
                builder.append("&status=").append(status)
            }
            return builder.toString()
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("爱情", "aiqing"))
            list.add(Pair.create("冒险", "maoxian"))
            list.add(Pair.create("奇幻", "qihuan"))
            list.add(Pair.create("百合", "baihe"))
            list.add(Pair.create("校园", "xiaoyuan"))
            list.add(Pair.create("科幻", "kehuan"))
            list.add(Pair.create("生活", "shenghuo"))
            list.add(Pair.create("热血", "rexue"))
            list.add(Pair.create("搞笑", "gaoxiao"))
            list.add(Pair.create("都市", "dushi"))
            list.add(Pair.create("悬疑", "xuanyi"))
            list.add(Pair.create("仙侠", "xianxia"))
            list.add(Pair.create("恐怖", "kongbu"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("日漫", "japan"))
            list.add(Pair.create("韩漫", "korea"))
            list.add(Pair.create("美漫", "west"))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("连载中", "1"))
            list.add(Pair.create("已完结", "2"))
            return list
        }

        override fun hasOrder(): Boolean {
            return true
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("更新", "-datetime_updated"))
            list.add(Pair.create("热门", "-popular"))
            return list
        }

        private fun value(args: Array<out String>?, index: Int): String {
            if (args == null || index >= args.size || args[index] == null) {
                return ""
            }
            return args[index]
        }
    }

    companion object {
        const val TYPE = 26
        const val DEFAULT_TITLE = "拷贝漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
