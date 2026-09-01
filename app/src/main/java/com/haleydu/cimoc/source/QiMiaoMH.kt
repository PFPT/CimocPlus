package com.haleydu.cimoc.source

import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.core.Manga.getResponseBody
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedList
import java.util.Objects
import java.util.Random

class QiMiaoMH(source: Source?, private val httpClient: OkHttpClient) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1) {
            url = StringUtils.format(baseUrl + "/action/Search?keyword=%s", keyword)
        }
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".mt20 > .classification")) {
            override fun parse(node: Node): Comic {
                val cid = node.href("h2 > a")
                val title = node.text("h2 > a")
                val cover = node.attr("a > img", "data-src")
                return Comic(TYPE, cid, title, cover, null, null)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return baseUrl + cid
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter(baseUrl))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = baseUrl + cid
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.attr(".ctdbLeft > a", "title")
        val cover = body.attr(".ctdbLeft > a > img", "src")
        val intro = body.text("#worksDesc").trim()
        val author = body.text(".detailBox > .author").replace("作者", "").trim()
        val update = body.text(".updeteStatus > .date").replace("更新：", "").trim()
        val statusStr = body.text("a.status")
        val status = isFinish(statusStr)
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var i = 0
        for (node in Node(html).list(".comic-content-list > ul.comic-content-c")) {
            val title = node.attr("li.cimg > a", "title")
            val path = node.href("li.cimg > a")
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = baseUrl + path
        return Request.Builder().url(url).build()
    }

    @Throws(Manga.NetworkErrorException::class)
    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        try {
            val did = Objects.requireNonNull(StringUtils.match("var did =(.*?);", html, 1)).trim()
            val sid = Objects.requireNonNull(StringUtils.match("var sid =(.*?);", html, 1)).trim()
            val random = Random().nextFloat()
            val url = StringUtils.format(baseUrl + "/Action/Play/AjaxLoadImgUrl?did=%s&sid=%s&tmp=%f", did, sid, random)
            val request = Request.Builder().url(url).build()
            val body = getResponseBody(httpClient, request)
            val jsonArray = JSONObject(body).getJSONArray("listImg")
            for (i in 0 until jsonArray.length()) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, i + 1, jsonArray.getString(i), false))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return Node(html).text(".updeteStatus > .date").replace("更新：", "").trim()
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", baseUrl)
    }

    companion object {
        const val TYPE = 56
        const val DEFAULT_TITLE = "奇妙漫画"
        private const val baseUrl = "https://www.qimiaomh.com"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
