package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList

class McCmsParser(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val decrypted = decryptParams(html, chapter)
        if (decrypted.isNotEmpty()) {
            return decrypted
        }
        return super.parseImages(html, chapter)
    }

    private fun decryptParams(html: String?, chapter: Chapter?): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        var packed = StringUtils.match("params\\s*=\\s*'([^']+)'", html, 1)
        if (packed == null) {
            packed = StringUtils.match("params\\s*=\\s*\"([^\"]+)\"", html, 1)
        }
        if (packed == null) {
            return list
        }
        var obj: JSONObject? = null
        for (key in KEYS) {
            try {
                var json = DecryptionUtils.aesCbcDecryptEmbeddedIv(packed, key) ?: continue
                json = json.trim()
                if (!json.startsWith("{")) {
                    continue
                }
                obj = JSONObject(json)
                break
            } catch (_: Exception) {
            }
        }
        if (obj == null) {
            return list
        }
        val prefix = imagePrefix(obj)
        var array = obj.optJSONArray("chapter_images")
        if (array == null || array.length() == 0) {
            array = obj.optJSONArray("images")
        }
        if (array == null) {
            return list
        }
        var index = 0
        for (i in 0 until array.length()) {
            var url = array.optString(i, "")
            if (url.isEmpty()) {
                val item = array.optJSONObject(i)
                if (item != null) {
                    url = firstString(item, "url", "src", "image", "path")
                }
            }
            url = absChapterImage(url, prefix)
            if (url == null) {
                continue
            }
            val id = (comicChapter.toString() + "000" + index).toLong()
            list.add(ImageUrl(id, comicChapter, ++index, url, false))
        }
        return list
    }

    private fun imagePrefix(obj: JSONObject): String {
        val hosts = obj.optJSONArray("images_hosts")
        if (hosts != null && hosts.length() > 0) {
            val host = hosts.optString(0, "").trim()
            if (host.isNotEmpty()) {
                return trimSlash(host)
            }
        }
        val cdn = obj.optString("cdnurl", "").trim()
        if (cdn.isNotEmpty()) {
            return trimSlash(cdn)
        }
        if (!mConfig.imageServerUrl.isNullOrEmpty()) {
            return trimSlash(mConfig.imageServerUrl)
        }
        return ""
    }

    private fun absChapterImage(url: String?, prefix: String): String? {
        var u = url ?: return null
        u = u.trim()
        if (u.isEmpty()) {
            return null
        }
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u
        }
        if (u.startsWith("//")) {
            return "https:$u"
        }
        if (prefix.isNotEmpty()) {
            if (!u.startsWith("/")) {
                u = "/$u"
            }
            return prefix + u
        }
        return absImage(u)
    }

    companion object {
        const val TYPE_DUMANWUORG = 220
        const val TYPE_TTKANMANHUA = 221
        const val TYPE_HAODUOMAN = 222
        const val TYPE_MANHUAYU = 223
        const val TYPE_YOUMANHUAWU = 230

        private val KEYS = arrayOf(
            "5V&RoR%Jf@pJPydF",
            "9S8$vJnU2ANeSRoF"
        )

        private fun firstString(obj: JSONObject, vararg keys: String): String {
            for (key in keys) {
                val value = obj.optString(key, "")
                if (!value.isNullOrEmpty() && value != "null") {
                    return value
                }
            }
            return ""
        }

        private fun trimSlash(url: String): String {
            if (url.endsWith("/")) {
                return url.substring(0, url.length - 1)
            }
            return url
        }
    }
}
