package com.haleydu.cimoc.source

import android.util.Log
import com.haleydu.cimoc.core.Manga
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
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import java.util.LinkedList
import java.util.regex.Pattern

class YKMH(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, null)
    }

    private fun mHost(): String {
        var url = sourceConfigManager.getUrl("YKMH", "https://m.ykmh.net")
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        return url
    }

    private fun imageHost(): String {
        return sourceConfigManager.getUrl("YKMH_SERVER", "http://js.haotuyk.top")
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        Log.d("SourceSearch:", keyword.toString())
        return Request.Builder().url(mHost() + "search/?keywords=" + keyword + "&page=" + page)
            .addHeader("referer", mHost() + "search")
            .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36")
            .build()
    }

    @Throws(JSONException::class)
    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("#update_list > div.UpdateList > div")) {
            override fun parse(node: Node): Comic {
                val titleN = node.getChild("div.itemTxt > a")
                val cid = titleN.hrefWithLastSplit()
                val title = titleN.text()
                val cover = node.attr("div.itemImg > a > img", "src")
                val Update = node.text("p.txtItme > span.date")
                val Author = node.text("p > a")
                return Comic(TYPE, cid, title, cover, Update, Author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return StringUtils.format("%smanhua/%s", mHost(), cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.ykmh.net", "/manhua/(\\w.+)/"))
        filter.add(UrlFilter("m.ykmh.com", "/manhua/(\\w.+)/"))
    }

    override fun getHeader(): Headers {
        return Headers.headersOf(
            "Referer", mHost() + "search/",
            "user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"
        )
    }

    override fun getInfoRequest(cid: String): Request {
        Log.d("SourceInfo:", cid.toString())
        return Request.Builder().url(mHost().plus("manhua/").plus(cid).plus("/"))
            .addHeader("referer", mHost() + "search")
            .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36")
            .build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val info = body.getChild("div.Introduct_Sub")
        val title = body.text("div#comicName")
        val cover = info.getChild("div#Cover > *").src()
        val update = info.text("p.txtItme > span.date")
        val author = info.getParent("p.txtItme > span.icon01").text()
        val intro = body.getParent("p#full-des #showmore-des").text()
        val isFinish = info.getParent("p.txtItme > span.icon01").text()
        var finish = false
        if (isFinish.contains("完结")) finish = true
        comic.setInfo(title, cover, update, intro, author, finish)
        return comic
    }

    override fun parseChapter(html: String): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        val body = Node(html)
        for (node in body.list("div.chapter-warp ul.Drama > li > a")) {
            val title = node.text()
            val path = node.hrefWithSubString(1)
            list.add(Chapter(title, path))
        }
        return list
    }

    @Throws(JSONException::class)
    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        val body = Node(html)
        var i = 0
        for (node in body.list("div.chapter-warp ul.Drama > li > a")) {
            val title = node.text()
            val path = node.hrefWithSubString(1)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        Log.d("SourceImage:", path.toString())
        return Request.Builder().url(mHost().plus(path))
            .addHeader("referer", mHost() + "search")
            .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36")
            .build()
    }

    @Throws(Manga.NetworkErrorException::class, JSONException::class)
    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl>? {
        val list: MutableList<ImageUrl> = LinkedList()
        val matcher = Pattern.compile("var chapterImages\\s*=\\s*\\[(.+?)]").matcher(html)
        if (!matcher.find()) {
            return null
        }
        val CDATA = String.format("[%s]", matcher.group(1))
        try {
            val array = JSONArray(CDATA)
            for (i in 0 until array.length()) {
                val url = StringUtils.format("%s%s", imageHost(), array.getString(i))
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i + 1).toLong()
                list.add(ImageUrl(id, comicChapter, i + 1, url, false))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e("parseImages", "parseImages Error", e)
            return null
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return Node(html).text("p.txtItme > span.date")
    }

    override fun getTitle(): String {
        return DEFAULT_TITLE
    }

    companion object {
        const val TYPE = 91
        const val DEFAULT_TITLE = "优酷漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
