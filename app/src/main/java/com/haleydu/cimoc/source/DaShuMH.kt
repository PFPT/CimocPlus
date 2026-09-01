package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.utils.DecryptionUtils
import java.util.LinkedList
import java.util.regex.Pattern

class DaShuMH(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        val packedMatcher = PACKED.matcher(html)
        if (!packedMatcher.find()) {
            return super.parseImages(html, chapter)
        }
        try {
            val result = DecryptionUtils.evalDecrypt(packedMatcher.group() ?: "")
            if (result.isNullOrEmpty()) {
                return super.parseImages(html, chapter)
            }
            val urlMatcher = IMAGE_URL.matcher(result)
            var index = 0
            while (urlMatcher.find()) {
                var url = urlMatcher.group() ?: continue
                if (url.contains("banquan") || url.contains("icon_loading") || url.contains("grey")) {
                    continue
                }
                url = absImage(url) ?: continue
                if (url.isEmpty()) {
                    continue
                }
                val id = (comicChapter.toString() + "000" + index).toLong()
                list.add(ImageUrl(id, comicChapter, ++index, url, false))
            }
        } catch (_: Exception) {
        }
        if (list.isEmpty()) {
            return super.parseImages(html, chapter)
        }
        return list
    }

    companion object {
        const val TYPE = 228
        private val PACKED = Pattern.compile(
            "\\(function\\(p,a,c,k,e,d\\).*?0,\\{\\}\\)\\)"
        )
        private val IMAGE_URL = Pattern.compile("https?://[^\"'\\s]+")
    }
}
