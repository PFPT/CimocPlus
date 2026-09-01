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

class QiManWu(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) return null
        val url = baseUrl + "/spotlight/?keyword=" + keyword
        val body: RequestBody = FormBody.Builder()
            .add("keyword", keyword)
            .build()
        return Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1")
            .build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".search-result > .comic-list-item")) {
            override fun parse(node: Node): Comic {
                val cid = node.href("a")
                val title = node.text("p.comic-name")
                val cover = node.attr("img", "src")
                val author = node.text("p.comic-author")
                return Comic(TYPE, cid, title, cover, null, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return baseUrl.plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter(baseUrl))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = baseUrl.plus(cid)
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        ChapterHtml = html
        val body = Node(html)
        var update = body.text(".box-back2 > :eq(4)")
        if (!update.contains("更新时间：")) update = body.text(".box-back2 > :eq(3)")
        update = update.replace("更新时间：", "")
        val title = body.text(".box-back2 > h1")
        val intro = body.text("span.comic-intro")
        val author = body.text(".box-back2 > :eq(2)")
        val cover = body.src(".box-back1 > img")
        val status = isFinish(body.text(".box-back2 > p.txtItme.c1"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun getChapterRequest(html: String, cid: String): Request {
        val url = "http://qiman6.com/bookchapter/"
        val id = StringUtils.match(" data: \\{ \"id\":(.*?),", html, 1)?.trim() ?: ""
        val id2 = StringUtils.match(", \"id2\":(.*?)\\},", html, 1)?.trim() ?: ""
        val body: RequestBody = FormBody.Builder().add("id", id).add("id2", id2).build()
        return Request.Builder().url(url).post(body)
            .addHeader("Referer", baseUrl)
            .addHeader("Host", "qiman6.com")
            .build()
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        try {
            var k = 0
            for (node in Node(ChapterHtml!!).list("div.catalog-list > ul > li")) {
                val title = node.text("a")
                val path = node.attr("li", "data-id")
                list.add(Chapter((sourceComic.toString() + "000" + k++).toLong(), sourceComic, title, path))
            }
            val array = JSONArray(html)
            var i = 0
            while (i != array.length()) {
                val chapter = array.getJSONObject(i)
                val title = chapter.getString("name")
                val path = chapter.getString("id")
                list.add(Chapter((sourceComic.toString() + "000" + k++).toLong(), sourceComic, title, path))
                ++i
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://qiman6.com%s%s.html", cid, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val str = StringUtils.match("eval\\((.*?\\}\\))\\)", html, 0)
        try {
            val packedStr = str!!
            var str1 = DecryptionUtils.evalDecrypt(packedStr, "newImgs")
            if (str1.isEmpty()) {
                str1 = DecryptionUtils.evalDecrypt(packedStr)
            }
            val array = str1.split(",")
            for (i in array.indices) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, i + 1, array[i], false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        val body = Node(html)
        var update = body.text(".box-back2 > :eq(4)")
        if (!update.contains("更新时间：")) update = body.text(".box-back2 > :eq(3)")
        update = update.replace("更新时间：", "")
        return update
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1")
    }

    companion object {
        const val TYPE = 53
        const val DEFAULT_TITLE = "奇漫屋"
        const val baseUrl = "http://qiman6.com"
        private var ChapterHtml: String? = null

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
