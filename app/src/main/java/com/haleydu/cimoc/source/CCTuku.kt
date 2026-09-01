package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_AREA
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.util.LinkedList

class CCTuku(source: Source?) : MangaParser() {

    init {
        init(source, Category())
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page == 1) {
            val url = StringUtils.format("http://m.tuku.cc/search-%s/?language=1", keyword)
            return Request.Builder().url(url).build()
        } else return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("ul.searchResultList > li")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit("a", 1)
                val title = node.text("p.title")
                val cover = node.src("a > img")
                return Comic(TYPE, cid, title, cover, null, null)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "http://m.tuku.cc/comic/".plus(cid)
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://m.tuku.cc/comic/".plus(cid)
        return Request.Builder()
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .url(url)
            .build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.detailTop > div.content > div.info > p.title")
        val cover = body.src("div.detailTop > div.content > img")
        val update = body.textWithSubstring("#chapter > div > div > div.top > span", 0, 10)
        val author = body.text("div.detailTop > div.content > div.info > p:eq(1) > a")
        val intro = body.text("div.detailContent > p:eq(1)")
        comic.setInfo(title, cover, update, intro, author, false)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var i = 0
        for (node in body.list("#chapter > div > div > ul > li > a")) {
            var title = node.text()
            var path = node.hrefWithSplit(2)
            if (path == null) {
                path = ""
            }
            if (title == null) {
                title = ""
            }
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    private var _cid: String? = null
    private var _path: String? = null

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://m.tuku.cc/comic/%s/%s/", cid, path)
        _cid = cid
        _path = path
        return Request.Builder()
            .addHeader("Referer", StringUtils.format("http://m.tuku.cc/comic/%s/", cid))
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = ArrayList<ImageUrl>()
        val page = 10
        for (i in 1..page) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, i, StringUtils.format("http://m.tuku.cc/comic/%s/%s/p%s/", _cid, _path, i), true))
        }
        return list
    }

    override fun getLazyRequest(url: String): Request {
        return Request.Builder()
            .addHeader("Referer", StringUtils.format("http://m.tuku.cc/comic/%s/%s/", _cid, _path))
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .url(url).build()
    }

    override fun parseLazy(html: String, url: String): String? {
        val body = Node(html)
        return body.href("div.readForm > a")
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).textWithSubstring("div.book > div > div.row > div:eq(1) > div > dl:eq(5) > dd > font", 0, 10)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        val body = Node(html)
        val total = Integer.parseInt(StringUtils.match("\\d+", body.text("div.title-banner > div > h1") ?: "", 0) ?: "0")
        if (page <= total) {
            for (node in body.list("div.main-list > div > div > div")) {
                val cid = node.hrefWithSplit("div:eq(1) > div:eq(0) > a", 1)
                val title = node.text("div:eq(1) > div:eq(0) > a")
                val cover = node.src("div:eq(0) > a > img")
                val update = node.text("div:eq(1) > div:eq(1) > dl:eq(3) > dd > font")
                val author = node.text("div:eq(1) > div:eq(1) > dl:eq(1) > dd > a")
                list.add(Comic(TYPE, cid, title, cover, update, author))
            }
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://m.tuku.cc")
    }

    private class Category : MangaCategory() {
        override fun getFormat(vararg args: String): String {
            return if (args[CATEGORY_SUBJECT] != "") {
                StringUtils.format("http://m.tuku.cc/list/list_%s_%%d.htm", args[CATEGORY_SUBJECT])
            } else if (args[CATEGORY_AREA] != "") {
                StringUtils.format("http://m.tuku.cc/list/comic_%s_%%d.htm", args[CATEGORY_AREA])
            } else if (args[CATEGORY_PROGRESS] != "") {
                StringUtils.format("http://m.tuku.cc/%s/%%d", args[CATEGORY_PROGRESS])
            } else {
                "http://m.tuku.cc/newest/%d"
            }
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("魔幻", "1"))
            list.add(Pair.create("动作", "2"))
            list.add(Pair.create("热血", "5"))
            list.add(Pair.create("爱情", "4"))
            list.add(Pair.create("武侠", "15"))
            list.add(Pair.create("搞笑", "7"))
            list.add(Pair.create("校园", "20"))
            list.add(Pair.create("竞技", "3"))
            list.add(Pair.create("科幻", "11"))
            list.add(Pair.create("悬疑", "10"))
            list.add(Pair.create("拳皇", "12"))
            list.add(Pair.create("恐怖", "9"))
            list.add(Pair.create("美女", "19"))
            list.add(Pair.create("励志", "8"))
            list.add(Pair.create("历史", "22"))
            list.add(Pair.create("百合", "35"))
            list.add(Pair.create("猎奇", "39"))
            list.add(Pair.create("职场", "38"))
            list.add(Pair.create("短篇", "34"))
            list.add(Pair.create("美食", "31"))
            list.add(Pair.create("四格", "30"))
            list.add(Pair.create("同人", "18"))
            list.add(Pair.create("青年", "17"))
            list.add(Pair.create("游戏", "14"))
            list.add(Pair.create("街霸", "13"))
            list.add(Pair.create("萌系", "6"))
            list.add(Pair.create("机战", "43"))
            list.add(Pair.create("节操", "42"))
            list.add(Pair.create("伪娘", "41"))
            list.add(Pair.create("后宫", "40"))
            list.add(Pair.create("耽美", "16"))
            list.add(Pair.create("其它", "33"))
            list.add(Pair.create("轻小说", "21"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("香港", "1"))
            list.add(Pair.create("日本", "2"))
            list.add(Pair.create("欧美", "5"))
            list.add(Pair.create("台湾", "4"))
            list.add(Pair.create("韩国", "15"))
            list.add(Pair.create("大陆", "7"))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("连载", "lianzai"))
            list.add(Pair.create("完结", "wanjie"))
            return list
        }
    }

    companion object {
        const val TYPE = 3
        const val DEFAULT_TITLE = "CC图库"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, false)
        }
    }
}
