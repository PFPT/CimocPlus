package com.haleydu.cimoc.source

import android.net.Uri
import com.haleydu.cimoc.data.SourceConfigManager
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
import java.util.LinkedList

class MH160(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, null)
    }

    private fun host(): String {
        return sourceConfigManager.firstUrl("https://www.mh160mh.com", "MH160CC", "MH160")
    }

    private fun hostName(): String {
        return try {
            Uri.parse(host()).host ?: "www.mh160mh.com"
        } catch (_: Exception) {
            "www.mh160mh.com"
        }
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) {
            return null
        }
        val url = StringUtils.format(host() + "/statics/search.aspx?key=%s", keyword)
        return Request.Builder()
            .addHeader("Referer", host())
            .addHeader("Host", hostName())
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

    override fun getUrl(cid: String): String {
        return host() + cid
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.mh160mh.com", "(//(.+s)//)"))
        filter.add(UrlFilter("www.mh160.xyz", "(//(.+s)//)"))
        filter.add(UrlFilter("m.mh160.xyz", "/kanmanhua//(.+s)//"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = host() + cid
        return Request.Builder()
            .url(url)
            .addHeader("Referer", host())
            .addHeader("Host", hostName())
            .build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val cover = body.src(".mh-date-bgpic > a > img")
        val intro = body.text("#workint > p")
        val title = body.attr(".mh-date-bgpic > a > img", "title")
        val update = body.text("div.cy_zhangjie_top > :eq(2) > font")
        val author = body.text("span.one > em")
        val status = isFinish(body.text("p.works-info-tc > span:eq(3)"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("#mh-chapter-list-ol-0 > li > a")) {
            val title = node.text("p")
            val path = node.href()
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = host() + path
        return Request.Builder()
            .url(url)
            .addHeader("Referer", host())
            .addHeader("Host", hostName())
            .build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        var str = StringUtils.match("qTcms_S_m_murl_e=\"(.*?)\"", html, 1)
        val str_id = StringUtils.match("qTcms_S_p_id=\"(.*?)\"", html, 1)
        if (str != null) {
            try {
                str = DecryptionUtils.base64Decrypt(str)
                val array = str.split("\\$qingtiandy\\$".toRegex())
                val preUrl = imagePrefix(str_id)
                for (i in array.indices) {
                    var path = array[i] ?: continue
                    path = path.trim()
                    if (path.isEmpty()) {
                        continue
                    }
                    val url = if (path.startsWith("http://") || path.startsWith("https://")) {
                        path
                    } else if (path.startsWith("//")) {
                        "https:$path"
                    } else {
                        val p = if (!path.startsWith("/")) "/$path" else path
                        preUrl + p
                    }
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i + 1, url, false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    private fun imagePrefix(strId: String?): String {
        val server1 = sourceConfigManager.getField("MH160CC", "SERVER1", "https://mhpic5.kingwar.cn")
        val server2 = sourceConfigManager.getField("MH160CC", "SERVER2", "https://mhpic6.tgmhfc.uk")
        var id = 0
        if (!strId.isNullOrEmpty()) {
            try {
                id = Integer.parseInt(strId)
            } catch (_: Exception) {
            }
        }
        if (id > 542724) {
            return server1
        }
        return server2
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).text("div.cy_zhangjie_top > :eq(2) > font")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf(
            "User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Safari/537.36",
            "Referer", host() + "/"
        )
    }

    companion object {
        const val TYPE = 28
        const val DEFAULT_TITLE = "漫画160"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
