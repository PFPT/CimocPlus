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
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.util.LinkedList
import java.util.regex.Pattern

class MH517(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) return null
        val url = StringUtils.format("http://m.517manhua.com/statics/search.aspx?key=%s", keyword)
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("ul#listbody > li")) {
            override fun parse(node: Node): Comic {
                val cid = node.href("a.ImgA")
                val title = node.text("a.txtA")
                val cover = node.attr("a.ImgA > img", "src")
                val update = node.text("")
                val author = node.text("")
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "http://m.517manhua.com$cid"
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.517manhua.com", ".*", 0))
    }

    override fun getInfoRequest(cid: String): Request {
        var id = cid
        if (id.indexOf("http://m.517manhua.com") == -1) {
            id = "http://m.517manhua.com".plus(id)
        }
        return Request.Builder().url(id).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.attr("div#Cover > img", "title")
        val cover = body.src("div#Cover > img")
        val update = body.text("span.date")
        val author = body.text("")
        val intro = body.text("p.txtDesc")
        val status = false
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("#mh-chapter-list-ol-0 > li")) {
            val title = node.text("a > span")
            val path = node.hrefWithSplit("a", 2)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val p = StringUtils.format("http://m.517manhua.com%s/%s.html", cid, path)
        return Request.Builder().url(p).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl>? {
        val list = ArrayList<ImageUrl>()
        val pageMatcher = Pattern.compile("qTcms_S_m_murl_e=\"(.*?)\"").matcher(html)
        val mangaid = StringUtils.match("var qTcms_S_m_id=\"(\\w+?)\";", html, 1)
        if (!pageMatcher.find()) return null
        try {
            val imgArrStr = DecryptionUtils.base64Decrypt(pageMatcher.group(1) ?: "")
            var i = 0
            for (item in imgArrStr.split("\\$.*?\\$".toRegex())) {
                val url = "http://m.517manhua.com/statics/pic/?p=$item&wapif=1&picid=$mangaid&m_httpurl="
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, ++i, url, false))
            }
        } finally {
            return list
        }
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://m.517manhua.com/")
    }

    companion object {
        const val TYPE = 70
        const val DEFAULT_TITLE = "我要去漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
