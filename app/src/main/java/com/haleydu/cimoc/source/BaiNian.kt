package com.haleydu.cimoc.source

import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request

class BaiNian(source: Source) : MangaParser() {

    init {
        init(source, null)
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        val url = if (page == 1) {
            "https://m.bnmanhua.com/index.php/search.html"
        } else {
            ""
        }
        val body = FormBody.Builder()
            .add("keyword", keyword)
            .build()
        return Request.Builder()
            .addHeader("Referer", "https://m.bnmanhua.com/")
            .addHeader("Host", "m.bnmanhua.com")
            .url(url)
            .post(body)
            .build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("ul.tbox_m > li.vbox")) {
            override fun parse(node: Node): Comic {
                val title = node.attr("a.vbox_t", "title")
                val cid = node.attr("a.vbox_t", "href")
                val cover = node.attr("a.vbox_t > mip-img", "src")
                return Comic(TYPE, cid, title, cover, null, null)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "https://m.bnmanhua.com$cid"
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("m.bnmanhua.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        return Request.Builder().url("https://m.bnmanhua.com$cid").build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val cover = body.attr("div.dbox > div.img > mip-img", "src")
        val title = body.text("div.dbox > div.data > h4")
        val intro = body.text("div.tbox_js")
        val author = labeled(body.text("div.dbox > div.data > p.dir"))
        val update = labeledDate(body.text("div.dbox > div.data > p.act"))
        val status = isFinish(body.text("span.list_item"))
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = ArrayList<Chapter>()
        var i = 0
        for (node in Node(html).list("div.tabs_block > ul > li > a")) {
            val title = node.text()
            val path = node.href()
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        return Request.Builder().url("https://m.bnmanhua.com$path").build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = ArrayList<ImageUrl>()
        val host = IMG_HOST.find(html)?.groupValues?.get(1)
        val pathStr = Z_IMG.find(html)?.groupValues?.get(1)
        if (pathStr.isNullOrEmpty()) {
            return list
        }
        try {
            pathStr.split(",").forEachIndexed { i, raw ->
                val path = raw.replace("\"", "").replace("\\", "")
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, i + 1, "$host/$path", false))
            }
        } catch (_: Exception) {
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String {
        return labeledDate(Node(html).text("div.dbox > div.data > p.act"))
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "https://m.bnmanhua.com")
    }

    companion object {
        const val TYPE = 13
        const val DEFAULT_TITLE = "百年漫画"

        private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
        private val IMG_HOST = Regex("""src="(.*?)/upload""")
        private val Z_IMG = Regex("""z_img='\[(.*?)]'""")

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }

        private fun labeled(text: String?): String {
            val value = text.orEmpty()
            return value.substringAfter('：', value.substringAfter(':')).trim()
        }

        private fun labeledDate(text: String?): String {
            val value = text.orEmpty()
            return DATE.find(value)?.value ?: labeled(value).take(10)
        }
    }
}
