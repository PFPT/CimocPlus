package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.JsonIterator
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.util.LinkedList
import java.util.regex.Pattern

class BuKa(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        val url = "http://m.buka.cn/search/ajax_search"
        val data = FormBody.Builder()
            .add("key", keyword)
            .add("start", (15 * (page - 1)).toString())
            .add("count", "15")
            .build()
        return Request.Builder()
            .url(url)
            .post(data)
            .build()
    }

    override fun getSearchIterator(json: String, page: Int): SearchIterator? {
        return try {
            val obj = JSONObject(json)
            val dataObject = obj.getJSONObject("datas")
            val dataArray = dataObject.getJSONArray("items")
            object : JsonIterator(dataArray) {
                override fun parse(`object`: JSONObject): Comic? {
                    return try {
                        val cid = `object`.getString("mid")
                        val title = `object`.getString("name")
                        val cover = `object`.getString("logo")
                        val author = `object`.getString("author")
                        Comic(TYPE, cid, title, cover, null, author)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun getUrl(cid: String): String {
        return "http://m.buka.cn/m/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.buka.cn"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://m.buka.cn/m/".plus(cid)
        return Request.Builder()
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("p.mangadir-glass-name")
        val cover = body.src(".mangadir-glass-img > img")
        val update = body.text("span.top-title-right")
        val author = body.text(".mangadir-glass-author")
        val intro = body.text("span.description_intro")
        val status = isFinish("连载中")
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        var i = 0
        for (node in Node(html).list("div.chapter-center > a")) {
            val title = node.text()
            val path = node.href().orEmpty().split("/")[3]
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://m.buka.cn/read/%s/%s", cid, path)
        return Request.Builder()
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .url(url)
            .build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val m = Pattern.compile("<img class=\"lazy\" data-original=\"(http.*?jpg)\" />").matcher(html)
        if (m.find()) {
            try {
                var i = 0
                do {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, ++i, StringUtils.match("http.*jpg", m.group(0), 0) ?: "", false))
                } while (m.find())
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
        return Node(html).text("div.book-detail > div.cont-list > dl:eq(2) > dd")
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        val body = Node(html)
        for (node in body.list("li > a")) {
            val cid = node.hrefWithSplit(1)
            val title = node.text("h3")
            val cover = node.attr("div > img", "data-src")
            val update = node.text("dl:eq(5) > dd")
            val author = node.text("dl:eq(2) > dd")
            list.add(Comic(TYPE, cid, title, cover, update, author))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://m.buka.cn")
    }

    companion object {
        const val TYPE = 52
        const val DEFAULT_TITLE = "布卡漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
