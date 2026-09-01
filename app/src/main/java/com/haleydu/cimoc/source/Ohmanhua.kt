package com.haleydu.cimoc.source

import android.net.Uri
import android.util.Base64
import android.util.Pair
import com.haleydu.cimoc.data.SourceConfigManager
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.util.ArrayList
import java.util.LinkedList

class Ohmanhua(source: Source?, private val sourceConfigManager: SourceConfigManager) : MangaParser() {

    init {
        init(source, Category())
    }

    private fun baseUrl(): String {
        return sourceConfigManager.firstUrl(DEFAULT_HOST, "CoCoManHua", "OHMANHUA")
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) {
            return null
        }
        val path = StringUtils.format("/search?searchString=%s", keyword)
        return sourceConfigManager.hostPathRequest(path, DEFAULT_HOST, "CoCoManHua", "OHMANHUA")
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("dl.fed-deta-info")) {
            override fun parse(node: Node): Comic {
                val cid = node.href("dd > h1 > a")
                val title = node.text("dd > h1 > a")
                val cover = node.attr("dt > a", "data-original")
                var author = node.text("dd > ul > li:eq(3)")
                var update = node.text("dd > ul > li:eq(4)")
                if (!author.contains("作者")) {
                    author = update
                }
                if (!update.contains("更新")) {
                    update = node.text("dd > ul > li:eq(5)")
                }
                return Comic(
                    TYPE, cid, title, cover,
                    update.replace("更新", ""),
                    author.replace("作者", "")
                )
            }
        }
    }

    override fun getUrl(cid: String): String {
        return baseUrl() + cid
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.yoyomanga.com"))
        filter.add(UrlFilter("www.colamanga.com"))
        filter.add(UrlFilter("www.cocomanhua.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = baseUrl() + cid
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("dl.fed-deta-info > dd > h1")
        val cover = body.attr("dl.fed-deta-info > dt > a", "data-original")
        val intro = body.text("div.fed-tabs-boxs > div > p")
        var statusStr = body.text("dl.fed-deta-info > dd > ul > li:eq(0)")
        var author = body.text("dl.fed-deta-info > dd > ul > li:eq(1)")
        var update = body.text("dl.fed-deta-info > dd > ul > li:eq(2)")
        if (!statusStr.contains("状态")) {
            statusStr = author
        }
        if (!author.contains("作者")) {
            author = update
        }
        if (!update.contains("更新")) {
            update = body.text("dl.fed-deta-info > dd > ul > li:eq(3) > a")
        }
        val status = isFinish(statusStr.replace("状态", ""))
        comic.setInfo(title, cover, update.replace("更新", ""), intro, author.replace("作者", ""), status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var i = 0
        for (node in Node(html).list("div:not(.fed-hidden) > div.all_data_list > ul.fed-part-rows a")) {
            val title = node.attr("title")
            val path = node.href("a")
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = baseUrl() + path
        return Request.Builder().url(url).build()
    }

    var decryptKey1Arr = arrayOf("fw122587mkertyui", "fw12558899ertyui")
    var decryptKey2Arr = arrayOf("fw125gjdi9ertyui")

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val encodedData = StringUtils.match("C_DATA=\\'(.+?)\\'", html, 1)
        if (encodedData != null) {
            try {
                val decryptedData = decodeAndDecrypt("encodedData", encodedData, decryptKey1Arr)
                val imgType = StringUtils.match("img_type:\"(.+?)\"", decryptedData, 1)
                if (imgType == null || imgType.isEmpty()) {
                    val external = processPagesFromExternal(decryptedData, chapter)
                    if (external.isNotEmpty()) {
                        return external
                    }
                    return processPagesFromInternal(decryptedData, chapter)
                } else {
                    val internal = processPagesFromInternal(decryptedData, chapter)
                    if (internal.isNotEmpty()) {
                        return internal
                    }
                    return processPagesFromExternal(decryptedData, chapter)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return LinkedList()
    }

    private fun decodeAndDecrypt(decodeName: String, value: String, keyArr: Array<String>): String {
        val decodedValue = String(Base64.decode(value, Base64.NO_WRAP))
        for (i in keyArr.indices) {
            try {
                return DecryptionUtils.decryptAES(decodedValue, keyArr[i])
            } catch (e: Exception) {
            }
        }
        return ""
    }

    private fun processPagesFromExternal(decryptedData: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val encodedUrlsDirect = StringUtils.match("urls__direct:\"(.+?)\"", decryptedData, 1)
        if (encodedUrlsDirect == null || encodedUrlsDirect.isEmpty()) {
            return list
        }
        val decodedUrlsDirect = String(Base64.decode(encodedUrlsDirect, Base64.NO_WRAP))
        val pageUrlArr = decodedUrlsDirect.split(Regex("\\|SEPARATER\\|"))
        var index = 0
        for (rawPageUrl in pageUrlArr) {
            var pageUrl = rawPageUrl ?: continue
            pageUrl = pageUrl.trim()
            if (pageUrl.isEmpty()) {
                continue
            }
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + index).toLong()
            list.add(ImageUrl(id, comicChapter, ++index, pageUrl, false))
        }
        return list
    }

    private fun processPagesFromInternal(decryptedData: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        val imageServerDomain = StringUtils.match("domain:\"(.+?)\"", decryptedData, 1)
        val startImgRaw = StringUtils.match("startimg:([0-9]+?),", decryptedData, 1)
        val encodedRelativePath = StringUtils.match("enc_code2:\"(.+?)\"", decryptedData, 1)
        val encodedTotalPages = StringUtils.match("enc_code1:\"(.+?)\"", decryptedData, 1)
        if (imageServerDomain == null || imageServerDomain.isEmpty()
            || encodedRelativePath == null || encodedTotalPages == null
        ) {
            return list
        }
        val decryptedRelativePath = decodeAndDecrypt("encodedRelativePath", encodedRelativePath, decryptKey2Arr)
        val decryptedTotalPagesRaw = decodeAndDecrypt("encodedTotalPages", encodedTotalPages, decryptKey1Arr)
        if (decryptedRelativePath == null || decryptedRelativePath.isEmpty()
            || decryptedTotalPagesRaw == null || decryptedTotalPagesRaw.isEmpty()
        ) {
            return list
        }
        var startImg = 1
        if (startImgRaw != null && startImgRaw.isNotEmpty()) {
            startImg = startImgRaw.toInt()
        }
        val decryptedTotalPages = decryptedTotalPagesRaw.toInt()
        var i = startImg
        while (i <= decryptedTotalPages) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            val jpg = StringUtils.format("%04d.jpg", i)
            val url = "https://" + imageServerDomain + "/comic/" + encodeUri(decryptedRelativePath) + jpg
            list.add(ImageUrl(id, comicChapter, i, url, false))
            ++i
        }
        return list
    }

    private fun encodeUri(str: String): String {
        val whitelistChar = "@#&=*+-_.,:!?()/~'%"
        return Uri.encode(str, whitelistChar)
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        val body = Node(html)
        var update = body.text("dl.fed-deta-info > dd > ul > li:eq(2)")
        if (!update.contains("更新")) {
            update = body.text("dl.fed-deta-info > dd > ul > li:eq(3) > a")
        }
        return update.replace("更新", "")
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", baseUrl())
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list: MutableList<Comic> = LinkedList()
        val body = Node(html)
        var nodes = body.list("ul.fed-list-info > li, .fed-list-info > li")
        if (nodes.isEmpty()) {
            nodes = body.list("dl.fed-deta-info")
        }
        for (node in nodes) {
            var cid = node.href("a.fed-list-pics")
            if (cid == null || cid.isEmpty()) {
                cid = node.href("dt > a")
            }
            if (cid == null || cid.isEmpty()) {
                cid = node.href("a")
            }
            if (cid == null || cid.isEmpty()) {
                continue
            }
            var title = node.text("a.fed-list-title")
            if (title == null || title.isEmpty()) {
                title = node.text("dd > h1 > a")
            }
            if (title == null || title.isEmpty()) {
                title = node.text("a")
            }
            var cover = node.attr("a.fed-list-pics", "data-original")
            if (cover == null || cover.isEmpty()) {
                cover = node.attr("dt > a", "data-original")
            }
            val update = node.text(".fed-list-remarks")
            list.add(Comic(TYPE, cid, title, cover, update, null))
        }
        return list
    }

    private inner class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var id = if (args.size > CATEGORY_SUBJECT) args[CATEGORY_SUBJECT] else "1"
            if (id.isEmpty()) {
                id = "1"
            }
            return StringUtils.format(baseUrl() + "/show?id=%s&page=%%d", id)
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("全部", "1"))
            list.add(Pair.create("热血", "2"))
            list.add(Pair.create("恋爱", "3"))
            list.add(Pair.create("校园", "4"))
            list.add(Pair.create("冒险", "5"))
            list.add(Pair.create("科幻", "6"))
            list.add(Pair.create("生活", "7"))
            list.add(Pair.create("搞笑", "8"))
            return list
        }
    }

    companion object {
        const val TYPE = 71
        const val DEFAULT_TITLE = "oh漫画"
        private const val DEFAULT_HOST = "https://www.yoyomanga.com"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
