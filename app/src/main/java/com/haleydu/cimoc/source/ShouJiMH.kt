package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import java.util.LinkedList

class ShouJiMH(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        val packed = StringUtils.match("eval\\(.*\\)", html, 0)
        if (packed == null) {
            return super.parseImages(html, chapter)
        }
        try {
            var decrypted = DecryptionUtils.evalDecrypt(packed, "newImgs")
            if (decrypted == null || decrypted.isEmpty() || "undefined" == decrypted) {
                decrypted = DecryptionUtils.evalDecrypt(packed)
            }
            if (decrypted == null || decrypted.isEmpty()) {
                return super.parseImages(html, chapter)
            }
            val array = decrypted.split(",")
            var index = 0
            for (item in array) {
                var url = item.trim()
                if (url.isEmpty()) {
                    continue
                }
                url = absImage(url) ?: continue
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
        const val TYPE = 226
    }
}
