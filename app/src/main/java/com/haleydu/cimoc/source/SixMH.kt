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
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import java.util.LinkedList
import java.util.Objects

class SixMH(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1) {
            url = StringUtils.format("http://%s/search.php?keyword=%s", website, keyword)
        }
        return Request.Builder().url(url).build()
    }

    override fun getUrl(cid: String): String {
        return "http://".plus(website).plus("/").plus(cid).plus("/")
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter(website, "\\w+", 0))
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("div.cy_list_mh > ul")) {
            override fun parse(node: Node): Comic {
                val title = node.text("li.title > a")
                val cid = node.href("li > a.pic")
                val cover = node.attr("a.pic > img", "src")
                val update = node.text("li.updata > a > span")
                return Comic(TYPE, cid, title, cover, update, null)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://$website$cid"
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        ChapterHtml = html
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

    override fun getChapterRequest(html: String, cid: String): Request {
        val url = "http://$website/bookchapter/"
        val body = Node(html)
        val id = body.attr("a#zhankai", "data-id")
        val id2 = body.attr("a#zhankai", "data-vid")
        val requestBody: RequestBody = FormBody.Builder().add("id", id).add("id2", id2).build()
        return Request.Builder().url(url).post(requestBody).build()
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        try {
            var i = 0
            for (node in Node(ChapterHtml!!).list("div.cy_plist > ul > li")) {
                val title = node.text()
                val path = node.hrefWithSplit("a", 1)
                list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
            }
            val array = JSONArray(html)
            var k = 0
            while (k != array.length()) {
                val chapter = array.getJSONObject(k)
                val title = chapter.getString("chaptername")
                val path = chapter.getString("chapterid")
                list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
                ++k
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://%s%s%s.html", website, cid, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        var str = StringUtils.match("eval\\(.*\\)", html, 0)
        str = DecryptionUtils.evalDecrypt(str!!, "newImgs")
        var i = 0
        for (url in Objects.requireNonNull(str).split(",")) {
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
        return Headers.headersOf("Referer", "http://$website")
    }

    companion object {
        const val TYPE = 101
        const val DEFAULT_TITLE = "6漫画"
        private const val website = "www.sixmh6.com"
        private var ChapterHtml: String? = null

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
