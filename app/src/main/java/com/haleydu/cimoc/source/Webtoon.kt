package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import java.util.ArrayList
import java.util.LinkedList

class Webtoon(source: Source?) : MangaParser() {

    init {
        init(source, Category())
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page == 1) {
            val url = "https://m.webtoons.com/zh-hant/search"
            val body: RequestBody = FormBody.Builder().add("keyword", keyword).add("searchType", "ALL").build()
            return Request.Builder().url(url).post(body).addHeader("Referer", "https://m.webtoons.com").build()
        }
        return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("ul._searchResultList > li > a")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit(-1)
                val title = node.text("div.row > div.info > p.subj > span")
                val cover = node.src("div.row > div.pic > img")
                val author = node.text("div.row > div.info > p.author")
                return Comic(TYPE, cid, title, cover, null, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "https://m.webtoons.com/episodeList?titleNo=".plus(cid)
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "https://m.webtoons.com/episodeList?titleNo=".plus(cid)
        return Request.Builder().url(url).addHeader("Referer", "http://m.webtoons.com").build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("#ct > div.detail_info > a._btnInfo > p.subj")
        val cover = body.src("#_episodeList > li > a > div.row > div.pic > img")
        var update = body.text("#_episodeList > li > a > div.row > div.info > p.date")
        if (update != null) {
            val args = update.split(Regex("\\D"))
            update = StringUtils.format("%4d-%02d-%02d", args[0].toInt(), args[1].toInt(), args[2].toInt())
        }
        val author = body.text("#ct > div.detail_info > a._btnInfo > p.author")
        val intro = body.text("#_informationLayer > p.summary_area")
        val status = isFinish(body.text("#_informationLayer > div.info_update"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        val body = Node(html)
        var i = 0
        for (node in body.list("#_episodeList > li > a")) {
            val title = node.text("div.row > div.info > p.sub_title > span")
            val path = node.hrefWithSubString(30)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = "https://m.webtoons.com/zh-hant/".plus(path)
        return Request.Builder().url(url).addHeader("Referer", "https://m.webtoons.com").build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val jsonString = StringUtils.match("var imageList = ([\\s\\S]*?);", html, 1)
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                val size = array.length()
                var i = 0
                while (i != size) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    val obj = array.getJSONObject(i)
                    list.add(ImageUrl(id, comicChapter, i + 1, obj.getString("url"), false))
                    ++i
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        var update = Node(html).text("#_episodeList > li > a > div.row > div.info > p.date")
        if (update != null) {
            val args = update.split(Regex("\\D"))
            update = StringUtils.format("%4d-%02d-%02d", args[0].toInt(), args[1].toInt(), args[2].toInt())
        }
        return update
    }

    override fun getCategoryRequest(format: String, page: Int): Request {
        if (page == 1) {
            return Request.Builder().url(format).addHeader("Referer", "https://m.webtoons.com").build()
        }
        return super.getCategoryRequest(format, page)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list: MutableList<Comic> = ArrayList()
        val body = Node(html)
        for (node in body.list("#ct > ul > li > a")) {
            val cid = node.hrefWithSplit(-1)
            val title = node.text("div.info > p.subj > span")
            val cover = node.attrWithSplit("div.pic", "style", "\\(|\\)", 1)
            list.add(Comic(TYPE, cid, title, cover, null, null))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "https://m.webtoons.com")
    }

    private class Category : MangaCategory() {
        override fun getFormat(vararg args: String): String {
            return "https://m.webtoons.com/zh-hant/new"
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("新作推荐", ""))
            return list
        }
    }

    companion object {
        const val TYPE = 6
        const val DEFAULT_TITLE = "Webtoon"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
