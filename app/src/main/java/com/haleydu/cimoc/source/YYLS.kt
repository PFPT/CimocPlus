package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedList
import java.util.regex.Pattern

class YYLS(source: Source?) : MangaParser() {

    private var _cid = ""
    private var Baseurl = "http://8comic.se/"

    init {
        init(source, Category())
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1)
            url = "http://8comic.se/%e6%90%9c%e5%b0%8b%e7%b5%90%e6%9e%9c/?w=$keyword"
        return Request.Builder()
            .url(url)
            .build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("#content #dp-widget-posts-2 > ul > li")) {
            override fun parse(node: Node): Comic {
                val cid = node.href("a")
                val title = node.text("a")
                val cover: String? = null
                val update = ""
                val author: String? = null
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://8comic.se".plus(cid)
        _cid = cid
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("#main > div > div.entry-header.cf > div > h1")
        val cover = body.src("#details > div.entry-content.rich-content > table > tbody > tr:nth-child(1) > td:nth-child(1) > img")
        val update = body.getLastChild("div.entry-content.rich-content a").text().trim()
        val author = ""
        val intro = body.text("#details > div.entry-content.rich-content > table > tbody > tr:nth-child(1) > td:nth-child(2) > p")
        val matcher = Pattern.compile("經典完結").matcher(html)
        val status = matcher.find()
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var i = 0
        for (node in Node(html).list("div.entry-content.rich-content a")) {
            val title = node.text()
            var path = node.href()
            path = path.substring(17, path.length - 1)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        Collections.reverse(list)
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val fullPath = Baseurl.plus(path)
        return Request.Builder().addHeader("Referer", StringUtils.format("http://8comic.se%s", _cid)).url(fullPath).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl>? {
        val list: MutableList<ImageUrl> = LinkedList()
        val pageMatcher = Pattern.compile("id=.*?caonima.*?src=\"(.*?)\\d{3}\\.jpg").matcher(html)
        if (!pageMatcher.find()) return null
        val numMatcher = Pattern.compile("共([\\d]*?)頁").matcher(html)
        if (!numMatcher.find()) return null
        val page = numMatcher.group(1)!!.toInt()
        for (i in 1..page) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, i, StringUtils.format("%s//%03d.jpg", pageMatcher.group(1), i), false))
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return Node(html).getLastChild("div.entry-content.rich-content a").text().trim()
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list: MutableList<Comic> = ArrayList()
        val body = Node(html)
        for (node in body.list(".nag.cf > div")) {
            var cid = node.href(".thumb > a")
            cid = StringUtils.match("http://8comic.se(/\\d+)/", cid, 1) ?: continue
            val title = node.attr(".thumb > a", "title")
            val cover = node.src(".thumb > a img")
            val update = ""
            list.add(Comic(TYPE, cid, title, cover, update, null))
        }
        return list
    }

    private class Category : MangaCategory() {
        override fun getFormat(vararg args: String): String {
            return StringUtils.format(
                "http://8comic.se/category/漫畫分類/%s/page/%%d/",
                args[CATEGORY_SUBJECT]
            )
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("完结漫画", "經典完結"))
            list.add(Pair.create("连载漫画", "漫畫連載"))
            list.add(Pair.create("冒险", "冒險"))
            list.add(Pair.create("格斗", "格鬥"))
            list.add(Pair.create("科幻", "科幻"))
            list.add(Pair.create("竞技", "競技"))
            list.add(Pair.create("侦探", "偵探"))
            list.add(Pair.create("恐怖", "恐怖"))
            list.add(Pair.create("搞笑", "搞笑"))
            list.add(Pair.create("校园", "校園"))
            list.add(Pair.create("魔幻", "魔幻"))
            list.add(Pair.create("魔法", "魔法"))
            list.add(Pair.create("少女", "少女"))
            list.add(Pair.create("少男", "少男"))
            list.add(Pair.create("其他", "其他"))
            return list
        }
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://8comic.se".plus(_cid))
    }

    companion object {
        const val TYPE = 9
        const val DEFAULT_TITLE = "YYLS"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
