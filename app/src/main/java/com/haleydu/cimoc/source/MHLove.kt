package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.util.LinkedList

class MHLove(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) {
            return null
        }
        val url = StringUtils.format("$baseUrl/statics/search.aspx?key=%s", keyword)
        return Request.Builder()
            .addHeader("Referer", baseUrl)
            .url(url)
            .build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".mh-search-result > ul > li")) {
            override fun parse(node: Node): Comic {
                val cover = node.attr("img", "src")
                val title = node.text("h4").orEmpty().trim()
                val cid = node.attr(".mh-works-info > a", "href")
                val update = node.text(".mh-up-time.fr").orEmpty().replace("最后更新时间：", "")
                return Comic(TYPE, cid, title, cover, update, null)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = baseUrl + cid
        return Request.Builder().url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val cover = body.src(".mh-date-bgpic > a > img")
        val intro = body.text("#workint > p")
        val title = body.attr(".mh-date-bgpic > a > img", "title")
        val update = body.text("div.mh-chapter-record > span > em")
        val author = body.text("span.one > em")
        val status = isFinish(body.text("p.works-info-tc > span:eq(3)"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("#mh-chapter-list-ol-0 > li > a")) {
            val title = node.text()
            val path = node.href()
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = baseUrl + path
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        var str = StringUtils.match("qTcms_S_m_murl_e=\"(.*?)\"", html, 1)
        if (str != null) {
            try {
                str = DecryptionUtils.base64Decrypt(str)
                val array = str.split("\\\$qingtiandy\\\$".toRegex())
                var preUrl = ""
                if (!array[0].contains("http")) {
                    preUrl = "http://www.9qv.cn"
                }
                for (i in array.indices) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i + 1, preUrl + array[i], false))
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
        return Node(html).text("div.mh-chapter-record > span > em")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", baseUrl)
    }

    companion object {
        const val TYPE = 27
        const val DEFAULT_TITLE = "漫画Love"
        const val baseUrl = "http://www.php06.com"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, false)
        }
    }
}
