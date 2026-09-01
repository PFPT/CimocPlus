package com.haleydu.cimoc.source

import com.google.common.collect.Lists
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
import com.haleydu.cimoc.utils.DecryptionUtils.evalDecrypt
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.LinkedList
import java.util.regex.Pattern

class Tencent(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, null)
    }

    private fun mobileHost(): String {
        return sourceConfigManager.firstUrl("https://m.ac.qq.com", "TENCENTM", "TENCENT")
    }

    private fun webHost(): String {
        return sourceConfigManager.firstUrl("http://ac.qq.com", "TENCENT", "TENCENTM")
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) {
            return null
        }
        val path = StringUtils.format("/search/result?word=%s", keyword)
        return mobileRequest(path)
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".comic-item")) {
            override fun parse(node: Node): Comic {
                var cid = node.attr("a", "href")
                cid = cid.substring("/comic/index/id/".length)
                val title = node.text(".comic-title")
                val cover = node.attr(".cover-image", "src")
                val update = node.text(".comic-update")
                val author = "UNKNOWN"
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return webHost() + "/Comic/ComicInfo/id/".plus(cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("ac.qq.com"))
        filter.add(UrlFilter("m.ac.qq.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        return mobileRequest("/comic/index/id/".plus(cid))
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.head-title-tags > h1")
        val cover = body.src("div.head-banner > img")
        val update = ""
        val author = body.text("li.author-wr")
        val intro = body.text("div.head-info-desc")
        val status = isFinish("连载中")
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun getChapterRequest(html: String, cid: String): Request {
        return mobileRequest("/comic/chapterList/id/".plus(cid))
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var i = 0
        for (node in Node(html).list("ul.normal > li.chapter-item")) {
            val title = node.text("a")
            val href = node.href("a")
            if (href == null || href.isEmpty()) {
                continue
            }
            val path = if (href.contains("/cid/")) href.substring(href.lastIndexOf("/cid/") + 5) else href
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val suffix = StringUtils.format("/chapter/index/id/%s/cid/%s", cid, path)
        return mobileRequest(suffix)
    }

    private fun splice(str: String, from: Int, length: Int): String {
        return str.substring(0, from) + str.substring(from + length, str.length)
    }

    private fun decodeData(str: String, nonceRaw: String): String {
        var nonce = evalDecrypt(nonceRaw)
        val m = Pattern.compile("\\d+[a-zA-Z]+").matcher(nonce)
        val matches: MutableList<String> = ArrayList()
        while (m.find()) {
            matches.add(m.group(0))
        }
        var len = matches.size
        var result = str
        while (len-- != 0) {
            result = splice(
                result,
                StringUtils.match("^\\d+", matches[len], 0)!!.toInt() and 255,
                StringUtils.replaceAll(matches[len], "\\d+", "")!!.length
            )
        }
        return result
    }

    @Throws(JSONException::class)
    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        var str = StringUtils.match("data:\\s*'(.*)?',", html, 1)
        if (str == null) {
            return list
        }
        val nonce = StringUtils.match("<script>window.*?=(.*?)<\\/script>", html, 1)
            ?: throw JSONException("nonce")
        try {
            str = DecryptionUtils.base64Decrypt(decodeData(str, nonce))
        } catch (e: Exception) {
            throw JSONException(e.message ?: "decode")
        }
        val obj = JSONObject(str)
        val array = obj.optJSONArray("picture") ?: throw JSONException("picture")
        val preview = isPaywalled(obj) || (array.length() <= 1 && hasPayFlag(obj))
        var i = 0
        while (i != array.length()) {
            val url = pictureUrl(array.getJSONObject(i))
            if (url == null || url.isEmpty()) {
                ++i
                continue
            }
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            val imageUrl = ImageUrl(id, comicChapter, i + 1, url, false)
            imageUrl.preview = preview
            list.add(imageUrl)
            ++i
        }
        return list
    }

    private fun pictureUrl(item: JSONObject): String? {
        var url = item.optString("url")
        if (url == null || url.isEmpty()) {
            url = item.optString("src")
        }
        if (url == null || url.isEmpty()) {
            url = item.optString("pic")
        }
        return url
    }

    private fun isPaywalled(root: JSONObject): Boolean {
        return isDenied(root)
            || isDenied(root.optJSONObject("chapter"))
            || isDenied(root.optJSONObject("comic"))
    }

    private fun isDenied(obj: JSONObject?): Boolean {
        if (obj == null) {
            return false
        }
        if (obj.has("canRead") && !obj.optBoolean("canRead")) {
            return true
        }
        if (obj.has("buy") && !obj.optBoolean("buy")) {
            return true
        }
        if (obj.optInt("needPay", 0) == 1) {
            return true
        }
        if (obj.optInt("payState", 0) > 1) {
            return true
        }
        if (obj.optInt("payStatus", 0) > 1) {
            return true
        }
        return false
    }

    private fun hasPayFlag(root: JSONObject): Boolean {
        var chapter = root.optJSONObject("chapter")
        if (chapter == null) {
            chapter = root
        }
        return chapter.has("payState")
            || chapter.has("payStatus")
            || chapter.has("needPay")
            || chapter.has("vip")
            || chapter.has("vipState")
            || root.has("payState")
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
        return Headers.headersOf("Referer", mobileHost(), "User-Agent", MOBILE_UA)
    }

    private fun mobileRequest(path: String): Request {
        val request = sourceConfigManager.hostPathRequest(path, "https://m.ac.qq.com", "TENCENTM", "TENCENT")
        return request.newBuilder().header("User-Agent", MOBILE_UA).build()
    }

    companion object {
        const val TYPE = 51
        const val DEFAULT_TITLE = "腾讯动漫"
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
