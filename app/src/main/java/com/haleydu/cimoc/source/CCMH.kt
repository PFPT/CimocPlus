package com.haleydu.cimoc.source

import com.google.common.collect.Lists
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
import java.util.LinkedList
import java.util.regex.Pattern

class CCMH(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        val url = ""
        if (page == 1) {
            val requestBodyPost = FormBody.Builder()
                .add("Key", keyword)
                .build()
            return Request.Builder()
                .addHeader("Referer", "http://m.ccmh6.com/Search")
                .addHeader("Origin", "http://m.ccmh6.com")
                .addHeader("Host", "m.ccmh6.com")
                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
                .url("http://m.ccmh6.com/Search")
                .post(requestBodyPost)
                .build()
        }
        return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".list > div")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit("a", 1)
                val title = node.textWithSplit("a", "\\s+", 0)
                val cover = node.src("a > img")
                val author = node.textWithSplit("a", "\\s+", 2)
                return Comic(TYPE, cid, title, cover, "", author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return StringUtils.format("http://m.ccmh6.com/manhua/%s", cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.50mh.com", "manhua\\/(\\w+)", 1))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = StringUtils.format("http://m.ccmh6.com/manhua/%s", cid)
        return Request.Builder()
            .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
            .url(url)
            .build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val intro = body.text(".intro")
        val title = body.text(".other > div > strong")
        val cover = body.src(".cover > img")
        val author = body.textWithSplit(".other", "\\s+|：", 8)
        val update = body.textWithSplit(".other", "\\s+|：", 12)
            .replace("[", "").replace("]", "")
        val status = isFinish(body.textWithSplit(".other", "\\s+|：", 10))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var i = 0
        for (node in body.list(".list > a")) {
            val title = node.attr("title")
            val path = node.hrefWithSplit(2)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    private var _cid: String? = null
    private var _path: String? = null

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://m.ccmh6.com/manhua/%s/%s.html", cid, path)
        _cid = cid
        _path = path
        return Request.Builder()
            .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
            .url(url)
            .build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val pageCountMatcher = Pattern.compile("<a href=\"\\?p=(\\d+)\">\\d+<\\/a>").matcher(html)
        var pageCount = 0
        while (pageCountMatcher.find()) {
            val pageCountTemp = Integer.parseInt(pageCountMatcher.group(1))
            pageCount = if (pageCount > pageCountTemp) pageCount else pageCountTemp
        }
        for (i in 0 until pageCount) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, i + 1, StringUtils.format("http://m.ccmh6.com/manhua/%s/%s.html?p=%d", _cid, _path, i + 1), true))
        }
        return list
    }

    override fun getLazyRequest(url: String): Request {
        return Request.Builder()
            .addHeader("Referer", StringUtils.format("http://m.ccmh6.com/manhua/%s/%s.html", _cid, _path))
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .url(url).build()
    }

    override fun parseLazy(html: String, url: String): String? {
        val body = Node(html)
        return body.src(".img > img")
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).text(".Introduct_Sub > .sub_r > .txtItme:eq(4)")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://m.ccmh6.com/")
    }

    companion object {
        const val TYPE = 23
        const val DEFAULT_TITLE = "CC漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
