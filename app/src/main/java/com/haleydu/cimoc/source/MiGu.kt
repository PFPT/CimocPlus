package com.haleydu.cimoc.source

import android.util.Base64
import com.google.common.collect.Lists
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.RegexIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

class MiGu(source: Source?) : MangaParser() {

    private var _cid: String? = null
    private var _path: String? = null

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        val keywordByte = keyword.toByteArray(Charset.forName("UTF-8"))
        var keyPre = ""
        for (k in keywordByte) {
            keyPre += "%" + String.format("%02x", k)
        }
        val keywordconv = Base64.encodeToString(keyPre.uppercase().toByteArray(), keyPre.length).trim()
        val url = StringUtils.format(
            "http://www.migudm.cn/search/result/list.html?hintKey=%s&hintType=2&pageSize=30&pageNo=%d",
            keywordconv,
            page
        )
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val m = Pattern.compile("href=\"\\/comic\\/(.*?).html\"  title=\"(.*?)\">\\s+<img src=\"(.*?)\".*?>").matcher(html)
        return object : RegexIterator(m) {
            override fun parse(match: Matcher): Comic {
                return Comic(TYPE, match.group(1), match.group(2), match.group(3), "", "")
            }
        }
    }

    override fun getUrl(cid: String): String {
        return StringUtils.format("http://www.migudm.cn/comic/%s.html", cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.migudm.cn"))
        filter.add(UrlFilter("m.migudm.cn"))
    }

    override fun getInfoRequest(cid: String): Request {
        return Request.Builder().url(StringUtils.format("http://www.migudm.cn/comic/%s.html", cid)).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.inner > .ctdbRight > .ctdbRightInner > .title").trim()
        val cover = body.attr("div.inner > .ctdbLeft > a > img", "src")
        val intro = body.text("#worksDesc").trim()
        val status = false
        comic.setInfo(title, cover, "", intro, "", status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        val m = Pattern.compile("<a stat='.*?' href=\"(?:.*?)(\\d+)\\.html\" class=\"item ellipsis\" title=\"(.*?)\" data-opusname=\"(?:.*?)\" data-index=\"(?:.*?)\" data-url=\"(?:.*?)\" target=\"_blank\">").matcher(html)
        var i = 0
        while (m.find()) {
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, m.group(2), m.group(1)))
        }
        return Lists.reverse(list)
    }

    fun httpGet(url: String): String {
        val okHttpClient = OkHttpClient.Builder().readTimeout(10, TimeUnit.SECONDS).build()
        val request = Request.Builder()
            .url(url)
            .addHeader("user-agent", "Mozilla/5.0 (Linux; U; Android 4.0.4; en-gb; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30")
            .get()
            .build()
        val call: Call = okHttpClient.newCall(request)
        return try {
            val response = call.execute()
            response.body!!.string()
        } catch (ex: Exception) {
            ""
        }
    }

    override fun getImagesRequest(cid: String, path: String): Request? {
        _cid = cid
        _path = path
        var url = StringUtils.format("http://www.migudm.cn/%s/chapter/%s.html", cid, path)
        val html = httpGet(url)
        val m = Pattern.compile("<input type=\"hidden\" id=\"playUrl\" value=\"(.*)\">").matcher(html)
        if (m.find()) {
            url = "http://www.migudm.cn/opus/webQueryWatchOpusInfo.html?".plus(m.group(1))
            return Request.Builder().url(url).build()
        }
        return null
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = ArrayList()
        try {
            val json = JSONObject(html)
            val jpgJsonArr = json.getJSONObject("data").getJSONArray("jpgList")
            for (i in 0 until jpgJsonArr.length()) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                val j = jpgJsonArr.getJSONObject(i)
                list.add(ImageUrl(id, comicChapter, i + 1, j.getString("url"), false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun getHeader(url: String): Headers {
        return Headers.headersOf("Referer", url)
    }

    companion object {
        const val TYPE = 58
        const val DEFAULT_TITLE = "咪咕漫画"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
