package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.CATEGORY_AREA
import com.haleydu.cimoc.parser.Category.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.Category.CATEGORY_YEAR
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
import org.json.JSONArray
import java.util.LinkedList

class MH57(source: Source?) : MangaParser() {

    init {
        init(source, Category())
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        val url = StringUtils.format("http://m.wuqimh.com/search/q_%s-p-%d", keyword, page)
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator? {
        val body = Node(html)
        for (node in body.list("div.book-result > div.pager-cont > span.pager > span.current")) {
            try {
                if (Integer.parseInt(node.text()) < page) {
                    return null
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
        return object : NodeIterator(body.list("#data_list > li")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit("a:eq(0)", 0)
                val title = node.text("a:eq(0) > h3")
                val cover = node.attr("a:eq(0) > div.thumb > img", "data-src")
                val update = node.text("dl:eq(4) > dd")
                val author = node.text("dl:eq(1) > a > dd")
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "http://m.wuqimh.com/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.wuqimh.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://m.wuqimh.com/".plus(cid)
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.main-bar > h1")
        val cover = body.src("div.book-detail > div.cont-list > div.thumb > img")
        val update = body.text("div.book-detail > div.cont-list > dl:eq(7) > dd")
        val author = body.text("div.book-detail > div.cont-list > dl:eq(3) > dd")
        val intro = body.text("#bookIntro")
        val status = isFinish(body.text("div.book-detail > div.cont-list > div.thumb > i"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        var i = 0
        for (node in body.list("#chapterList > ul > li > a")) {
            val title = node.text()
            val path = node.hrefWithSplit(1)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://m.wuqimh.com/%s/%s.html", cid, path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val packed = StringUtils.match("eval(.*?)\\n", html, 1)
        if (packed != null) {
            val result = DecryptionUtils.evalDecrypt(packed)
            val jsonString = StringUtils.match("'fs':\\s*(\\[.*?\\])", result, 1) ?: return list
            try {
                val array = JSONArray(jsonString)
                val size = array.length()
                for (i in 0 until size) {
                    var url = array.getString(i)
                    if (url.indexOf("http://") == -1) {
                        url = servers[0] + url
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

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).text("div.book-detail > div.cont-list > dl:eq(7) > dd")
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = ArrayList<Comic>()
        val body = Node(html)
        for (node in body.list("span.pager > span.current")) {
            try {
                if (Integer.parseInt(node.text()) < page) {
                    return list
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
        for (node in body.list("#contList > li")) {
            val cid = node.hrefWithSplit("a", 0)
            val title = node.attr("a", "title")
            val cover = node.attr("a > img", "data-src")
            val update = node.textWithSubstring("span.updateon", 4, 14)
            list.add(Comic(TYPE, cid, title, cover, update, null))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://m.wuqimh.com/")
    }

    private class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            return StringUtils.format(
                "http://www.5qmh.com/list/area-%s-smid-%s-year-%s-lz-%s-order-%s-p-%%d",
                args[CATEGORY_AREA], args[CATEGORY_SUBJECT], args[CATEGORY_YEAR], args[CATEGORY_PROGRESS], args[CATEGORY_ORDER]
            )
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("热血", "1"))
            list.add(Pair.create("武侠", "2"))
            list.add(Pair.create("搞笑", "3"))
            list.add(Pair.create("耽美", "4"))
            list.add(Pair.create("爱情", "5"))
            list.add(Pair.create("科幻", "6"))
            list.add(Pair.create("魔法", "7"))
            list.add(Pair.create("神魔", "8"))
            list.add(Pair.create("竞技", "9"))
            list.add(Pair.create("格斗", "10"))
            list.add(Pair.create("机战", "11"))
            list.add(Pair.create("体育", "12"))
            list.add(Pair.create("运动", "13"))
            list.add(Pair.create("校园", "14"))
            list.add(Pair.create("励志", "15"))
            list.add(Pair.create("历史", "16"))
            list.add(Pair.create("伪娘", "17"))
            list.add(Pair.create("百合", "18"))
            list.add(Pair.create("后宫", "19"))
            list.add(Pair.create("治愈", "20"))
            list.add(Pair.create("美食", "21"))
            list.add(Pair.create("推理", "22"))
            list.add(Pair.create("悬疑", "23"))
            list.add(Pair.create("恐怖", "24"))
            list.add(Pair.create("职场", "25"))
            list.add(Pair.create("BL", "26"))
            list.add(Pair.create("剧情", "27"))
            list.add(Pair.create("生活", "28"))
            list.add(Pair.create("幻想", "29"))
            list.add(Pair.create("战争", "30"))
            list.add(Pair.create("仙侠", "33"))
            list.add(Pair.create("性转换", "40"))
            list.add(Pair.create("冒险", "41"))
            list.add(Pair.create("其他", "32"))
            return list
        }

        override fun hasArea(): Boolean {
            return true
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("日本", "日本"))
            list.add(Pair.create("港台", "港台"))
            list.add(Pair.create("欧美", "欧美"))
            list.add(Pair.create("韩国", "韩国"))
            list.add(Pair.create("国产", "国产"))
            list.add(Pair.create("其它", "其它"))
            return list
        }

        override fun hasYear(): Boolean {
            return true
        }

        override fun getYear(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("2017", "2017"))
            list.add(Pair.create("2016", "2016"))
            list.add(Pair.create("2015", "2015"))
            list.add(Pair.create("2014", "2014"))
            list.add(Pair.create("2013", "2013"))
            list.add(Pair.create("2012", "2012"))
            list.add(Pair.create("2011", "2011"))
            list.add(Pair.create("2010", "2010"))
            list.add(Pair.create("2009", "2009"))
            list.add(Pair.create("2008", "2008"))
            list.add(Pair.create("2007", "2007"))
            list.add(Pair.create("2006", "2006"))
            list.add(Pair.create("2005", "2005"))
            list.add(Pair.create("2004", "2004"))
            list.add(Pair.create("2003", "2003"))
            list.add(Pair.create("2002", "2002"))
            list.add(Pair.create("2001", "2001"))
            list.add(Pair.create("2000", "2000"))
            list.add(Pair.create("1990", "1990"))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("全部", ""))
            list.add(Pair.create("连载", "1"))
            list.add(Pair.create("完结", "2"))
            return list
        }

        override fun hasOrder(): Boolean {
            return true
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("更新", "addtime"))
            list.add(Pair.create("发布", "id"))
            list.add(Pair.create("人气", "hits"))
            list.add(Pair.create("评分", "gold"))
            return list
        }
    }

    companion object {
        const val TYPE = 8
        const val DEFAULT_TITLE = "57漫画"
        private val servers = arrayOf("http://images.lancaier.com")

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
