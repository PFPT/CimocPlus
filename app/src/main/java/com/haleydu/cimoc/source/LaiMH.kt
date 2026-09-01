package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.utils.StringUtils
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.LinkedList

class LaiMH(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        val json = StringUtils.match("var mhInfo=(\\{.*?\\});", html, 1)
            ?: return super.parseImages(html, chapter)
        try {
            val obj = JSONObject(json)
            val images = obj.optJSONArray("images")
            if (images == null || images.length() == 0) {
                return list
            }
            var path = encodePath(obj.optString("path", ""))
            var host = obj.optString("host", "").trim()
            if (host.isEmpty()) {
                host = mConfig.imageServerUrl
            }
            if (host.isNullOrEmpty()) {
                host = IMAGE_HOSTS[0]
            }
            if (!host.startsWith("http")) {
                host = "https://$host"
            }
            host = if (host.endsWith("/")) host.substring(0, host.length - 1) else host
            if (path.isNotEmpty() && !path.startsWith("/")) {
                path = "/$path"
            }
            if (!path.endsWith("/") && path.isNotEmpty()) {
                path = "$path/"
            }
            var index = 0
            for (i in 0 until images.length()) {
                val name = images.optString(i, "").trim()
                if (name.isEmpty()) {
                    continue
                }
                val url = if (name.startsWith("http")) name else host + path + name
                val id = (comicChapter.toString() + "000" + index).toLong()
                list.add(ImageUrl(id, comicChapter, ++index, url, false))
            }
        } catch (_: Exception) {
        }
        return list
    }

    companion object {
        const val TYPE = 225
        private val IMAGE_HOSTS = arrayOf(
            "https://mhpic5eer.tgmhfc.uk",
            "https://mhpic7899-5.tgmhfc.uk",
            "https://mhpic7ffr.tgmhfc.uk",
            "https://mhpicwwt.tgmhfc.uk",
            "https://mhpicwwx.tgmhfc.uk"
        )

        private fun encodePath(path: String?): String {
            if (path.isNullOrEmpty()) {
                return ""
            }
            val parts = path.split("/", limit = -1).toTypedArray()
            val builder = StringBuilder()
            for (i in parts.indices) {
                if (i > 0) {
                    builder.append('/')
                }
                if (parts[i].isEmpty()) {
                    continue
                }
                try {
                    builder.append(URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"))
                } catch (_: Exception) {
                    builder.append(parts[i])
                }
            }
            return builder.toString()
        }
    }
}
