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
import java.util.LinkedList
import java.util.Objects

class TuHao(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1) {
            url = StringUtils.format("https://%s/sort/?key=%s", website, keyword)
        }
        return Request.Builder().url(url).build()
    }

    override fun getUrl(cid: String): String {
        return "https://".plus(website).plus("/").plus(cid).plus("/")
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter(website, "\\w+", 0))
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("div.cy_list_mh > ul")) {
            override fun parse(node: Node): Comic {
                val title = node.text("li.title > a")
                val cid = node.hrefWithSplit("li > a", 1)
                val cover = node.attr("a.pic > img", "src")
                val update = node.text("li.updata > a > span")
                return Comic(TYPE, cid, title, cover, update, null)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "https://$website/manhua/$cid/"
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val cover = body.src("img.pic")
        val intro = body.text("p#comic-description")
        val title = body.text("div.cy_title > h1")
        val update = body.text("div.cy_zhangjie_top > p >font")
        val author = body.text("div.cy_xinxi > span:eq(0)")
        val status = isFinish(body.text("div.cy_xinxi > span:eq(1) > a"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var i = 0
        for (node in Node(html).list("div.cy_plist > ul > li")) {
            val title = node.text()
            val path = node.hrefWithSplit("a", 1)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("https://%s/chapter/%s.html", website, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val str = StringUtils.match("\"page_url\":\"(.*?)\",", html, 1) ?: return list
        var i = 0
        for (url in str.split(Regex("\\|72cms\\|"))) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, ++i, url, false))
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return Node(html).text("div.cy_zhangjie_top > p >font")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "https://m.tohomh456.com")
    }

    companion object {
        const val TYPE = 24
        const val DEFAULT_TITLE = "土豪漫画"
        private const val website = "tuhao456.com"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
