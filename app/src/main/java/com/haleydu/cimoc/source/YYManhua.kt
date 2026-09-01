package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceConfig
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import org.json.JSONObject
import java.util.Collections
import java.util.LinkedList

class YYManhua(source: Source?, config: SourceConfig) : GenericHtmlParser(source, config) {

    private fun imageHost(): String {
        if (mConfig.imageServerUrl != null && mConfig.imageServerUrl.isNotEmpty()) {
            return mConfig.imageServerUrl
        }
        return "https://i.hamreus.com"
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var body = Node(html)
        val baseText = body.id("__VIEWSTATE").attr("value")
        if (!StringUtils.isEmpty(baseText)) {
            body = Node(DecryptionUtils.LZ64Decrypt(baseText) ?: "")
        }
        var i = 0
        for (node in body.list("div.chapter-list")) {
            val uls = node.list("ul")
            Collections.reverse(uls)
            for (ul in uls) {
                for (li in ul.list("li > a")) {
                    var title = li.attr("title")
                    if (title == null || title.isEmpty()) {
                        title = li.text()
                    }
                    val href = li.href()
                    var path = extractPath(href)
                    if (path == null || path.isEmpty()) {
                        path = li.hrefWithSplit(2)
                    }
                    if (path == null || path.isEmpty()) {
                        continue
                    }
                    try {
                        list.add(Chapter((sourceComic.toString() + "000" + i).toLong(), sourceComic, title, path))
                        i++
                    } catch (_: Exception) {
                    }
                }
            }
        }
        if (list.isEmpty()) {
            return super.parseChapter(html, comic, sourceComic)
        }
        return list
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val comicChapter = chapter?.id
        if (html == null || comicChapter == null) {
            return list
        }
        var packed = StringUtils.match("\\(function\\(p,a,c,k,e,d\\).*?0,\\{\\}\\)\\)", html, 0)
        if (packed == null) {
            return super.parseImages(html, chapter)
        }
        try {
            val replaceable = StringUtils.split(packed, ",", -3)!!
            val fake = StringUtils.split(replaceable, "'", 1)
            val real = DecryptionUtils.LZ64Decrypt(fake)
            packed = packed.replace(replaceable, StringUtils.format("'%s'.split('|')", real))
            val result = DecryptionUtils.evalDecrypt(packed)
            val jsonString = result.substring(12, result.length - 12)
            val obj = JSONObject(jsonString)
            val path = obj.getString("path")
            val e = obj.getJSONObject("sl").getString("e")
            val m = obj.getJSONObject("sl").getString("m")
            val array = obj.getJSONArray("files")
            for (i in 0 until array.length()) {
                val id = (comicChapter.toString() + "000" + i).toLong()
                val url = StringUtils.format("%s%s%s?e=%s&m=%s", imageHost(), path, array.getString(i), e, m)
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
        const val TYPE = 224
    }
}
