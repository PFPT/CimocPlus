package com.haleydu.cimoc.parser

import android.net.Uri
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONException

abstract class MangaParser : Parser {

    protected var mTitle: String = ""
    protected var filter: MutableList<UrlFilter> = ArrayList()
    private var mCategory: Category? = null

    protected fun init(source: Source?, category: Category?) {
        mTitle = if (source != null && source.title != null) source.title else ""
        mCategory = category
        initUrlFilterList()
    }

    protected open fun initUrlFilterList() {
    }

    @Throws(JSONException::class)
    open override fun parseChapter(html: String): List<Chapter>? = null

    @Throws(JSONException::class)
    open override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter>? = parseChapter(html)

    @Throws(Manga.NetworkErrorException::class, JSONException::class)
    open override fun parseImages(html: String): List<ImageUrl>? = null

    @Throws(Manga.NetworkErrorException::class, JSONException::class)
    open override fun parseImages(html: String, chapter: Chapter): List<ImageUrl>? = parseImages(html)

    open override fun getChapterRequest(html: String, cid: String): Request? = null

    open override fun getImagesRequest(cid: String, path: String): Request? = null

    open override fun getLazyRequest(url: String): Request? = null

    open override fun parseLazy(html: String, url: String): String? = null

    open override fun getCheckRequest(cid: String): Request? = null

    open override fun parseCheck(html: String): String? = null

    open override fun getCategory(): Category? = mCategory

    open override fun getCategoryRequest(format: String, page: Int): Request {
        val url = StringUtils.format(format, page)
        return Request.Builder().url(url).build()
    }

    open override fun parseCategory(html: String, page: Int): List<Comic> = ArrayList()

    open override fun getTitle(): String = mTitle

    protected fun buildUrl(path: String, servers: Array<String>?): Array<String>? {
        if (servers != null) {
            val url = Array(servers.size) { i -> servers[i] + path }
            return url
        }
        return null
    }

    protected fun isFinish(text: String?): Boolean {
        return text != null && (text.contains("完结") || text.contains("Completed") || text.contains("完結"))
    }

    open override fun getUrl(cid: String): String = cid

    open override fun getHeader(): Headers? = null

    open override fun getHeader(url: String): Headers? = getHeader()

    open override fun getHeader(list: List<ImageUrl>): Headers? = getHeader()

    open override fun isHere(uri: Uri): Boolean {
        var `val` = false
        for (uf in filter) {
            `val` = `val` or (uri.host!!.indexOf(uf.Filter) != -1)
        }
        return `val`
    }

    open override fun getComicId(uri: Uri): String? {
        for (uf in filter) {
            if (uri.host!!.indexOf(uf.Filter) != -1) {
                return StringUtils.match(uf.Regex, uri.path ?: "", uf.Group)
            }
        }
        return null
    }
}
