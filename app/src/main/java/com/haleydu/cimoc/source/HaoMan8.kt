package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.parser.JsonIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class HaoMan8(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    private fun apiHost(): String {
        var server = mConfig.serverUrl
        if (server != null) {
            server = server.trim()
            if (server.startsWith("http")) {
                if (server.endsWith("/")) {
                    server = server.substring(0, server.length - 1)
                }
                return server
            }
        }
        return DEFAULT_API
    }

    @Throws(Exception::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        val path = StringUtils.format(
            "/app/v1/search/index?keyword=%s&source=0&page=%d&size=20",
            encoded, page
        )
        val host = apiHost()
        val builder = requestBuilder(host + path) ?: return null
        if (DEFAULT_API != host) {
            builder.header("X-Cimoc-Fallback", DEFAULT_API + path)
        }
        return builder.build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val array = extractList(html) ?: return super.getSearchIterator(html, page)
        return object : JsonIterator(array) {
            override fun parse(`object`: JSONObject): Comic? {
                val cid = firstString(`object`, "id", "comic_id", "comicId")
                if (cid.isEmpty()) {
                    return null
                }
                val title = firstString(`object`, "title", "name", "comic_name")
                var cover = firstString(`object`, "cover", "coverUrl", "picUrl", "pic")
                if (cover.startsWith("//")) {
                    cover = "https:$cover"
                }
                val author = firstString(`object`, "authors", "author", "author_name")
                val update = firstString(`object`, "last_update_chapter_name", "last_name", "status")
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    companion object {
        const val TYPE = 206
        const val DEFAULT_TITLE = "好漫8"
        private const val DEFAULT_API = "https://v4api.zaimanhua.com"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }

        private fun extractList(html: String?): JSONArray? {
            if (html == null) {
                return null
            }
            val trimmed = html.trim()
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                return null
            }
            try {
                if (trimmed.startsWith("[")) {
                    return JSONArray(trimmed)
                }
                val root = JSONObject(trimmed)
                val direct = firstArray(root, "comicList", "list", "comics", "data")
                if (direct != null) {
                    return direct
                }
                val data = root.optJSONObject("data")
                if (data != null) {
                    return firstArray(data, "comicList", "list", "comics", "result")
                }
            } catch (_: Exception) {
            }
            return null
        }

        private fun firstArray(`object`: JSONObject, vararg keys: String): JSONArray? {
            for (key in keys) {
                val array = `object`.optJSONArray(key)
                if (array != null && array.length() > 0) {
                    return array
                }
            }
            return null
        }

        private fun firstString(`object`: JSONObject, vararg keys: String): String {
            for (key in keys) {
                val value = `object`.optString(key, "")
                if (!value.isNullOrEmpty() && value != "null") {
                    return value
                }
            }
            return ""
        }
    }
}
