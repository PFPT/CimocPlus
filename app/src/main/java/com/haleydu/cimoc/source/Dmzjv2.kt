package com.haleydu.cimoc.source

import android.util.Pair
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
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
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

class Dmzjv2(source: Source?) : MangaParser() {

    init {
        init(source, Category())
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("manhua.dmzj.com", "/(\\w+)"))
        filter.add(UrlFilter("m.dmzj.com", "/info/(\\w+).html"))
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page == 1) {
            val url = StringUtils.format("https://m.dmzj.com/search/%s.html", keyword)
            return Request.Builder().url(url).build()
        }
        return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator? {
        try {
            val JsonString = StringUtils.match("var serchArry=(\\[\\{.*?\\}\\])", html, 1)
            val decodeJsonString = UicodeBackslashU.unicodeToCn(JsonString).replace("\\/", "/")
            return object : JsonIterator(JSONArray(decodeJsonString)) {
                override fun parse(`object`: JSONObject): Comic? {
                    return try {
                        val cid = `object`.getString("comic_py")
                        val title = `object`.getString("name")
                        var cover = `object`.getString("cover")
                        cover = "https://images.dmzj.com/$cover"
                        val author = `object`.optString("authors")
                        val time = java.lang.Long.parseLong(`object`.getString("last_updatetime")) * 1000
                        val update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
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

    override fun getUrl(cid: String): String {
        return StringUtils.format("http://m.dmzj.com/info/%s.html", cid)
    }

    override fun getInfoRequest(cid: String): Request {
        val url = StringUtils.format("http://m.dmzj.com/info/%s.html", cid)
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val intro = body.textWithSubstring("p.txtDesc", 3)
        val title = body.text("#comicName")
        val cover = body.src("#Cover > img")
        val author = body.text("a.pd.introName")
        val update = body.textWithSubstring("div.Introduct_Sub > div.sub_r > p:eq(3) > span.date", 0, 10)
        val status = isFinish(body.text("div.sub_r > p:eq(2)"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        try {
            val JsonArrayString = StringUtils.match("initIntroData\\((.*)\\);", html, 1)
            val decodeJsonArrayString = UicodeBackslashU.unicodeToCn(JsonArrayString)
            val allJsonArray = JSONArray(decodeJsonArrayString)
            var k = 0
            for (i in 0 until allJsonArray.length()) {
                val JSONArray = allJsonArray.getJSONObject(i).getJSONArray("data")
                val tag = allJsonArray.getJSONObject(i).getString("title")
                for (j in 0 until JSONArray.length()) {
                    val chapter = JSONArray.getJSONObject(j)
                    val title = chapter.getString("chapter_name")
                    val comic_id = chapter.getString("comic_id")
                    val chapter_id = chapter.getString("id")
                    val path = "$comic_id/$chapter_id"
                    list.add(Chapter((sourceComic.toString() + "000" + k++).toLong(), sourceComic, "$tag $title", path))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://m.dmzj.com/view/%s.html", path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val jsonString = StringUtils.match("\"page_url\":(\\[.*?\\]),", html, 1)
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i + 1, array.getString(i), false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).textWithSubstring("div.Introduct_Sub > div.sub_r > p:eq(3) > span.date", 0, 10)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        try {
            val array = JSONArray(html)
            for (i in 0 until array.length()) {
                try {
                    val obj = array.getJSONObject(i)
                    val cid = obj.getString("id")
                    val title = obj.getString("title")
                    val cover = obj.getString("cover")
                    val time = if (obj.has("last_updatetime")) obj.getLong("last_updatetime") * 1000 else null
                    val update = if (time == null) null else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
                    val author = obj.optString("authors")
                    list.add(Comic(TYPE, cid, title, cover, update, author))
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
        return Headers.headersOf("Referer", "http://images.dmzj.com/")
    }

    private class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var path = args[CATEGORY_SUBJECT].plus(" ").plus(args[CATEGORY_READER]).plus(" ").plus(args[CATEGORY_PROGRESS])
                .plus(" ").plus(args[CATEGORY_AREA]).trim()
            path = if (path.isEmpty()) {
                0.toString()
            } else {
                path.replace("\\s+".toRegex(), "-")
            }
            return StringUtils.format("http://v2.api.dmzj.com/classify/%s/%s/%%d.json", path, args[CATEGORY_ORDER])
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("冒险", "4"))
            list.add(Pair.create("百合", "3243"))
            list.add(Pair.create("生活", "3242"))
            list.add(Pair.create("四格", "17"))
            list.add(Pair.create("伪娘", "3244"))
            list.add(Pair.create("悬疑", "3245"))
            list.add(Pair.create("后宫", "3249"))
            list.add(Pair.create("热血", "3248"))
            list.add(Pair.create("耽美", "3246"))
            list.add(Pair.create("其他", "16"))
            list.add(Pair.create("恐怖", "14"))
            list.add(Pair.create("科幻", "7"))
            list.add(Pair.create("格斗", "6"))
            list.add(Pair.create("欢乐向", "5"))
            list.add(Pair.create("爱情", "8"))
            list.add(Pair.create("侦探", "9"))
            list.add(Pair.create("校园", "13"))
            list.add(Pair.create("神鬼", "12"))
            list.add(Pair.create("魔法", "11"))
            list.add(Pair.create("竞技", "10"))
            list.add(Pair.create("历史", "3250"))
            list.add(Pair.create("战争", "3251"))
            list.add(Pair.create("魔幻", "5806"))
            list.add(Pair.create("扶她", "5345"))
            list.add(Pair.create("东方", "5077"))
            list.add(Pair.create("奇幻", "5848"))
            list.add(Pair.create("轻小说", "6316"))
            list.add(Pair.create("仙侠", "7900"))
            list.add(Pair.create("搞笑", "7568"))
            list.add(Pair.create("颜艺", "6437"))
            list.add(Pair.create("性转换", "4518"))
            list.add(Pair.create("高清单行", "4459"))
            list.add(Pair.create("治愈", "3254"))
            list.add(Pair.create("宅系", "3253"))
            list.add(Pair.create("萌系", "3252"))
            list.add(Pair.create("励志", "3255"))
            list.add(Pair.create("节操", "6219"))
            list.add(Pair.create("职场", "3328"))
            list.add(Pair.create("西方魔幻", "3365"))
            list.add(Pair.create("音乐舞蹈", "3326"))
            list.add(Pair.create("机战", "3325"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("日本", "2304"))
            list.add(Pair.create("韩国", "2305"))
            list.add(Pair.create("欧美", "2306"))
            list.add(Pair.create("港台", "2307"))
            list.add(Pair.create("内地", "2308"))
            list.add(Pair.create("其他", "8453"))
            return list
        }

        override fun hasReader(): Boolean {
            return true
        }

        override fun getReader(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
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
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("连载", "2309"))
            list.add(Pair.create("完结", "2310"))
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
        const val TYPE = 10
        const val DEFAULT_TITLE = "动漫之家v2"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
