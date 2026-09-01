package com.haleydu.cimoc.source

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
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.LinkedList

class KuaiKan(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        var src = source
        if (src == null) {
            src = getDefaultSource()
        }
        init(src, null)
    }

    private fun webHost(): String {
        return sourceConfigManager.firstUrl("https://www.kuaikanmanhua.com", "KUAIKANMANHUA")
    }

    private fun apiHost(): String {
        return sourceConfigManager.firstUrl("https://api.kkmh.com", "KUAIKANMANHUASERVER", "KUAIKANMANHUA")
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("kuaikanmanhua.com"))
        filter.add(UrlFilter("kkmh.com"))
    }

    @Throws(Exception::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val offset = Math.max(page - 1, 0) * 20
        val url = StringUtils.format(
            "%s/v1/search/topic?q=%s&offset=%d&limit=20",
            apiHost(), encoded, offset
        )
        return apiGet(url)
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        var array = jsonArray(html, "hit")
        if (array == null) {
            array = JSONArray()
        }
        return object : JsonIterator(array) {
            override fun parse(`object`: JSONObject): Comic? {
                val cid = `object`.optString("id")
                if (cid.isEmpty() || cid == "0") {
                    return null
                }
                val title = `object`.optString("title")
                val cover = firstNonEmpty(`object`, "vertical_image_url", "cover_image_url")
                var author = ""
                val user = `object`.optJSONObject("user")
                if (user != null) {
                    author = user.optString("nickname")
                }
                return Comic(TYPE, cid, title, cover, null, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return webHost() + "/web/topic/" + cid
    }

    override fun getInfoRequest(cid: String): Request {
        return apiGet(webHost() + "/v2/pweb/topic/" + cid)
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        try {
            val info = topicInfo(html) ?: return comic
            val title = info.optString("title")
            val cover = firstNonEmpty(info, "vertical_image_url", "cover_image_url")
            val intro = info.optString("description")
            var author = ""
            val user = info.optJSONObject("user")
            if (user != null) {
                author = user.optString("nickname")
            }
            val comics = info.optJSONArray("comics")
            var update = ""
            if (comics != null && comics.length() > 0) {
                val last = comics.optJSONObject(comics.length() - 1)
                if (last != null) {
                    update = last.optString("created_at")
                }
            }
            val finish = intro.contains("完结") || info.optString("update_status").contains("完结")
            comic.setInfo(
                if (title.isEmpty()) comic.title else title,
                if (cover.isEmpty()) comic.cover else cover,
                update, intro, author, finish
            )
        } catch (_: Exception) {
        }
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        try {
            val info = topicInfo(html)
            var comics = info?.optJSONArray("comics")
            if (comics == null) {
                comics = JSONObject(html).optJSONObject("data").optJSONArray("comics")
            }
            if (comics == null) {
                return list
            }
            var index = 0
            for (i in 0 until comics.length()) {
                val item = comics.optJSONObject(i) ?: continue
                val locked = item.optBoolean("locked", false)
                val pay = item.optBoolean("is_pay_comic", false)
                val free = item.optBoolean("is_free", false)
                if (locked || (pay && !free)) {
                    continue
                }
                val path = item.optString("id")
                if (path.isEmpty() || path == "0") {
                    continue
                }
                var title = item.optString("title")
                if (title.isEmpty()) {
                    title = path
                }
                list.add(Chapter((sourceComic.toString() + "000" + index).toLong(), sourceComic, title, path))
                index++
            }
        } catch (_: Exception) {
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        return apiGet(apiHost() + "/v2/comic/" + path)
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        try {
            val root = JSONObject(html)
            val data = root.optJSONObject("data") ?: return list
            val images = data.optJSONArray("images") ?: return list
            for (i in 0 until images.length()) {
                val url = images.optString(i)
                if (url.isNullOrEmpty()) {
                    continue
                }
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, i + 1, url, false))
            }
        } catch (_: Exception) {
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        try {
            val info = topicInfo(html)
            val comics = info?.optJSONArray("comics")
            if (comics != null && comics.length() > 0) {
                val last = comics.optJSONObject(comics.length() - 1)
                if (last != null) {
                    return last.optString("created_at")
                }
            }
        } catch (_: Exception) {
        }
        return ""
    }

    override fun getHeader(): Headers {
        return Headers.headersOf(
            "Referer", webHost() + "/",
            "User-Agent", USER_AGENT,
            "Accept", "application/json"
        )
    }

    private fun apiGet(url: String): Request {
        return Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", webHost() + "/")
            .header("Accept", "application/json")
            .build()
    }

    private fun topicInfo(html: String): JSONObject? {
        return try {
            val data = JSONObject(html).optJSONObject("data") ?: return null
            data.optJSONObject("topic_info")
        } catch (_: Exception) {
            null
        }
    }

    private fun jsonArray(html: String, key: String): JSONArray? {
        return try {
            val root = JSONObject(html)
            val data = root.optJSONObject("data")
            if (data != null) {
                val nested = data.optJSONArray(key)
                if (nested != null) {
                    return nested
                }
            }
            root.optJSONArray(key)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val TYPE = 231
        const val DEFAULT_TITLE = "快看漫画"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

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
