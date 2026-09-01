package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Request
import java.util.LinkedList

class EHentai(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = ""
        if (page == 1)
            url = StringUtils.format("https://e-hentai.org/?f_doujinshi=1&f_manga=1&f_artistcg=1&f_gamecg=1&f_western=1&f_non-h=1&f_imageset=1&f_cosplay=1&f_asianporn=1&f_misc=1&f_search=%s&f_apply=Apply+Filter", keyword)
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("div.ido > div > table.itg > tbody > tr[class^=gtr]")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSubString("td:eq(2) > div > div:eq(2) > a", 23, -2)
                var title = node.text("td:eq(2) > div > div:eq(2) > a")
                var cover = node.src("td:eq(2) > div > div:eq(0) > img")
                if (cover.isEmpty()) {
                    val temp = node.textWithSubstring("td:eq(2) > div > div:eq(0)", 14)?.split("~", limit = 2)?.getOrNull(0) ?: ""
                    cover = "http://ehgt.org/".plus(temp)
                }
                val update = node.textWithSubstring("td:eq(1)", 0, 10)
                val author = StringUtils.match("\\[(.*?)\\]", title, 1)
                title = title.replaceFirst("\\[.*?\\]\\s*".toRegex(), "")
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getInfoRequest(cid: String): Request {
        val url = StringUtils.format("http://g.e-hentai.org/g/%s", cid)
        return Request.Builder().url(url).header("Cookie", "nw=1").build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val update = body.textWithSubstring("#gdd > table > tbody > tr:eq(0) > td:eq(1)", 0, 10)
        val title = body.text("#gn")
        val intro = body.text("#gj")
        val author = body.text("#taglist > table > tbody > tr > td:eq(1) > div > a[id^=ta_artist]")
        val cover = "https://github.com/Haleydu/Cimoc/raw/release-tci/screenshot/icon.png"
        comic.setInfo(title, cover, update, intro, author, true)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val body = Node(html)
        val length = body.textWithSplit("#gdd > table > tbody > tr:eq(5) > td:eq(1)", " ", 0)
        val size = if (Integer.parseInt(length) % 40 == 0) Integer.parseInt(length) / 40 else Integer.parseInt(length) / 40 + 1
        for (i in 0 until size) {
            list.add(Chapter((sourceComic.toString() + "000" + i).toLong(), sourceComic, "Ch$i", i.toString()))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("http://g.e-hentai.org/g/%s/?p=%s", cid, path)
        return Request.Builder().url(url).header("Cookie", "nw=1").build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val body = Node(html)
        var count = 0
        for (node in body.list("#gdt > div > div > a")) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + count).toLong()
            list.add(ImageUrl(id, comicChapter, ++count, node.href().orEmpty(), true))
        }
        return list
    }

    override fun getLazyRequest(url: String): Request {
        return Request.Builder().url(url).build()
    }

    override fun parseLazy(html: String, url: String): String? {
        return Node(html).src("a > img[style]")
    }

    companion object {
        const val TYPE = 60
        const val DEFAULT_TITLE = "EHentai"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
