package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.CATEGORY_AREA
import com.haleydu.cimoc.parser.Category.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.CATEGORY_READER
import com.haleydu.cimoc.parser.Category.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.JsonIterator
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.utils.StringUtils
import com.haleydu.cimoc.utils.UicodeBackslashU
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale

class Dmzj(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, Category())
    }

    private fun mobileHost(): String {
        return sourceConfigManager.firstUrl("http://m.dmzj1.com", "DMZJ", "DongManZhiJia", "DMZJV2")
    }

    private fun pictureHost(): String {
        return sourceConfigManager.firstUrl("http://images.dmzj1.com", "DMZJPICTURE", "DongManZhiJia")
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var encoded = keyword
        try {
            encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        } catch (_: Exception) {
        }
        val suffix = StringUtils.format("/search/show/0/%s/%d.json", encoded, Math.max(page - 1, 0))
        val hosts = sourceConfigManager.listUrls(
            "https://v3api.idmzj.com", "DMZJV2SERVER", "DMZJSERVER"
        ).toMutableList()
        addHost(hosts, "https://v3api.dmzj.com")
        addHost(hosts, "http://v3api.dmzj1.com")
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
                        val title = firstNonEmpty(`object`, "title", "comic_name", "name")
                        var cover = firstNonEmpty(`object`, "cover", "comic_cover")
                        if (cover.startsWith("//")) {
                            cover = "https:$cover"
                        }
                        val author = firstNonEmpty(`object`, "authors", "comic_author", "author")
                        Comic(TYPE, cid, title, cover, null, author)
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
        var embedded = StringUtils.match("var g_search_data = (.*?);", html, 1)
        if (embedded == null) {
            embedded = StringUtils.match("var serchArry=(\\[\\{.*?\\}\\])", html, 1)
        }
        if (embedded == null) {
            return null
        }
        return JSONArray(UicodeBackslashU.unicodeToCn(embedded).replace("\\/", "/"))
    }

    override fun getUrl(cid: String): String {
        return StringUtils.format("%s/info/%s.html", mobileHost(), cid)
    }

    override fun getInfoRequest(cid: String): Request {
        val id = "comic_" + cid + "_android"
        val suffix = StringUtils.format("/comic/%s.json", id)
        return sourceConfigManager.hostPathRequest(suffix, "http://v3api.dmzj1.com", "DMZJSERVER", "DMZJV2SERVER")
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        try {
            val obj = JSONObject(html)
            val title = obj.getString("title")
            val cover = obj.getString("cover")
            val time = if (obj.has("last_updatetime")) obj.getLong("last_updatetime") * 1000 else null
            val update = if (time == null) null else StringUtils.getFormatTime("yyyy-MM-dd", time)
            val intro = obj.optString("description")
            val sb = StringBuilder()
            val array = obj.getJSONArray("authors")
            for (i in 0 until array.length()) {
                sb.append(array.getJSONObject(i).getString("tag_name")).append(" ")
            }
            val author = sb.toString()
            val status = obj.getJSONArray("status").getJSONObject(0).getInt("tag_id") == 2310
            comic.setInfo(title, cover, update, intro, author, status)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        try {
            val obj = JSONObject(html)
            val array = obj.getJSONArray("chapters")
            var k = 0
            for (i in 0 until array.length()) {
                val data = array.getJSONObject(i).getJSONArray("data")
                for (j in 0 until data.length()) {
                    val chapter = data.getJSONObject(j)
                    val title = chapter.getString("chapter_title")
                    val path = chapter.getString("chapter_id")
                    list.add(Chapter((sourceComic.toString() + "000" + k++).toLong(), sourceComic, title, path))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val suffix = StringUtils.format("/chapter/%s/%s.json", cid, path)
        return sourceConfigManager.hostPathRequest(suffix, "http://v3api.dmzj1.com", "DMZJSERVER", "DMZJV2SERVER")
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        try {
            val obj = JSONObject(html)
            val array = obj.getJSONArray("page_url")
            for (i in 0 until array.length()) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, i + 1, array.getString(i), false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        try {
            val obj = JSONObject(html)
            val time = obj.getLong("last_updatetime") * 1000
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        try {
            val decodeJsonString = UicodeBackslashU.unicodeToCn(html).replace("\\/", "/")
            val array = JSONArray(decodeJsonString)
            for (i in 0 until array.length()) {
                try {
                    val obj = array.getJSONObject(i)
                    if (obj.optInt("hidden", 1) != 1) {
                        val cid = obj.getString("id")
                        val title = obj.getString("comic_name")
                        val cover = pictureHost() + "/".plus(obj.getString("comic_cover"))
                        val time = if (obj.has("last_updatetime")) obj.getLong("last_updatetime") * 1000 else null
                        val update = if (time == null) null else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
                        val author = obj.optString("comic_author")
                        list.add(Comic(TYPE, cid, title, cover, update, author))
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", mobileHost() + "/")
    }

    private inner class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            return StringUtils.format(
                "%s/classify/%s-%s-%s-%s-%s-%%d.json",
                mobileHost(),
                args[CATEGORY_SUBJECT], args[CATEGORY_READER], args[CATEGORY_PROGRESS], args[CATEGORY_AREA], args[CATEGORY_ORDER]
            )
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "0"))
            list.add(Pair.create("冒险", "1"))
            list.add(Pair.create("欢乐向", "2"))
            list.add(Pair.create("格斗", "3"))
            list.add(Pair.create("科幻", "4"))
            list.add(Pair.create("爱情", "5"))
            list.add(Pair.create("竞技", "6"))
            list.add(Pair.create("魔法", "7"))
            list.add(Pair.create("校园", "8"))
            list.add(Pair.create("悬疑", "9"))
            list.add(Pair.create("恐怖", "10"))
            list.add(Pair.create("生活亲情", "11"))
            list.add(Pair.create("百合", "12"))
            list.add(Pair.create("伪娘", "13"))
            list.add(Pair.create("耽美", "14"))
            list.add(Pair.create("后宫", "15"))
            list.add(Pair.create("萌系", "16"))
            list.add(Pair.create("治愈", "17"))
            list.add(Pair.create("武侠", "18"))
            list.add(Pair.create("职场", "19"))
            list.add(Pair.create("奇幻", "20"))
            list.add(Pair.create("节操", "21"))
            list.add(Pair.create("轻小说", "22"))
            list.add(Pair.create("搞笑", "23"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "0"))
            list.add(Pair.create("日本", "1"))
            list.add(Pair.create("内地", "2"))
            list.add(Pair.create("欧美", "3"))
            list.add(Pair.create("港台", "4"))
            list.add(Pair.create("韩国", "5"))
            list.add(Pair.create("其他", "6"))
            return list
        }

        override fun hasReader(): Boolean {
            return true
        }

        override fun getReader(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "0"))
            list.add(Pair.create("少年", "3262"))
            list.add(Pair.create("少女", "3263"))
            list.add(Pair.create("青年", "3264"))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", "0"))
            list.add(Pair.create("连载", "1"))
            list.add(Pair.create("完结", "2"))
            return list
        }

        override fun hasOrder(): Boolean {
            return true
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("更新", "1"))
            list.add(Pair.create("人气", "0"))
            return list
        }
    }

    companion object {
        const val TYPE = 1
        const val DEFAULT_TITLE = "动漫之家"

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

        private fun addHost(hosts: MutableList<String>, host: String?) {
            if (!host.isNullOrEmpty() && !hosts.contains(host)) {
                hosts.add(host)
            }
        }
    }
}
