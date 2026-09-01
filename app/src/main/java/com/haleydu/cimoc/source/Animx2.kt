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
import java.util.regex.Pattern

class Animx2(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) return null
        val url = StringUtils.format("http://www.2animx.com/search-index?searchType=1&q=%s&page=%d", keyword, page)
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("ul.liemh > li")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit("a", 0)
                val title = node.text("a > div.tit")
                val cover = "http://www.2animx.com" + node.attr("a > img", "src")
                val update = node.text("a > font")
                return Comic(TYPE, cid, title, cover, update, "")
            }
        }
    }

    override fun getUrl(cid: String): String {
        return cid
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.bnmanhua.com", ".*", 0))
    }

    override fun getInfoRequest(cid: String): Request {
        var id = cid
        if (id.indexOf("http://www.2animx.com") == -1) {
            id = "http://www.2animx.com/".plus(id)
        }
        return Request.Builder().url(id).addHeader("Cookie", "isAdult=1").build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.position > strong")
        val cover = "http://www.2animx.com/" + body.src("dl.mh-detail > dt > a > img")
        val update = ""
        val author = ""
        val intro = body.text(".mh-introduce")
        val status = false
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("div#oneCon2 > ul > li")) {
            var title = node.attr("a", "title")
            val mTitle = Pattern.compile("\\d+").matcher(title)
            title = if (mTitle.find()) mTitle.group() else title
            val path = node.hrefWithSplit("a", 0)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    private var _cid: String? = null
    private var _path: String? = null

    override fun getImagesRequest(cid: String, path: String): Request {
        var p = path
        if (p.indexOf("http://www.2animx.com") == -1) {
            p = "http://www.2animx.com/".plus(p)
        }
        _cid = cid
        _path = p
        return Request.Builder().url(p).addHeader("Cookie", "isAdult=1").build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl>? {
        val list = ArrayList<ImageUrl>()
        val pageMatcher = Pattern.compile("id=\"total\" value=\"(.*?)\"").matcher(html)
        if (!pageMatcher.find()) return null
        val page = Integer.parseInt(pageMatcher.group(1) ?: "0")
        for (i in 1..page) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, i, StringUtils.format("%s-p-%d", _path, i), true))
        }
        return list
    }

    override fun getLazyRequest(url: String): Request {
        return Request.Builder()
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .addHeader("Cookie", "isAdult=1")
            .url(url).build()
    }

    override fun parseLazy(html: String, url: String): String? {
        val m = Pattern.compile("<\\/div><img src=\"(.*?)\" alt=").matcher(html)
        if (m.find()) {
            return m.group(1)
        }
        return null
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun getHeader(url: String): Headers {
        return Headers.headersOf("Referer", url)
    }

    companion object {
        const val TYPE = 55
        const val DEFAULT_TITLE = "2animx"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
