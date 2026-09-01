package com.haleydu.cimoc.source

import android.annotation.SuppressLint
import com.haleydu.cimoc.core.Manga
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
import com.haleydu.cimoc.utils.UicodeBackslashU
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale

class DmzjFix(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, null)
    }

    private fun mobileHost(): String {
        return sourceConfigManager.firstUrl("https://m.dmzj.com", "DMZJV2", "DongManZhiJia", "DMZJ", "DMZJFIX")
    }

    private fun pictureHost(): String {
        return sourceConfigManager.firstUrl("https://images.dmzj.com", "DMZJFIXPICTURE", "DongManZhiJia")
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("manhua.dmzj.com", "/(\\w+)"))
        filter.add(UrlFilter("m.dmzj.com", "/info/(\\w+).html"))
    }

    @Throws(UnsupportedEncodingException::class, Exception::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        val suffix = StringUtils.format("/search/show/0/%s/%d.json", encoded, Math.max(page - 1, 0))
        val hosts = sourceConfigManager.listUrls(
            "https://v3api.idmzj.com", "DMZJV2SERVER", "DMZJSERVER"
        ).toMutableList()
        if (!hosts.contains("https://v3api.dmzj.com")) {
            hosts.add("https://v3api.dmzj.com")
        }
        if (!hosts.contains("http://v3api.dmzj1.com")) {
            hosts.add("http://v3api.dmzj1.com")
        }
        val host = if (hosts.isEmpty()) "https://v3api.idmzj.com" else hosts[0]
        val builder = Request.Builder()
            .url(host + suffix)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        sourceConfigManager.applyHostFallbacks(builder, suffix, hosts)
        return builder.build()
    }

    override fun getUrl(cid: String): String {
        return StringUtils.format("%s/info/%s.html", mobileHost(), cid)
    }

    @Throws(JSONException::class)
    override fun getSearchIterator(html: String, page: Int): SearchIterator? {
        try {
            val array = searchArray(html) ?: return null
            return object : JsonIterator(array) {
                override fun parse(`object`: JSONObject): Comic? {
                    return try {
                        val cid = `object`.optString("id")
                        if (cid.isEmpty()) {
                            return null
                        }
                        val title = firstNonEmpty(`object`, "title", "name", "comic_name")
                        var cover = firstNonEmpty(`object`, "cover", "comic_cover")
                        if (cover.isNotEmpty() && !cover.startsWith("http") && !cover.startsWith("//")) {
                            cover = pictureHost() + "/" + cover
                        } else if (cover.startsWith("//")) {
                            cover = "https:$cover"
                        }
                        val author = firstNonEmpty(`object`, "authors", "author", "comic_author")
                        var update = `object`.optString("last_name")
                        if (`object`.has("last_updatetime")) {
                            try {
                                val time = java.lang.Long.parseLong(`object`.getString("last_updatetime")) * 1000
                                update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
                            } catch (_: Exception) {
                            }
                        }
                        Comic(TYPE, cid, title, cover, update, author)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(JSONException::class)
    private fun searchArray(html: String?): JSONArray? {
        if (html == null) {
            return null
        }
        val trimmed = html.trim()
        if (trimmed.startsWith("[")) {
            return JSONArray(trimmed)
        }
        var embedded = StringUtils.match("var serchArry=(\\[\\{.*?\\}\\])", html, 1)
        if (embedded == null) {
            embedded = StringUtils.match("var g_search_data = (.*?);", html, 1)
        }
        if (embedded == null) {
            return null
        }
        return JSONArray(UicodeBackslashU.unicodeToCn(embedded).replace("\\/", "/"))
    }

    override fun getInfoRequest(cid: String): Request {
        val suffix = StringUtils.format("/dynamic/comicinfo/%s.json", cid)
        return sourceConfigManager.hostPathRequest(suffix, "http://api.dmzj.com", "DMZJFIXSERVER", "DongManZhiJia")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", pictureHost() + "/")
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        try {
            val root = JSONObject(html).getJSONObject("data")
            val info = root.getJSONObject("info")
            val title = info.getString("title")
            val status = !info.getString("status").contains("连载")
            val cover = info.getString("cover")
            val author = info.getString("authors")
            var update = info.getString("last_updatetime")
            update = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(Integer.parseInt(update) * 1000L))
            val intro = info.getString("description")
            comic.setInfo(title, cover, update, intro, author, status)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter>? {
        val list = LinkedList<Chapter>()
        val list1 = LinkedList<Chapter>()
        try {
            val root = JSONObject(html).getJSONObject("data").getJSONArray("list")
            for (i in 0 until root.length()) {
                val chapter = root.getJSONObject(i)
                val title = chapter.getString("chapter_name")
                val comic_id = chapter.getString("comic_id")
                val chapter_id = chapter.getString("id")
                val path = "$comic_id/$chapter_id"
                list.add(Chapter((sourceComic.toString() + "000" + i + 1).toLong(), sourceComic, title, path, "默认线路"))
                list1.add(Chapter((sourceComic.toString() + "001" + i + 1).toLong(), sourceComic, title.plus(" (备用)"), path + "x", "备用线路"))
            }
            list.addAll(list1)
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val suffix = StringUtils.format("/chapinfo/%s.html", path.replace("x", ""))
        return sourceConfigManager.hostPathRequest(suffix, "https://m.dmzj.com", "DMZJV2", "DongManZhiJia", "DMZJ")
    }

    @Throws(Manga.NetworkErrorException::class, JSONException::class)
    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val root = JSONObject(html).getJSONArray("page_url")
        val flag = chapter.id.toString().replace(chapter.sourceComic.toString(), "")
        for (i in 0 until root.length()) {
            val comicChapter = chapter.id
            var url = root.getString(i)
            val id = (comicChapter.toString() + "000" + i).toLong()
            if (flag.startsWith("001")) {
                url = url.replace("dmzj", "dmzj1")
            }
            list.add(ImageUrl(id, comicChapter, i + 1, url, false))
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    @SuppressLint("SimpleDateFormat")
    override fun parseCheck(html: String): String? {
        try {
            val update = JSONObject(html).getJSONObject("data").getString("last_updatetime")
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(Integer.parseInt(update) * 1000L))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        const val TYPE = 100
        const val DEFAULT_TITLE = "动漫之家v2Fix"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }

        private fun firstNonEmpty(`object`: JSONObject, vararg keys: String): String {
            for (key in keys) {
                val value = `object`.optString(key, "")
                if (!value.isNullOrEmpty() && value != "null") {
                    return value
                }
            }
            return ""
        }
    }
}
