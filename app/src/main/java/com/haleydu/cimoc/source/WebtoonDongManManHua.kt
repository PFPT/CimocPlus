package com.haleydu.cimoc.source

import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.core.Manga.getResponseBody
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.LinkedList

class WebtoonDongManManHua(source: Source?, private val httpClient: OkHttpClient) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page == 1) {
            val url = "$baseUrl/search/?keyword=$keyword"
            return Request.Builder().url(url).addHeader("Referer", "www.dongmanmanhua.cn").build()
        }
        return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".card_wrap.search > .card_lst > li > a")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit(-1)
                val title = node.text("div.info > p.subj")
                val cover = node.src("img")
                val author = node.text("div.info > p.author")
                return Comic(TYPE, cid, title, cover, null, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return baseUrl + "/episodeList?titleNo=".plus(cid)
    }

    override fun getInfoRequest(cid: String): Request {
        val url = baseUrl + "/episodeList?titleNo=".plus(cid)
        return Request.Builder().url(url).addHeader("Referer", "www.dongmanmanhua.cn").build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        k = 0
        val body = Node(html)
        val title = body.text(".detail_header > .info > h1.subj")
        val cover = body.src("ul#_listUl > li:eq(0) > a > span.thmb > img")
        val update = body.text("ul#_listUl > li:eq(0) > a > span.date").trim()
        val author = body.text(".detail_header > div.info > span.author")
        val intro = body.text("#_asideDetail > p.summary")
        val status = isFinish(body.text("#_asideDetail > p.day_info"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    private var k = 0

    fun parseChapter(body: Node, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        for (node in body.list("ul#_listUl > li > a")) {
            val title = node.text("span.subj > span") + " " + node.text("span.tx")
            val path = "https:" + node.href()
            list.add(Chapter((sourceComic.toString() + "000" + k++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        val body = Node(html)
        for (nodePage in body.list("div.detail_lst > div.paginate > a")) {
            val urlPage = nodePage.href()
            val urlPageTag = nodePage.attr("a", "class")
            if (urlPage == "#" && (urlPageTag == null || urlPageTag == "")) {
                list.addAll(parseChapter(body, sourceComic))
            } else if (urlPageTag == null || urlPageTag == "") {
                try {
                    val pageTagUrl = baseUrl + urlPage
                    val request = Request.Builder()
                        .url(pageTagUrl)
                        .addHeader("Referer", "www.dongmanmanhua.cn")
                        .build()
                    val htmlPage = getResponseBody(httpClient, request)
                    list.addAll(parseChapter(Node(htmlPage), sourceComic))
                } catch (e: Manga.NetworkErrorException) {
                    e.printStackTrace()
                }
            } else if (urlPageTag == "pg_next") {
                try {
                    val pageTagUrl = baseUrl + urlPage
                    val request = Request.Builder()
                        .url(pageTagUrl)
                        .addHeader("Referer", "www.dongmanmanhua.cn")
                        .build()
                    val htmlPageNext = getResponseBody(httpClient, request)
                    list.addAll(parseChapter(htmlPageNext, comic, sourceComic))
                } catch (e: Manga.NetworkErrorException) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        return Request.Builder().url(path).addHeader("Referer", "www.dongmanmanhua.cn").build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val body = Node(html)
        var i = 1
        for (node in body.list("div#_imageList > img")) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            val url = node.attr("img", "data-url")
            list.add(ImageUrl(id, comicChapter, i++, url, false))
        }
        if (list.isNotEmpty()) return list

        val docUrl = StringUtils.match("documentURL:.*?'(.*?)'", html, 0)
        val motiontoonPath = StringUtils.match("jpg:.*?'(.*?)\\{", html, 0)
        try {
            if (docUrl == null) return list
            val html1 = getResponseBody(
                httpClient,
                Request.Builder().url(docUrl)
                    .addHeader("Referer", "www.dongmanmanhua.cn")
                    .build()
            )
            val motiontoonJson = JSONObject(html1).getJSONObject("assets").getJSONObject("image")
            val jsonIterator = motiontoonJson.keys()
            i = 1
            while (jsonIterator.hasNext()) {
                val key = jsonIterator.next()
                if (key.contains("layer")) {
                    val comicChapter = chapter.id
                    val id = (comicChapter.toString() + "000" + i).toLong()
                    list.add(ImageUrl(id, comicChapter, i++, motiontoonPath + motiontoonJson.getString(key), false))
                }
            }
        } catch (e: Manga.NetworkErrorException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return Node(html).text("ul#_listUl > li:eq(0) > a > span.date").trim()
    }

    override fun getCategoryRequest(format: String, page: Int): Request {
        if (page == 1) {
            return Request.Builder().url(format).addHeader("Referer", "https://m.webtoons.com").build()
        }
        return super.getCategoryRequest(format, page)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list: MutableList<Comic> = ArrayList()
        val body = Node(html)
        for (node in body.list("#ct > ul > li > a")) {
            val cid = node.hrefWithSplit(-1)
            val title = node.text("div.info > p.subj > span")
            val cover = node.attrWithSplit("div.pic", "style", "\\(|\\)", 1)
            list.add(Comic(TYPE, cid, title, cover, null, null))
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", baseUrl)
    }

    companion object {
        const val TYPE = 11
        const val DEFAULT_TITLE = "咚漫漫画"
        const val baseUrl = "https://www.dongmanmanhua.cn"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
