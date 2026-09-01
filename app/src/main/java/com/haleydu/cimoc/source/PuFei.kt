package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.net.URLEncoder
import java.util.ArrayList
import java.util.LinkedList

class PuFei(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, Category())
    }

    private fun host(): String {
        return sourceConfigManager.getUrl("PUFEI", "http://m.pufei.cc")
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1) {
            url = StringUtils.format(
                host() + "/e/search/?searchget=1&tbname=mh&show=title,player,playadmin,bieming,pinyin,playadmin&tempid=4&keyboard=%s",
                URLEncoder.encode(keyword, "GB2312")
            )
        }
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("#detail > li")) {
            override fun parse(node: Node): Comic {
                val node_a = node.list("a")[0]
                val cid = node_a.hrefWithSplit(1)
                val title = node_a.text("h3")
                val cover = node_a.attr("div > img", "data-src")
                val author = node_a.text("dl > dd")
                val update = node.text("dl:eq(4) > dd")
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return host() + "/manhua/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.pufei.cc"))
        filter.add(UrlFilter("m.pufei.net"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = host() + "/manhua/".plus(cid)
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.main-bar > h1")
        val cover = body.src("div.book-detail > div.cont-list > div.thumb > img")
        val update = body.text("div.book-detail > div.cont-list > dl:eq(2) > dd")
        val author = body.text("div.book-detail > div.cont-list > dl:eq(3) > dd")
        val intro = body.text("#bookIntro")
        val status = isFinish(body.text("div.book-detail > div.cont-list > div.thumb > i"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var i = 0
        for (node in Node(html).list("#chapterList2 > ul > li > a")) {
            val title = node.attr("title")
            val path = node.hrefWithSplit(2)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format(host() + "/manhua/%s/%s.html", cid, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        var str = StringUtils.match("cp=\"(.*?)\"", html, 1)
        if (str != null) {
            try {
                str = DecryptionUtils.evalDecrypt(DecryptionUtils.base64Decrypt(str))
                val array = str.split(",")
                var i = 0
                while (i != array.size) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i + 1, "http://res.img.youzipi.net/" + array[i], false))
                    ++i
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

    override fun parseCheck(html: String): String {
        return Node(html).text("div.book-detail > div.cont-list > dl:eq(2) > dd")
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list: MutableList<Comic> = LinkedList()
        val body = Node(html)
        for (node in body.list("div.cont-list > ul > li")) {
            val cid = node.getChild("a").hrefWithSplit(1)
            val title = node.text("a > h3")
            val cover = node.attr("a > div > img", "data-src")
            val update = node.text("dl:eq(4) > dd")
            val author = node.text("a > dl:eq(2) > dd")
            list.add(Comic(TYPE, cid, title, cover, update, author))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", host())
    }

    private inner class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            val subject = if (args.size > CATEGORY_SUBJECT) args[CATEGORY_SUBJECT] else "manhua/update"
            val order = if (args.size > CATEGORY_ORDER) args[CATEGORY_ORDER] else "update"
            return StringUtils.format(
                host() + "/act/?act=list&page=%%d&catid=%s&ajax=1&order=%s",
                subject, order
            )
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("最近更新", "manhua/update"))
            list.add(Pair.create("漫画排行", "manhua/paihang"))
            list.add(Pair.create("少年热血", "shaonianrexue"))
            list.add(Pair.create("武侠格斗", "wuxiagedou"))
            list.add(Pair.create("科幻魔幻", "kehuan"))
            list.add(Pair.create("竞技体育", "jingjitiyu"))
            list.add(Pair.create("搞笑喜剧", "gaoxiaoxiju"))
            list.add(Pair.create("侦探推理", "zhentantuili"))
            list.add(Pair.create("恐怖灵异", "kongbulingyi"))
            list.add(Pair.create("少女爱情", "shaonvaiqing"))
            list.add(Pair.create("耽美BL", "danmeirensheng"))
            return list
        }

        override fun hasOrder(): Boolean {
            return true
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("发布", "index"))
            list.add(Pair.create("更新", "update"))
            list.add(Pair.create("人气", "view"))
            return list
        }
    }

    companion object {
        const val TYPE = 50
        const val DEFAULT_TITLE = "扑飞漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
