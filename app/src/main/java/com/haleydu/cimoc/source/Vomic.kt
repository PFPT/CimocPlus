package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList
import java.util.regex.Pattern

class Vomic(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    private var mKeyword = ""

    @Throws(Exception::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        mKeyword = if (keyword == null) "" else keyword.trim()
        if (page != 1) {
            return null
        }
        return buildGet(mConfig.baseUrl + "/")
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        val nodes: List<Node>
        try {
            nodes = body.list("a[href*=/detail/]")
        } catch (e: Exception) {
            return super.getSearchIterator(html, page)
        }
        val keyword = mKeyword
        return object : NodeIterator(nodes) {
            override fun parse(node: Node): Comic? {
                val cid = extractPath(node.href())
                if (cid == null || cid.isEmpty()) {
                    return null
                }
                var title = node.text("div.title")
                if (title == null || title.isEmpty()) {
                    title = node.text()
                }
                if (keyword.isNotEmpty() && (title == null || !title.contains(keyword))) {
                    return null
                }
                var cover = node.src("img")
                if (cover != null && cover.startsWith("//")) {
                    cover = "https:$cover"
                }
                return Comic(TYPE, cid, title, cover, null, null)
            }
        }
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        var title = meta(html, "og:title")
        if (title != null) {
            val dash = title.indexOf(" - ")
            if (dash > 0) {
                title = title.substring(0, dash).trim()
            }
        }
        var cover = meta(html, "og:image")
        if (cover != null) {
            cover = cover.replace("&amp;", "&")
        }
        var intro = StringUtils.match("简介：</span><span[^>]*>(.*?)</span>", html, 1)
        if (intro != null) {
            intro = intro.replace("<!-- -->", "").trim()
        }
        val finish = isFinish(html)
        comic.setInfo(
            empty(title, comic.title),
            empty(cover, comic.cover),
            null, intro, null, finish
        )
        return comic
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        val array = findImgList(html)
        if (array == null) {
            return super.parseImages(html, chapter)
        }
        var index = 0
        for (i in 0 until array.length()) {
            var url: String
            val item = array.optJSONObject(i)
            if (item != null) {
                url = item.optString("url", "")
                if (url.isEmpty()) {
                    url = item.optString("src", "")
                }
            } else {
                url = array.optString(i, "")
            }
            url = absImage(url) ?: continue
            val id = (comicChapter.toString() + "000" + index).toLong()
            list.add(ImageUrl(id, comicChapter, ++index, url, false))
        }
        if (list.isEmpty()) {
            return super.parseImages(html, chapter)
        }
        return list
    }

    private fun findImgList(html: String): JSONArray? {
        val matcher = Pattern.compile("\"img_list\"\\s*:\\s*(\\[.*?\\])").matcher(html)
        while (matcher.find()) {
            try {
                val array = JSONArray(matcher.group(1))
                if (array.length() > 0) {
                    return array
                }
            } catch (_: Exception) {
            }
        }
        val json = StringUtils.match("\"chapterData\"\\s*:\\s*(\\{.*?\\})", html, 1)
        if (json != null) {
            try {
                return JSONObject(json).optJSONArray("img_list")
            } catch (_: Exception) {
            }
        }
        return null
    }

    companion object {
        const val TYPE = 227

        private fun meta(html: String, property: String): String? {
            var value = StringUtils.match("property=\"" + property + "\" content=\"([^\"]+)\"", html, 1)
            if (value == null) {
                value = StringUtils.match("content=\"([^\"]+)\" property=\"" + property + "\"", html, 1)
            }
            return value
        }

        private fun empty(value: String?, fallback: String?): String? {
            return if (value == null || value.isEmpty()) fallback else value
        }
    }
}
