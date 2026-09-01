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
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONException
import java.io.UnsupportedEncodingException
import java.util.LinkedList

class JMTT(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) return null
        val url = StringUtils.format(baseUrl + "/search/photos?search_query=%s&main_tag=0", keyword)
        return Request.Builder().url(url).build()
    }

    @Throws(JSONException::class)
    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("div#wrapper > div.container > div.row > div > div.row > div")) {
            override fun parse(node: Node): Comic {
                val cid = node.href("div.thumb-overlay > a")
                val title = node.text("span.video-title")
                val cover = node.attr("div.thumb-overlay > a > img", "data-original")
                val update = node.text("div.video-views")
                val author = ""
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return baseUrl + cid
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter(baseUrl))
        filter.add(UrlFilter("https://comic18j-rita.cc"))
        filter.add(UrlFilter("https://comic18j-rita.me"))
        filter.add(UrlFilter("https://jm18c-dfg.org"))
        filter.add(UrlFilter("https://18comic1.one/"))
        filter.add(UrlFilter("https://18comic2.one/"))
        filter.add(UrlFilter("https://18comic.vip"))
        filter.add(UrlFilter("18comic.org"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = baseUrl + cid
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        try {
            val body = Node(html)
            val intro = body.text("#intro-block > div:eq(0)")
            val title = body.text("div.panel-heading > div")
            val cover = body.attr("img.lazy_img.img-responsive", "src").orEmpty().trim()
            val author = body.text("#intro-block > div:eq(4) > span")
            val update = body.attr("#album_photo_cover > div:eq(1) > div:eq(3)", "content")
            val status = isFinish(body.text("#intro-block > div:eq(2) > span"))
            comic.setInfo(title, cover, update, intro, author, status)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var i = 0
        val startTitle = body.text(".col.btn.btn-primary.dropdown-toggle.reading").orEmpty().trim()
        val startPath = body.href(".col.btn.btn-primary.dropdown-toggle.reading")
        list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, startTitle, startPath))
        for (node in body.list("#episode-block > div > div.episode > ul > a")) {
            val title = node.text("li").orEmpty().trim()
            val path = node.href()
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    private var imgpath = ""

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = baseUrl + path
        imgpath = path
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = ArrayList<ImageUrl>()
        try {
            var i = 0
            for (node in Node(html).list("img.lazy_img")) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                val img1 = node.attr("img", "src").orEmpty()
                val img2 = node.attr("img", "data-original").orEmpty()
                val reg = imgpath.split("/")
                if (img1.contains(reg[2])) {
                    list.add(ImageUrl(id, comicChapter, ++i, img1, false))
                } else if (img2.contains(reg[2])) {
                    list.add(ImageUrl(id, comicChapter, ++i, img2, false))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).attr("#album_photo_cover > div:eq(1) > div:eq(3)", "content")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", baseUrl)
    }

    companion object {
        const val TYPE = 72
        const val DEFAULT_TITLE = "禁漫天堂"
        const val baseUrl = "https://comic18j-rita.cc"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, false)
        }
    }
}
