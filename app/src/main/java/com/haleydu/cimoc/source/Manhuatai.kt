package com.haleydu.cimoc.source

import android.util.Pair
import com.google.common.collect.Lists
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.JsonIterator
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.LinkedList

class Manhuatai(
    source: Source?,
    private val httpClient: OkHttpClient,
    private val sourceConfigManager: SourceConfigManager
) : MangaParser() {

    init {
        init(source, Category())
    }

    private fun mobileHost(): String {
        return sourceConfigManager.getField("ManHuaTai", "serverUrl", "https://m.kanman.com")
    }

    private fun webHost(): String {
        return sourceConfigManager.getField("ManHuaTai", "baseUrl", "https://www.kanman.com")
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        val url = StringUtils.format(
            mobileHost() + "/api/getsortlist/?product_id=2&productname=mht&platformname=wap&orderby=click&search_key=%s&page=%d&size=48",
            URLEncoder.encode(keyword, "UTF-8"), page
        )
        return Request.Builder().url(url).build()
    }

    @Throws(JSONException::class)
    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val obj = JSONObject(html)
        return object : JsonIterator(obj.getJSONObject("data").getJSONArray("data")) {
            override fun parse(`object`: JSONObject): Comic {
                val title = `object`.getString("comic_name")
                val cid = `object`.getString("comic_newid")
                val cover = "https://image.yqmh.com/mh/" + `object`.getString("comic_id") + ".jpg-300x400.webp"
                val author: String? = null
                val update: String? = null
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    @Throws(Manga.NetworkErrorException::class)
    private fun getComicNode(cid: String): Node {
        val request = getInfoRequest(cid)
        val html = Manga.getResponseBody(httpClient, request)
        return Node(html)
    }

    override fun getInfoRequest(cid: String): Request {
        val url = webHost() + "/".plus(cid) + "/"
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.attr("h1#detail-title", "title")
        var cover = body.attr("div.detail-cover > img", "data-src")
        cover = "https:$cover"
        val update = body.text("span.update").orEmpty().substring(0, 10)
        val author: String? = null
        val intro = body.text("div#js_comciDesc > p.desc-content")
        comic.setInfo(title, cover, update, intro, author, false)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("ol#j_chapter_list > li > a")) {
            val title = node.attr("title")
            val path = node.hrefWithSplit(1)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    private var _path: String? = null

    override fun getImagesRequest(cid: String, path: String): Request {
        _path = path
        val url = StringUtils.format(mobileHost() + "/api/getcomicinfo_body?product_id=2&productname=mht&platformname=wap&comic_newid=%s", cid)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        try {
            val obj = JSONObject(html)
            if (obj.getInt("status") != 0) {
                return list
            }
            val chapters = obj.getJSONObject("data").getJSONArray("comic_chapter")
            var chapterNew: JSONObject? = null
            for (i in 0 until chapters.length()) {
                chapterNew = chapters.getJSONObject(i)
                val a = chapterNew.getString("chapter_id")
                if (a == _path) {
                    break
                }
            }
            val ImagePattern = "http://mhpic." + chapterNew!!.getString("chapter_domain") + chapterNew.getString("rule") + "-mht.low.webp"
            for (index in chapterNew.getInt("start_num")..chapterNew.getInt("end_num")) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + index).toLong()
                val image = ImagePattern.replaceFirst("\\$\\$".toRegex(), Integer.toString(index))
                list.add(ImageUrl(id, comicChapter, index, image, false))
            }
        } catch (_: JSONException) {
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return Node(html).text("span.update").orEmpty().substring(0, 10)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        if (html == null) {
            return list
        }
        val trim = html.trim()
        if (trim.startsWith("{")) {
            try {
                val array = JSONObject(trim).getJSONObject("data").getJSONArray("data")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val title = obj.getString("comic_name")
                    val cid = obj.getString("comic_newid")
                    val cover = "https://image.yqmh.com/mh/" + obj.getString("comic_id") + ".jpg-300x400.webp"
                    list.add(Comic(TYPE, cid, title, cover, null, null))
                }
                return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val body = Node(html)
        for (node in body.list("a.sdiv")) {
            val cid = node.hrefWithSplit(0)
            val title = node.attr("title")
            var cover = node.getChild("img").attr("data-url")
            var node1: Node? = null
            try {
                node1 = getComicNode(cid)
            } catch (e: Manga.NetworkErrorException) {
                e.printStackTrace()
            }
            if (StringUtils.isEmpty(cover) && node1 != null) {
                cover = node1.src("#offlinebtn-container > img")
            }
            var author: String? = null
            var update: String? = null
            if (node1 != null) {
                author = node1.text("div.jshtml > ul > li:nth-child(3)").substring(3)
                update = node1.text("div.jshtml > ul > li:nth-child(5)").substring(3)
            }
            list.add(Comic(TYPE, cid, title, cover, update, author))
        }
        return list
    }

    private inner class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var subject = if (args != null && args.size > CATEGORY_SUBJECT && args[CATEGORY_SUBJECT] != null)
                args[CATEGORY_SUBJECT] else "all"
            if (subject.isEmpty()) {
                subject = "all"
            }
            return mobileHost() + "/api/getsortlist/?product_id=2&productname=mht&platformname=wap&orderby=click&comic_sort=" +
                subject + "&page=%d&size=48"
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部漫画", "all"))
            list.add(Pair.create("知音漫客", "zhiyinmanke"))
            list.add(Pair.create("神漫", "shenman"))
            list.add(Pair.create("风炫漫画", "fengxuanmanhua"))
            list.add(Pair.create("漫画周刊", "manhuazhoukan"))
            list.add(Pair.create("飒漫乐画", "samanlehua"))
            list.add(Pair.create("飒漫画", "samanhua"))
            list.add(Pair.create("漫画世界", "manhuashijie"))
            return list
        }

        override fun hasOrder(): Boolean {
            return false
        }

        override fun getOrder(): List<Pair<String, String>>? {
            return null
        }
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", mobileHost())
    }

    companion object {
        const val TYPE = 49
        const val DEFAULT_TITLE = "漫画台"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
