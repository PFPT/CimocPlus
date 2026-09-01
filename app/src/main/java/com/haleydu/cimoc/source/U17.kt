package com.haleydu.cimoc.source

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_ORDER
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_PROGRESS
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_READER
import com.haleydu.cimoc.parser.Category.Companion.CATEGORY_SUBJECT
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.NodeIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.LinkedList

class U17(source: Source?) : MangaParser() {

    init {
        init(source, Category())
    }

    override fun getSearchRequest(keyword: String, page: Int): Request {
        val url = StringUtils.format("http://so.u17.com/all/%s/m0_p%d.html", keyword, page)
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list("#comiclist > div.search_list > div.comiclist > ul > li > div")) {
            override fun parse(node: Node): Comic {
                val cid = node.hrefWithSplit("div:eq(1) > h3 > strong > a", 1)
                val title = node.attr("div:eq(1) > h3 > strong > a", "title")
                val cover = node.src("div:eq(0) > a > img")
                val update = node.textWithSubstring("div:eq(1) > h3 > span.fr", 7)
                val author = node.text("div:eq(1) > h3 > a[title]")
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return StringUtils.format("http://www.u17.com/comic/%s.html", cid)
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.u17.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = StringUtils.format("http://www.u17.com/comic/%s.html", cid)
        return Request.Builder().url(url).build()
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text("div.comic_info > div.left > h1.fl")
        val cover = body.src("div.comic_info > div.left > div.coverBox > div.cover > a > img")
        val author = body.text("div.comic_info > div.right > div.author_info > div.info > a.name")
        val intro = body.text("#words")
        val status = isFinish(body.text("div.comic_info > div.left > div.info > div.top > div.line1 > span:eq(2)"))
        val update = body.textWithSubstring("div.main > div.chapterlist > div.chapterlist_box > div.bot > div.fl > span", 7)
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        val body = Node(html)
        var i = 0
        for (node in body.list("#chapter > li > a")) {
            val title = node.text()
            val path = node.hrefWithSplit(1)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return list
    }

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = "http://www.u17.com/comic/ajax.php?mod=chapter&act=get_chapter_v5&chapter_id=".plus(path)
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        try {
            val obj = JSONObject(html)
            val array = obj.getJSONArray("image_list")
            for (i in 0 until array.length()) {
                val comicChapter = chapter.id
                val id = (comicChapter.toString() + "000" + i).toLong()
                val url = array.getJSONObject(i).getString("src")
                list.add(ImageUrl(id, comicChapter, i + 1, url, false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun parseCheck(html: String): String? {
        return Node(html).textWithSubstring("div.main > div.chapterlist > div.chapterlist_box > div.bot > div.fl > span", 7)
    }

    override fun getCategoryRequest(format: String, page: Int): Request {
        val args = format.split(" ")
        val url = "http://www.u17.com/comic/ajax.php?mod=comic_list&act=comic_list_new_fun&a=get_comic_list"
        val body: RequestBody = FormBody.Builder()
            .add("data[group_id]", args[0])
            .add("data[theme_id]", args[1])
            .add("data[is_vip]", "no")
            .add("data[accredit]", "no")
            .add("data[color]", "no")
            .add("data[comic_type]", "no")
            .add("data[series_status]", args[2])
            .add("data[order]", args[3])
            .add("data[page_num]", page.toString())
            .add("data[read_mode]", "no")
            .build()
        return Request.Builder().url(url).post(body).addHeader("Referer", "http://www.u17.com").build()
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        val list: MutableList<Comic> = ArrayList()
        try {
            val array = JSONObject(html).getJSONArray("comic_list")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val cid = obj.getString("comic_id")
                val title = obj.getString("name")
                val cover = obj.getString("cover")
                list.add(Comic(TYPE, cid, title, cover, null, null))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://www.u17.com")
    }

    private class Category : MangaCategory() {
        override fun isComposite(): Boolean {
            return true
        }

        override fun getFormat(vararg args: String): String {
            return StringUtils.format(
                "%s %s %s %s",
                args[CATEGORY_READER], args[CATEGORY_SUBJECT], args[CATEGORY_PROGRESS], args[CATEGORY_ORDER]
            )
        }

        override fun getSubject(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("全部", "no"))
            list.add(Pair.create("搞笑", "1"))
            list.add(Pair.create("魔幻", "2"))
            list.add(Pair.create("生活", "3"))
            list.add(Pair.create("恋爱", "4"))
            list.add(Pair.create("动作", "5"))
            list.add(Pair.create("科幻", "6"))
            list.add(Pair.create("战争", "7"))
            list.add(Pair.create("体育", "8"))
            list.add(Pair.create("推理", "9"))
            list.add(Pair.create("惊悚", "11"))
            list.add(Pair.create("同人", "12"))
            return list
        }

        override fun hasReader(): Boolean {
            return true
        }

        override fun getReader(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("全部", "no"))
            list.add(Pair.create("少年", "1"))
            list.add(Pair.create("少女", "2"))
            list.add(Pair.create("耽美", "3"))
            list.add(Pair.create("绘本", "4"))
            return list
        }

        override fun hasProgress(): Boolean {
            return true
        }

        override fun getProgress(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("全部", "no"))
            list.add(Pair.create("连载", "0"))
            list.add(Pair.create("完结", "1"))
            return list
        }

        override fun hasOrder(): Boolean {
            return true
        }

        override fun getOrder(): List<Pair<String, String>> {
            val list: MutableList<Pair<String, String>> = ArrayList()
            list.add(Pair.create("全站最热", "0"))
            list.add(Pair.create("更新时间", "1"))
            list.add(Pair.create("上升最快", "2"))
            list.add(Pair.create("最新发布", "3"))
            return list
        }
    }

    companion object {
        const val TYPE = 4
        const val DEFAULT_TITLE = "有妖气"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
