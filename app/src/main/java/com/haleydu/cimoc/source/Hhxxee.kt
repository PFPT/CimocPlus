package com.haleydu.cimoc.source

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
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.util.LinkedList

class Hhxxee(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1)
            url = "http://99770.hhxxee.com/search/s.aspx"
        val requestBodyPost = FormBody.Builder()
            .add("tbSTxt", keyword)
            .build()
        return Request.Builder().url(url)
            .post(requestBodyPost).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".cInfoItem")) {
            override fun parse(node: Node): Comic {
                val cid = node.href(".cListTitle > a").orEmpty().substring("http://99770.hhxxee.com/comic/".length)
                var title = node.text(".cListTitle > span").orEmpty()
                title = title.substring(1, title.length - 1)
                val cover = node.src(".cListSlt > img")
                val update = node.text(".cListh2 > span").orEmpty().substring(8)
                val author = node.text(".cl1_2").orEmpty().substring(3)
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "http://99770.hhxxee.com/comic/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("99770.hhxxee.com", "(\\d+)$"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://99770.hhxxee.com/comic/".plus(cid)
        return Request.Builder().url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text(".cTitle")
        val cover = body.src(".cDefaultImg > img")
        val update = body.text(".cInfoTxt  tr:nth-child(5) > td:last-child").orEmpty().trim().split("更新時間:")[1]
        val author = body.text(".cInfoTxt  tr:nth-child(2) > td:last-child").orEmpty().trim()
        val intro = body.text(".cCon")
        val status = false
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list(".cVolList > div")) {
            val title = node.text("a")
            val path = node.hrefWithSplit("a", 2)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://99770.hhxxee.com/comic/%s/%s/", cid, path)
        return Request.Builder().url(url).build()
    }

    private fun getPictureServers(url: String): Int {
        return Integer.parseInt(StringUtils.match("ok\\-comic(\\d+)", url, 1)) - 1
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val str = StringUtils.match("var sFiles=\"(.*?)\"", html, 1)
        if (str != null) {
            try {
                val array = str.split("|")
                for (i in array.indices) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i + 1, servers[getPictureServers(array[i])] + array[i], false))
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
        return Node(html).text("div.book-detail > div.cont-list > dl:eq(2) > dd")
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        val body = Node(html)
        for (node in body.list("li > a")) {
            val cid = node.hrefWithSplit(1)
            val title = node.text("h3")
            val cover = node.attr("div > img", "data-src")
            val update = node.text("dl:eq(5) > dd")
            val author = node.text("dl:eq(2) > dd")
            list.add(Comic(TYPE, cid, title, cover, update, author))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://99770.hhxxee.com")
    }

    companion object {
        const val TYPE = 59
        const val DEFAULT_TITLE = "997700"
        private const val serverstr = "http://20.125084.com/dm01/|http://20.125084.com/dm02/|http://20.125084.com/dm03/|http://20.125084.com/dm04/|http://20.125084.com/dm05/|http://20.125084.com/dm06/|http://20.125084.com/dm07/|http://20.125084.com/dm08/|http://20.125084.com/dm09/|http://20.125084.com/dm10/|http://20.125084.com/dm11/|http://20.125084.com/dm12/|http://20.125084.com/dm13/|http://20.125084.com/dm14/|http://20.125084.com/dm15/|http://20.125084.com/dm16/"
        private val servers = serverstr.split("|").toTypedArray()

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
