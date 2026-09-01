package com.haleydu.cimoc.parser

import android.net.Uri
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import okhttp3.Headers
import okhttp3.Request
import org.json.JSONException
import java.io.UnsupportedEncodingException

interface Parser {

    @Throws(UnsupportedEncodingException::class, Exception::class)
    fun getSearchRequest(keyword: String, page: Int): Request?

    @Throws(JSONException::class)
    fun getSearchIterator(html: String, page: Int): SearchIterator?

    fun getInfoRequest(cid: String): Request?

    @Throws(UnsupportedEncodingException::class)
    fun parseInfo(html: String, comic: Comic): Comic

    fun getChapterRequest(html: String, cid: String): Request?

    @Throws(JSONException::class)
    fun parseChapter(html: String): List<Chapter>?

    @Throws(JSONException::class)
    fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter>?

    fun getImagesRequest(cid: String, path: String): Request?

    @Throws(Manga.NetworkErrorException::class, JSONException::class)
    fun parseImages(html: String): List<ImageUrl>?

    @Throws(Manga.NetworkErrorException::class, JSONException::class)
    fun parseImages(html: String, chapter: Chapter): List<ImageUrl>?

    fun getLazyRequest(url: String): Request?

    fun parseLazy(html: String, url: String): String?

    fun getCheckRequest(cid: String): Request?

    fun parseCheck(html: String): String?

    fun getCategory(): Category?

    fun getCategoryRequest(format: String, page: Int): Request

    fun parseCategory(html: String, page: Int): List<Comic>

    fun getTitle(): String

    fun getHeader(): Headers?

    fun getHeader(url: String): Headers?

    fun getHeader(list: List<ImageUrl>): Headers?

    fun getUrl(cid: String): String

    fun isHere(uri: Uri): Boolean

    fun getComicId(uri: Uri): String?
}

val Parser.title: String
    get() = getTitle()

val Parser.header: Headers?
    get() = getHeader()
