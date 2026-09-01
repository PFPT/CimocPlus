package com.haleydu.cimoc.source

import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.SearchIterator
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONException

class Null : MangaParser() {

    init {
        mTitle = DEFAULT_TITLE
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? {
        return null
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator? {
        return null
    }

    override fun getInfoRequest(cid: String): Request? {
        return null
    }

    override fun parseInfo(html: String, comic: Comic): Comic {
        return comic
    }

    override fun parseChapter(html: String): List<Chapter>? {
        return null
    }

    @Throws(JSONException::class)
    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        return emptyList()
    }

    override fun getImagesRequest(cid: String, path: String): Request? {
        return null
    }

    override fun parseImages(html: String): List<ImageUrl>? {
        return null
    }

    @Throws(Manga.NetworkErrorException::class, JSONException::class)
    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl>? {
        return null
    }

    override fun getCheckRequest(cid: String): Request? {
        return null
    }

    override fun parseCheck(html: String): String? {
        return null
    }

    override fun parseCategory(html: String, page: Int): List<Comic> {
        return emptyList()
    }

    override fun getHeader(): Headers? {
        return null
    }

    override fun getUrl(cid: String): String {
        return ""
    }

    companion object {
        const val TYPE = -1
        const val DEFAULT_TITLE = "(null)"

        @JvmField
        val DEFAULT_SERVER: String? = null
    }
}
