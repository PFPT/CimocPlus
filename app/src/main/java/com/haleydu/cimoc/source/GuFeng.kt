package com.haleydu.cimoc.source

import com.google.common.collect.Lists
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.LinkedList

class GuFeng(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, null)
    }

    private fun host(): String {
        return sourceConfigManager.firstUrl("https://m.gufengmh.com", "GuFengManHua", "GuFengWang", "GUFENG")
    }

    private fun imageServer(): String {
        return sourceConfigManager.firstUrl("https://res.xiaoqinre.com", "GUFENGSERVER", "GuFengManHua", "GuFengWang")
    }

    private fun siteHosts(): List<String> {
        val hosts = sourceConfigManager.listUrls(
            "https://m.gufengmh.com", "GuFengManHua", "GuFengWang", "GUFENG"
        ).toMutableList()
        val extra = arrayOf(
            "https://m.gufengmh8.com",
            "https://m.gufengmh9.com",
            "https://m.gufengmanhua.com"
        )
        for (extraHost in extra) {
            if (!hosts.contains(extraHost)) {
                hosts.add(extraHost)
            }
        }
        return hosts
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) {
            return null
        }
        val path = "/search/?keywords=" + URLEncoder.encode(keyword, "UTF-8")
        val hosts = siteHosts()
        val primary = if (hosts.isEmpty()) "https://m.gufengmh.com" else hosts[0]
        val builder = Request.Builder().url(primary + path)
        sourceConfigManager.applyHostFallbacks(builder, path, hosts)
        return builder.build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("div.UpdateList > div.itemBox")) {
            override fun parse(node: Node): Comic {
                var cover = node.attr("div.itemImg > a > mip-img", "src")
                if (cover.isNullOrEmpty()) {
                    cover = node.attr("div.itemImg > a > img", "src")
                }
                if (cover.isNullOrEmpty()) {
                    cover = node.attr("div.itemImg img", "data-src")
                }
                val title = node.text("div.itemTxt > a")
                val href = node.attr("div.itemTxt > a", "href")
                var cid = href?.replaceFirst("https?://[^/]+/manhua/".toRegex(), "") ?: ""
                if (cid.endsWith("/")) {
                    cid = cid.substring(0, cid.length - 1)
                }
                val update = node.text("div.itemTxt > p:eq(3) > span.date")
                val author = node.text("div.itemTxt > p:eq(1)")
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = host() + "/manhua/".plus(cid) + "/"
        return Request.Builder().url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val cover = body.src("#Cover > mip-img")
        val intro = body.text("div.comic-view.clearfix > p")
        val title = body.text("h1.title")
        val update = body.text("div.pic > dl:eq(4) > dd")
        val author = body.text("div.pic > dl:eq(2) > dd")
        val status = isFinish("连载")
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("ul[id^=chapter-list] > li > a")) {
            val title = node.text()
            val path = node.hrefWithSplit(2)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format(host() + "/manhua/%s/%s.html", cid, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val str = StringUtils.match("chapterImages = \\[(.*?)\\]", html, 1)
        if (str != null) {
            try {
                val array = str.split(",")
                val urlPrev = StringUtils.match("chapterPath = \"(.*?)\"", html, 1)
                for (i in array.indices) {
                    val s = array[i].substring(1, array[i].length - 1)
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i + 1, imageServer() + "/" + urlPrev + s, false))
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
        return Node(html).text("div.pic > dl:eq(4) > dd")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36")
    }

    companion object {
        const val TYPE = 25
        const val DEFAULT_TITLE = "古风漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
