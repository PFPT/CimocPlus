package com.haleydu.cimoc.source

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
import org.json.JSONObject
import java.util.Collections
import java.util.LinkedList

class IKanman(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    private var referer = ""

    init {
        init(source, null)
    }

    private fun wwwHost(): String {
        return sourceConfigManager.firstUrl("https://www.manhuagui.com", "IKANMAN")
    }

    private fun comicHost(): String {
        return sourceConfigManager.firstUrl("https://tw.manhuagui.com", "IKANMANTW", "IKANMAN")
    }

    private fun imageHost(): String {
        return sourceConfigManager.firstUrl("https://i.hamreus.com", "IKANMANSERVER")
    }

    private fun siteHosts(fallback: String, vararg keys: String): List<String> {
        val hosts = sourceConfigManager.listUrls(fallback, *keys).toMutableList()
        val extra = arrayOf(
            "https://www.manhuagui.com",
            "https://tw.manhuagui.com",
            "https://m.manhuagui.com",
            "https://www.manhuagui.net",
            "https://www.manhuagui8.com"
        )
        for (host in extra) {
            if (!hosts.contains(host)) {
                hosts.add(host)
            }
        }
        return hosts
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var encoded = keyword
        try {
            encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        } catch (_: Exception) {
        }
        val path = StringUtils.format("/s/%s_p%d.html", encoded, page)
        val builder = Request.Builder()
            .addHeader("User-Agent", UA)
            .url(wwwHost() + path)
        sourceConfigManager.applyHostFallbacks(builder, path, siteHosts("https://www.manhuagui.com", "IKANMAN", "IKANMANTW"))
        return builder.build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("li.cf")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit("a.bcover", 1)
                val title = node.text(".book-detail > dl > dt > a")
                val cover = node.attr("a.bcover > img", "src")
                return Comic(TYPE, cid, title, cover, "", "")
            }
        }
    }

    override fun getUrl(cid: String): String {
        return comicHost() + "/comic/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.manhuagui.com"))
        filter.add(UrlFilter("tw.manhuagui.com"))
        filter.add(UrlFilter("m.manhuagui.com"))
        filter.add(UrlFilter("www.manhuagui.net"))
        filter.add(UrlFilter("www.manhuagui8.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        val path = "/comic/".plus(cid).plus("/")
        val builder = Request.Builder()
            .addHeader("User-Agent", UA)
            .url(comicHost() + path)
        sourceConfigManager.applyHostFallbacks(builder, path, siteHosts("https://tw.manhuagui.com", "IKANMANTW", "IKANMAN"))
        return builder.build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.book-title > h1")
        val cover = body.src("p.hcover > img")
        val update = body.text("div.chapter-bar > span.fr > span:eq(1)")
        val author = body.attr("ul.detail-list > li:eq(1) > span:eq(1) > a", "title")
        val intro = body.text("#intro-cut")
        val status = isFinish(body.text("div.chapter-bar > span.fr > span:eq(0)"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var body = Node(html)
        val baseText = body.id("__VIEWSTATE").attr("value")
        if (!StringUtils.isEmpty(baseText)) {
            body = Node(DecryptionUtils.LZ64Decrypt(baseText) ?: "")
        }
        var i = 0
        for (node in body.list("div.chapter-list")) {
            val uls = node.list("ul").toMutableList()
            Collections.reverse(uls)
            for (ul in uls) {
                for (li in ul.list("li > a")) {
                    val title = li.attr("title")
                    val path = li.hrefWithSplit(2)
                    list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
                }
            }
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val suffix = StringUtils.format("/comic/%s/%s.html", cid, path)
        referer = comicHost() + suffix
        val builder = Request.Builder()
            .addHeader("User-Agent", UA)
            .url(referer)
        sourceConfigManager.applyHostFallbacks(builder, suffix, siteHosts("https://tw.manhuagui.com", "IKANMANTW", "IKANMAN"))
        return builder.build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        var packed = StringUtils.match("\\(function\\(p,a,c,k,e,d\\).*?0,\\{\\}\\)\\)", html, 0)
        if (packed != null) {
            try {
                val replaceable = StringUtils.split(packed, ",", -3) ?: ""
                val fake = StringUtils.split(replaceable, "'", 1)
                val real = DecryptionUtils.LZ64Decrypt(fake)
                packed = packed.replace(replaceable, StringUtils.format("'%s'.split('|')", real ?: ""))
                val result = DecryptionUtils.evalDecrypt(packed)
                val jsonString = result.substring(12, result.length - 12)
                val obj = JSONObject(jsonString)
                val path = obj.getString("path")
                val e = obj.getJSONObject("sl").getString("e")
                val m = obj.getJSONObject("sl").getString("m")
                val array = obj.getJSONArray("files")
                for (i in 0 until array.length()) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    val url = StringUtils.format("%s%s%s?e=%s&m=%s", imageHost(), path, array.getString(i), e, m)
                    list.add(ImageUrl(id, comicChapter, i + 1, url, false))
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
        return Node(html).text("div.chapter-bar > span.fr > span:eq(1)")
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = ArrayList<Comic>()
        val body = Node(html)
        for (node in body.list("#AspNetPager1 > span.current")) {
            try {
                if (Integer.parseInt(node.text()) < page) {
                    return list
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
        for (node in body.list("#contList > li")) {
            val cid = node.hrefWithSplit("a", 1)
            val title = node.attr("a", "title")
            var cover = node.src("a > img")
            if (StringUtils.isEmpty(cover)) {
                cover = node.attr("a > img", "data-src")
            }
            val update = node.textWithSubstring("span.updateon", 4, 14)
            list.add(Comic(TYPE, cid, title, cover, update, null))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf(
            "Referer", referer,
            "User-Agent", UA
        )
    }

    companion object {
        const val TYPE = 0
        const val DEFAULT_TITLE = "漫画柜"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, false)
        }
    }
}
