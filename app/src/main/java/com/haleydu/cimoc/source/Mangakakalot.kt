package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.util.LinkedHashSet
import java.util.LinkedList

class Mangakakalot(source: Source?) : MangaParser() {

    init {
        init(source, Category())
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) return null
        val url = "https://mangakakalot.com/search/story/$keyword"
        return Request.Builder().url(url).addHeader("Referer", "https://mangakakalot.com/").build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".story_item")) {
            override fun parse(node: Node): Comic {
                val cid = node.href("a")
                val title = node.attr("img", "alt")
                val cover = node.src("img")
                val update = node.text("div.story_item_right > span:eq(4)").orEmpty().replace("Updated :", "").trim()
                val author = node.text("div.story_item_right > span:eq(3)").orEmpty().replace("Author(s) :", "").trim()
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        cidUrl = cid
        return cid
    }

    override fun getInfoRequest(cid: String): Request {
        return Request.Builder().url(cid).addHeader("Referer", "https://mangakakalot.com/").build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        if (cidUrl.contains("mangakakalot")) {
            val title = body.attr(".manga-info-pic > img", "alt")
            val cover = body.src(".manga-info-pic > img")
            val update = body.text(".manga-info-text > :eq(3)").orEmpty().replace("Last updated :", "").trim()
            val author = body.text(".manga-info-text > :eq(3)").orEmpty().replace("Author(s) :", "").trim()
            val intro = body.text("#noidungm")
            val status = isFinish(body.text(".manga-info-text > :eq(2)"))
            comic.setInfo(title, cover, update, intro, author, status)
        } else if (cidUrl.contains("manganelo")) {
            val title = body.attr(".info-image > img", "alt")
            val cover = body.src(".info-image > img")
            val update = body.text("div.story-info-right-extent > p:eq(0) > span.stre-value")
            val author = body.text("table.variations-tableInfo > tbody > tr:eq(1) > td.table-value > a")
            val intro = body.text("#panel-story-info-description").orEmpty().replace("Description :", "")
            val status = isFinish(body.text("table.variations-tableInfo > tbody > tr:eq(2) > td.table-value > a"))
            comic.setInfo(title, cover, update, intro, author, status)
        }
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val set = LinkedHashSet<Chapter>()
        val body = Node(html)
        if (cidUrl.contains("mangakakalot")) {
            var i = 0
            for (node in body.list(".chapter-list > div")) {
                val title = node.text("span > a")
                val path = node.href("span > a")
                set.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
            }
        } else if (cidUrl.contains("manganelo")) {
            var i = 0
            for (node in body.list(".row-content-chapter > li")) {
                val title = node.text("a")
                val path = node.href("a")
                set.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
            }
        }
        return LinkedList(set)
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        return Request.Builder().url(path).addHeader("Referer", "https://mangakakalot.com/").build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list = LinkedList<ImageUrl>()
        val body = Node(html)
        var i = 0
        if (cidUrl.contains("mangakakalot")) {
            for (node in body.list("div#vungdoc > img")) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, ++i, node.src().orEmpty(), false))
            }
        } else if (cidUrl.contains("manganelo")) {
            for (node in body.list("div.container-chapter-reader > img")) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                list.add(ImageUrl(id, comicChapter, ++i, node.src().orEmpty(), false))
            }
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "https://mangakakalot.com/")
    }

    override fun getCategoryRequest(format: String, page: Int): Request {
        return super.getCategoryRequest(format, page)
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list = LinkedList<Comic>()
        val body = Node(html)
        val total = Integer.parseInt(
            StringUtils.match(
                "\\d+", body.list("div.phan-trang > a")[4].href() ?: "", 0
            ) ?: "0"
        )
        if (page <= total) {
            for (node in body.list("div.truyen-list > div.list-truyen-item-wrap")) {
                val cid = node.href("h3 > a").orEmpty().replace("https://mangakakalot.com/manga/", "")
                val title = node.text("h3 > a")
                val cover = node.src("a > img")
                val update = node.list("a")[2].text()
                val author = node.text("div:eq(1) > div:eq(1) > dl:eq(1) > dd > a")
                list.add(Comic(TYPE, cid, title, cover, update, author))
            }
        }
        return list
    }

    private class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            var path = "category=".plus(args[CATEGORY_SUBJECT])
                .plus("&alpha=all&state=")
                .plus(args[CATEGORY_PROGRESS])
                .plus("&group=all")
                .trim()
            path = path.replace("\\s+".toRegex(), "-")
            return StringUtils.format("https://manganel.com/manga_list?type=new&page=%%d&%s", path)
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("All", "all"))
            list.add(Pair.create("Action", "2"))
            list.add(Pair.create("Adventure", "4"))
            list.add(Pair.create("Comedy", "6"))
            list.add(Pair.create("Cooking", "7"))
            list.add(Pair.create("Drama", "10"))
            list.add(Pair.create("Fantasy", "12"))
            list.add(Pair.create("Historical", "15"))
            list.add(Pair.create("Horror", "16"))
            list.add(Pair.create("Josei", "17"))
            list.add(Pair.create("Manhua", "44"))
            list.add(Pair.create("Manhwa", "43"))
            list.add(Pair.create("Martial Arts", "19"))
            list.add(Pair.create("Mature", "20"))
            list.add(Pair.create("Mecha", "21"))
            list.add(Pair.create("Medical", "22"))
            list.add(Pair.create("Mystery", "24"))
            list.add(Pair.create("One Shot", "25"))
            list.add(Pair.create("Psychological", "26"))
            list.add(Pair.create("Romance", "27"))
            list.add(Pair.create("Sci Fi", "29"))
            return list
        }

        override fun hasArea(): Boolean {
            return false
        }

        override fun getArea(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("All", ""))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("All", "all"))
            list.add(Pair.create("Ongoing", "Ongoing"))
            list.add(Pair.create("Completed", "Completed"))
            return list
        }

        override fun hasOrder(): Boolean {
            return false
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list = ArrayList<Pair<String, String>>()
            list.add(Pair.create("All", ""))
            return list
        }
    }

    companion object {
        const val TYPE = 44
        const val DEFAULT_TITLE = "Mangakakalot"

        @JvmField
        var cidUrl: String = ""

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
