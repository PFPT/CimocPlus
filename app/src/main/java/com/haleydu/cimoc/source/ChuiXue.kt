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
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.LinkedList

class ChuiXue(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1) {
            url = StringUtils.format(
                "http://www.chuixue.net/e/search/?searchget=1&show=title,player,playadmin,pinyin&keyboard=%s",
                URLEncoder.encode(keyword, "GB2312")
            )
        }
        return Request.Builder().url(url).build()
    }

    override fun getUrl(cid: String): String {
        return "http://www.chuixue.net/manhua/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.chuixue.net"))
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("#dmList> ul > li")) {
            override fun parse(node: Node): Comic {
                val node_cover = node.list("p > a")[0]
                val cid = node_cover.hrefWithSplit(1)
                val title = node_cover.attr("img", "alt")
                val cover = node_cover.attr("img", "_src")
                val update = node.text("dl > dd > p:eq(0) > span")
                return Comic(TYPE, cid, title, cover, update, null)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://www.chuixue.net/manhua/".plus(cid)
        return Request.Builder().url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.intro_l > div.title > h1")
        val cover = body.src("div.intro_l > div.info_cover > p.cover > img")
        val update = body.text("div.intro_l > div.info > p:eq(0) > span").substring(0, 10)
        val author = body.text("div.intro_l > div.info > p:eq(1)").substring(5).trim()
        val intro = body.text("#intro")
        val status = isFinish(body.text("div.intro_l > div.info > p:eq(2)"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("#play_0 > ul > li > a")) {
            val title = node.attr("title")
            val path = node.hrefWithSplit(2)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    private var _cid = ""
    private var _path = ""

    override fun getImagesRequest(cid: String, path: String): Request {
        _cid = cid
        _path = path
        val url = StringUtils.format("http://www.chuixue.net/manhua/%s/%s.html", cid, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val str = StringUtils.match("photosr\\[1\\](.*?)(\n|var)", html, 1)
        if (str != null) {
            try {
                val array = str.split(";")
                for (i in array.indices) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    val s_full = array[i].trim()
                    val index = s_full.indexOf("=")
                    val s = s_full.substring(index + 2, s_full.length - 1)
                    list.add(ImageUrl(id, comicChapter, i + 1, "http://chuixue1.tianshigege.com/$s", false))
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
        return Node(html).text("div.intro_l > div.info > p:eq(0) > span")?.substring(0, 10)
    }

    override fun getHeader(): Headers {
        val referer = StringUtils.format("http://www.chuixue.net/manhua/%s/%s.html", _cid, _path)
        return Headers.headersOf("Referer", referer)
    }

    companion object {
        const val TYPE = 69
        const val DEFAULT_TITLE = "吹雪漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
