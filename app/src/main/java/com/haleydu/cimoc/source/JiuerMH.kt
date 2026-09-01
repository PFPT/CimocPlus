package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.utils.StringUtils
import org.json.JSONArray
import java.util.LinkedList

class JiuerMH(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        val raw = StringUtils.match("chapterImages\\s*=\\s*(\\[[\\s\\S]*?\\])", html, 1)
        if (raw.isNullOrEmpty()) {
            return super.parseImages(html, chapter)
        }
        try {
            val array = JSONArray(raw)
            var server = StringUtils.match("getCih\\(\\)\\s*\\{\\s*return\\s*\"([^\"]+)\"", html, 1)
            if (server.isNullOrEmpty()) {
                server = mConfig.imageServerUrl
            }
            if (server == null) {
                server = ""
            }
            if (server.endsWith("/")) {
                server = server.substring(0, server.length - 1)
            }
            var chapterPath = StringUtils.match("chapterPath\\s*=\\s*\"([^\"]*)\"", html, 1)
            if (chapterPath == null) {
                chapterPath = ""
            }
            for (i in 0 until array.length()) {
                var path = array.optString(i).trim()
                if (path.isEmpty()) {
                    continue
                }
                val url: String
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    url = path
                } else {
                    if (!path.startsWith("/")) {
                        var prefix = chapterPath
                        if (prefix.isNotEmpty() && !prefix.endsWith("/")) {
                            prefix = "$prefix/"
                        }
                        path = prefix + path
                        if (!path.startsWith("/")) {
                            path = "/$path"
                        }
                    }
                    url = server + path
                }
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, i + 1, url, false))
            }
        } catch (_: Exception) {
        }
        if (list.isEmpty()) {
            return super.parseImages(html, chapter)
        }
        return list
    }

    companion object {
        const val TYPE = 233
    }
}
