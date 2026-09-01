package com.haleydu.cimoc.source

import android.util.Pair
import com.google.common.collect.Lists
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.CATEGORY_AREA
import com.haleydu.cimoc.parser.Category.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.JsonIterator
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedList
import java.util.Locale

class DM5(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, Category())
    }

    private fun mobileHost(): String {
        return sourceConfigManager.firstUrl("https://m.dm5.com", "DM5", "DongManWu")
    }

    private fun wwwHost(): String {
        val mobile = mobileHost()
        if (mobile.contains("://m.")) {
            return mobile.replace("://m.", "://www.")
        }
        return mobile
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        val url = mobileHost() + "/pagerdata.ashx"
        val body = FormBody.Builder()
            .add("t", "7")
            .add("pageindex", page.toString())
            .add("title", keyword)
            .build()
        return Request.Builder().url(url).post(body).addHeader("Referer", mobileHost()).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator? {
        return try {
            object : JsonIterator(JSONArray(html)) {
                override fun parse(`object`: JSONObject): Comic? {
                    return try {
                        val cid = `object`.getString("Url").split("/")[1]
                        val title = `object`.getString("Title")
                        val cover = `object`.getString("Pic")
                        val update = `object`.getString("LastPartTime")
                        val array = `object`.optJSONArray("Author")
                        var author = ""
                        var i = 0
                        while (array != null && i != array.length()) {
                            author = author.plus(array.optString(i))
                            ++i
                        }
                        Comic(TYPE, cid, title, cover, update, author)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        } catch (_: JSONException) {
            null
        }
    }

    override fun getUrl(cid: String): String {
        return wwwHost() + "/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.dm5.com", "/([\\w\\-]+)"))
        filter.add(UrlFilter("tel.dm5.com", "/([\\w\\-]+)"))
        filter.add(UrlFilter("m.dm5.com", "/([\\w\\-]+)"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = wwwHost() + "/".plus(cid)
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.textWithSplit("div.banner_detail_form > div.info > p.title", " ", 0)
        val cover = body.src("div.banner_detail_form > div.cover > img")
        var update = body.text("#tempc > div.detail-list-title > span.s > span")
        if (update != null) {
            val calendar = Calendar.getInstance()
            if (update.contains("今天") || update.contains("分钟前")) {
                update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            } else if (update.contains("昨天")) {
                calendar.add(Calendar.DATE, -1)
                update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            } else if (update.contains("前天")) {
                calendar.add(Calendar.DATE, -2)
                update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            } else {
                var result = StringUtils.match("\\d+-\\d+-\\d+", update, 0)
                if (result == null) {
                    val rs = StringUtils.match("(\\d+)月(\\d+)号", update, 1, 2)
                    if (rs != null) {
                        result = calendar.get(Calendar.YEAR).toString() + "-" + rs[0] + "-" + rs[1]
                    }
                }
                update = result
            }
        }
        val author = body.text("div.banner_detail_form > div.info > p.subtitle > a")
        var intro = body.text("div.banner_detail_form > div.info > p.content")
        if (intro != null) {
            intro = intro.replace("[+展开]", "").replace("[-折叠]", "")
        }
        val status = isFinish(body.text("div.banner_detail_form > div.info > p.tip > span:eq(0)"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var i = 0
        for (node in body.list("#chapterlistload > ul  li > a")) {
            val title = StringUtils.split(node.text(), " ", 0)
            val path = node.hrefWithSplit(0)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = mobileHost() + "/".plus(path)
        return Request.Builder()
            .addHeader("Referer", StringUtils.format("%s/%s", mobileHost(), path))
            .url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        var str = StringUtils.match("eval\\(.*\\)", html, 0)
        if (str != null) {
            try {
                str = DecryptionUtils.evalDecrypt(str, "newImgs")
                if (str == "") {
                    str = DecryptionUtils.evalDecrypt(str)
                }
                val array = str.split(",")
                for (i in array.indices) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i + 1, array[i], false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun getLazyRequest(url: String): Request {
        return Request.Builder().url(url)
            .addHeader("Referer", wwwHost())
            .build()
    }

    override fun parseLazy(html: String, url: String): String? {
        val result = DecryptionUtils.evalDecrypt(html)
        if (result != null) {
            return result.split(",")[0]
        }
        return null
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        val body = Node(html)
        var update = body.text("#tempc > div.detail-list-title > span.s > span")
        if (update != null) {
            val calendar = Calendar.getInstance()
            if (update.contains("今天") || update.contains("分钟前")) {
                update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            } else if (update.contains("昨天")) {
                calendar.add(Calendar.DATE, -1)
                update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            } else if (update.contains("前天")) {
                calendar.add(Calendar.DATE, -2)
                update = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            } else {
                var result = StringUtils.match("\\d+-\\d+-\\d+", update, 0)
                if (result == null) {
                    val rs = StringUtils.match("(\\d+)月(\\d+)号", update, 1, 2)
                    if (rs != null) {
                        result = calendar.get(Calendar.YEAR).toString() + "-" + rs[0] + "-" + rs[1]
                    }
                }
                update = result
            }
        }
        return update
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = ArrayList<Comic>()
        val body = Node(html)
        for (node in body.list("ul.mh-list > li > div.mh-item")) {
            val cid = node.hrefWithSplit("div > h2.title > a", 0)
            val title = node.text("div > h2.title > a")
            val cover = StringUtils.match("\\((.*?)\\)", node.attr("p.mh-cover", "style"), 1)
            val author = node.textWithSubstring("p.author", 3)
            list.add(Comic(TYPE, cid, title, cover, null, author))
        }
        return list
    }

    override fun getHeader(url: String): Headers {
        val cid = "m".plus(StringUtils.match("cid=(\\d+)", url, 1))
        return Headers.headersOf("Referer", mobileHost() + "/".plus(cid))
    }

    override fun getHeader(list: List<ImageUrl>): Headers {
        var cid = ""
        if (list != null) {
            cid = list[0].chapter ?: ""
        }
        return Headers.headersOf("Referer", mobileHost() + "/".plus(cid))
    }

    private inner class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var path = joinPart(args, CATEGORY_SUBJECT) + " " +
                joinPart(args, CATEGORY_AREA) + " " +
                joinPart(args, CATEGORY_PROGRESS) + " " +
                joinPart(args, CATEGORY_ORDER)
            path = path.trim().replace("\\s+".toRegex(), "-")
            if (path.isEmpty()) {
                return wwwHost() + "/manhua-list-p%d"
            }
            return StringUtils.format("%s/manhua-list-%s-p%%d", wwwHost(), path)
        }

        private fun joinPart(args: Array<out String>?, index: Int): String {
            if (args == null || index >= args.size || args[index] == null) {
                return ""
            }
            return args[index]
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("热血", "tag31"))
            list.add(Pair.create("恋爱", "tag26"))
            list.add(Pair.create("校园", "tag1"))
            list.add(Pair.create("百合", "tag3"))
            list.add(Pair.create("耽美", "tag27"))
            list.add(Pair.create("冒险", "tag2"))
            list.add(Pair.create("后宫", "tag8"))
            list.add(Pair.create("科幻", "tag25"))
            list.add(Pair.create("战争", "tag12"))
            list.add(Pair.create("悬疑", "tag17"))
            list.add(Pair.create("推理", "tag33"))
            list.add(Pair.create("搞笑", "tag37"))
            list.add(Pair.create("奇幻", "tag14"))
            list.add(Pair.create("魔法", "tag15"))
            list.add(Pair.create("恐怖", "tag29"))
            list.add(Pair.create("神鬼", "tag20"))
            list.add(Pair.create("历史", "tag4"))
            list.add(Pair.create("同人", "tag30"))
            list.add(Pair.create("运动", "tag34"))
            list.add(Pair.create("绅士", "tag36"))
            list.add(Pair.create("机战", "tag40"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("港台", "area35"))
            list.add(Pair.create("日韩", "area36"))
            list.add(Pair.create("内地", "area37"))
            list.add(Pair.create("欧美", "area38"))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("连载", "st1"))
            list.add(Pair.create("完结", "st2"))
            return list
        }

        override fun hasOrder(): Boolean {
            return true
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("更新", "s2"))
            list.add(Pair.create("人气", ""))
            list.add(Pair.create("新品上架", "s18"))
            return list
        }
    }

    companion object {
        const val TYPE = 5
        const val DEFAULT_TITLE = "动漫屋"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
