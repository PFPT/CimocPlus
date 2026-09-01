package com.haleydu.cimoc.source

import com.haleydu.cimoc.data.PreferenceManager
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
import java.util.LinkedList

class HHAAZZ(source: Source?, preferenceManager: PreferenceManager) : MangaParser() {

    private var title = ""
    private var _path: String? = null

    init {
        init(source, null)
        baseUrl = preferenceManager.getString(PreferenceManager.PREF_HHAAZZ_BASEURL, "") ?: ""
        sw = preferenceManager.getString(PreferenceManager.PREF_HHAAZZ_SW, "") ?: ""
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page == 1) {
            val url = baseUrl + "/comic/?act=search&st=".plus(keyword)
            return Request.Builder().url(url).build()
        }
        return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("div.cComicList > li")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit("a", 1)
                val title = node.attr("a", "title")
                val cover = node.src("a > img")
                return Comic(TYPE, cid, title, cover, null, null)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return baseUrl + "/comic/".plus(cid)
    }

    override fun getInfoRequest(cid: String): Request {
        val url = StringUtils.format(baseUrl + "/manhua/%s.html", cid)
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val cover = body.src("#about_style > img")
        var index = 0
        var update = ""
        var intro = ""
        var author = ""
        var status = false
        for (node in body.list("#about_kit > ul > li")) {
            when (index++) {
                0 -> title = node.getChild("h1").text().orEmpty().trim()
                1 -> author = node.text().orEmpty().replace("作者:", "").trim()
                2 -> {
                    val test = node.text().orEmpty().replace("状态:", "").trim()
                    status = "连载" !== test
                }
                4 -> update = node.text().orEmpty().replace("更新:", "").trim()
                7 -> intro = node.text().orEmpty().replace("简介", "").trim().substring(1)
            }
        }
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var i = 0
        for (node in body.list(".cVolList > ul")) {
            for (cnode in node.list("li")) {
                val title = cnode.attr("a", "title").orEmpty().replace(this.title, "").trim()
                val path = cnode.href("a")
                list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
            }
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = baseUrl + "".plus(path)
        _path = path
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val pathId = Node.splitHref(_path, 0)
        val pathS = Node.splitHref(_path, 4)
        val body = Node(html)
        var i = 1
        for (node in body.list("#iPageHtm > a")) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(
                ImageUrl(
                    id, comicChapter, i,
                    StringUtils.format(baseUrl + "/%s/%d.html?s=%s&d=0", pathId, i, pathS),
                    true
                )
            )
            i++
        }
        return list
    }

    override fun getLazyRequest(url: String): Request {
        return Request.Builder().url(url).build()
    }

    override fun parseLazy(html: String, url: String): String? {
        val body = Node(html)
        val imgEleIds = arrayOf("img1021", "img2391", "img7652", "imgCurr")
        var imgKey: String? = null
        for (i in imgEleIds.indices) {
            imgKey = body.attr("#".plus(imgEleIds[i]), "name")
            if (imgKey != null) break
        }
        val servers = body.attr("#hdDomain", "value").orEmpty().split("|")
        if (imgKey != null) {
            return servers[0] + unsuan(imgKey)
        }
        return null
    }

    private fun unsuan(s: String): String {
        val su = baseUrl.replace("http://", "")
        var b = false
        var str = s
        val swParts = sw.split("|".toRegex())
        for (i in swParts.indices) {
            if (su.indexOf(swParts[i]) > -1) {
                b = true
                break
            }
        }
        if (!b)
            return ""
        val x = str.substring(str.length - 1)
        val w = "abcdefghijklmnopqrstuvwxyz"
        val xi = w.indexOf(x) + 1
        val sk = str.substring(str.length - xi - 12, str.length - xi - 1)
        str = str.substring(0, str.length - xi - 12)
        val k = sk.substring(0, sk.length - 1)
        val f = sk.substring(sk.length - 1)
        for (i in k.indices) {
            str = str.replace(k.substring(i, i + 1), Integer.toString(i))
        }
        val ss = str.split(f.toRegex())
        str = ""
        for (i in ss.indices) {
            str += fromCharCode(Integer.parseInt(ss[i]))
        }
        return str
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).textWithSubstring("div.main > div > div.pic > div.con > p:eq(5)", 5)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        val body = Node(html)
        for (node in body.list("li.clearfix > a.pic")) {
            val cid = node.hrefWithSplit(1)
            val title = node.text("div.con > h3")
            val cover = node.src("img")
            val update = node.textWithSubstring("div.con > p > span", 0, 10)
            val author = node.text("div.con > p:eq(1)")
            list.add(Comic(TYPE, cid, title, cover, update, author))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", baseUrl)
    }

    companion object {
        const val TYPE = 2
        const val DEFAULT_TITLE = "汗汗酷漫"

        @JvmField
        var baseUrl = ""

        @JvmField
        var sw = ""

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }

        @JvmStatic
        fun fromCharCode(vararg codePoints: Int): String {
            return String(codePoints, 0, codePoints.size)
        }
    }
}
