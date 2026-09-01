package com.haleydu.cimoc.source

import com.alibaba.fastjson.JSON
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.util.LinkedList

class ManHuaDB(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1) {
            url = StringUtils.format("https://www.manhuadb.com/search?q=%s", keyword)
        }
        return Request.Builder().url(url).build()
    }

    override fun getUrl(cid: String): String {
        return "https://www.manhuadb.com/manhua/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.manhuadb.com"))
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("a.d-block")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit(1)
                val title = node.attr("title")
                val cover = node.attr("img", "data-original")
                return Comic(TYPE, cid, title, cover, null, null)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "https://www.manhuadb.com/manhua/".plus(cid)
        return Request.Builder().url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("h1.comic-title")
        val cover = body.src("td.comic-cover > img")
        val author = body.text("a.comic-creator")
        val intro = body.text("p.comic_story")
        val status = isFinish(body.text("a.comic-pub-state"))
        var update = body.text("a.comic-pub-end-date")
        if (update == null || update == "") {
            update = body.text("a.comic-pub-date")
        }
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("#comic-book-list > div > ol > li > a")) {
            val title = node.attr("title")
            val path = node.hrefWithSplit(2)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("https://www.manhuadb.com/manhua/%s/%s.html", cid, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = ArrayList<ImageUrl>()
        try {
            val imageHost = StringUtils.match("data-host=\"(.*?)\"", html, 1)
            val imagePre = StringUtils.match("data-img_pre=\"(.*?)\"", html, 1)
            val base64Data = StringUtils.match("var img_data = '(.*?)';", html, 1)
            val jsonStr = DecryptionUtils.base64Decrypt(base64Data ?: "")
            val imageList = JSON.parseArray(jsonStr)
            for (i in 0 until imageList.size) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                val image = imageList.getJSONObject(i)
                val imageUrl = (imageHost ?: "") + (imagePre ?: "") + image.getString("img")
                list.add(ImageUrl(id, comicChapter, image.getIntValue("p"), imageUrl, false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getLazyRequest(url: String): Request? {
        return null
    }

    override fun parseLazy(html: String, url: String): String? {
        return null
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        val body = Node(html)
        var update = body.text("a.comic-pub-end-date")
        if (update == null || update == "") {
            update = body.text("a.comic-pub-date")
        }
        return update
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "https://www.manhuadb.com")
    }

    companion object {
        const val TYPE = 46
        const val DEFAULT_TITLE = "漫画DB"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
