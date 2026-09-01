package com.haleydu.cimoc.core

import com.haleydu.cimoc.data.ChapterManager
import com.haleydu.cimoc.parser.Parser
import com.haleydu.cimoc.model.ImageUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.InterruptedIOException

object MangaImages {

    @JvmStatic
    @Throws(InterruptedIOException::class)
    fun getImageUrls(
        client: OkHttpClient,
        parser: Parser,
        source: Int,
        cid: String,
        path: String,
        title: String,
        mChapterManager: ChapterManager
    ): List<ImageUrl> {
        val list = ArrayList<ImageUrl>()
        var response: Response? = null
        try {
            if (list.isNotEmpty()) {
                return list
            }
            val request = parser.getImagesRequest(cid, path)
            response = client.newCall(request!!).execute()
            if (response.isSuccessful) {
                val chapter = mChapterManager.getChapter(path, title)
                if (chapter != null && chapter.size >= 1) {
                    list.addAll(parser.parseImages(response.body!!.string(), chapter[0]) ?: emptyList())
                }
                if (list.size == 0) {
                    list.addAll(parser.parseImages(response.body!!.string()) ?: emptyList())
                }
            } else {
                throw Manga.NetworkErrorException()
            }
        } catch (e: InterruptedIOException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            response?.close()
        }
        return list
    }

    @JvmStatic
    @Throws(InterruptedIOException::class)
    fun getLazyUrl(client: OkHttpClient, parser: Parser, url: String): String? {
        var response: Response? = null
        return try {
            val request = parser.getLazyRequest(url)
            response = client.newCall(request!!).execute()
            if (response.isSuccessful) {
                parser.parseLazy(response.body!!.string(), url)
            } else {
                throw Manga.NetworkErrorException()
            }
        } catch (e: InterruptedIOException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            response?.close()
        }
    }
}
