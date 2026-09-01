package com.haleydu.cimoc.source

import com.google.common.collect.Lists
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.RegexIterator
import com.haleydu.cimoc.parser.SearchIterator
import com.haleydu.cimoc.parser.UrlFilter
import com.haleydu.cimoc.soup.Node
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Matcher
import java.util.regex.Pattern

class Cartoonmad(source: Source?) : MangaParser() {

    init {
        init(source, null)
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request? {
        if (page != 1) return null
        val url = "https://www.cartoonmad.com/search.html"
        val body = FormBody.Builder()
            .add("keyword", URLEncoder.encode(keyword, "BIG5"))
            .add("searchtype", "all")
            .build()
        return Request.Builder().url(url).post(body).addHeader("Referer", "https://www.cartoonmad.com/").build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val pattern = Pattern.compile("<a href=comic\\/(\\d+)\\.html title=\"(.*?)\"><span class=\"covers\"><\\/span><img src=\"(.*?)\"")
        val matcher = pattern.matcher(html)
        return object : RegexIterator(matcher) {
            override fun parse(match: Matcher): Comic {
                val cid = match.group(1)
                val title = match.group(2)
                val cover = "https://www.cartoonmad.com" + match.group(3)
                return Comic(TYPE, cid, title, cover, "", "")
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "https://www.cartoonmad.com/comic/".plus(cid).plus(".html")
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.cartoonmad.com"))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "https://www.cartoonmad.com/comic/".plus(cid).plus(".html")
        return Request.Builder().url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val mTitle = Pattern.compile("<meta name=\"Keywords\" content=\"(.*?),").matcher(html)
        val title = if (mTitle.find()) mTitle.group(1) else ""
        val mCover = Pattern.compile("<div class=\"cover\"><\\/div><img src=\"(.*?)\"").matcher(html)
        val cover = if (mCover.find()) "https://www.cartoonmad.com" + mCover.group(1) else ""
        val update = ""
        val author = ""
        val mInro = Pattern.compile("<META name=\"description\" content=\"(.*?)\">").matcher(html)
        val intro = if (mInro.find()) mInro.group(1) else ""
        val status = false
        comic.setInfo(title, cover, update, intro, author, status)
        return comic
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list = LinkedList<Chapter>()
        val mChapter = Pattern.compile("<a href=(.*?) target=_blank>(.*?)<\\/a>&nbsp;").matcher(html)
        var i = 0
        while (mChapter.find()) {
            val title = mChapter.group(2)
            val path = mChapter.group(1)
            list.add(Chapter((sourceComic.toString() + "000" + i++).toLong(), sourceComic, title, path))
        }
        return Lists.reverse(list)
    }

    private var _cid: String? = null
    private var _path: String? = null

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = StringUtils.format("https://www.cartoonmad.com%s", path)
        _cid = cid
        _path = path
        return Request.Builder().url(url).build()
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl>? {
        val list = ArrayList<ImageUrl>()
        val pageMatcher = Pattern.compile("<a class=onpage>.*<a class=pages href=(.*)\\d{3}\\.html>(.*?)<\\/a>").matcher(html)
        if (!pageMatcher.find()) return null
        val page = Integer.parseInt(pageMatcher.group(2))
        for (i in 1..page) {
            val comicChapter = chapter.id
            val id = (comicChapter.toString() + "000" + i).toLong()
            list.add(ImageUrl(id, comicChapter, i, StringUtils.format("https://www.cartoonmad.com/comic/%s%03d.html", pageMatcher.group(1), i), true))
        }
        return list
    }

    override fun getLazyRequest(url: String): Request {
        return Request.Builder()
            .addHeader("Referer", url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
            .url(url).build()
    }

    override fun parseLazy(html: String, url: String): String? {
        val m = Pattern.compile("<img src=\"(.*?)\" border=\"0\" oncontextmenu").matcher(html)
        if (m.find()) {
            return "https://www.cartoonmad.com/comic/" + m.group(1)
        }
        return null
    }

    override fun getCheckRequest(cid: String): Request {
        return getInfoRequest(cid)
    }

    override fun getHeader(): Headers {
        return Headers.headersOf("Referer", "http://www.cartoonmad.com")
    }

    companion object {
        const val TYPE = 54
        const val DEFAULT_TITLE = "动漫狂"

        @JvmStatic
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true)
        }
    }
}
