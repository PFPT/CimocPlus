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
                val html = response.body?.string().orEmpty()
                val chapter = mChapterManager.getChapter(path, title)
                if (chapter.isNotEmpty()) {
                    list.addAll(parser.parseImages(html, chapter[0]) ?: emptyList())
                }
                if (list.isEmpty()) {
                    list.addAll(parser.parseImages(html) ?: emptyList())
                }
            } else {
                throw Manga.NetworkErrorException()
            }
            if (list.isEmpty()) {
                throw Manga.ParseErrorException()
            }
        } catch (e: InterruptedIOException) {
            throw e
        } catch (e: Manga.NetworkErrorException) {
            throw e
        } catch (e: Manga.ParseErrorException) {
            throw e
        } catch (_: java.io.IOException) {
            throw Manga.NetworkErrorException()
        } catch (_: Exception) {
            throw Manga.ParseErrorException()
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
        } catch (e: Manga.NetworkErrorException) {
            throw e
        } catch (e: Manga.ParseErrorException) {
            throw e
        } catch (_: java.io.IOException) {
            throw Manga.NetworkErrorException()
        } catch (_: Exception) {
            throw Manga.ParseErrorException()
        } finally {
            response?.close()
        }
    }
}
