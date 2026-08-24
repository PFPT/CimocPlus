package com.haleydu.cimoc.script

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Source
import com.haleydu.cimoc.model.SourceRule
import com.haleydu.cimoc.parser.MangaCategory
import com.haleydu.cimoc.parser.MangaParser
import com.haleydu.cimoc.parser.Category
import com.haleydu.cimoc.parser.SearchIterator
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class JsMangaParser(
    source: Source,
    private val rule: SourceRule,
    private val runner: ScriptRunner
) : MangaParser() {

    private val categoryImpl = JsCategory()

    private val supportCache = HashMap<String, Boolean>()

    init {
        mTitle = source.title
    }

    private fun supports(name: String): Boolean {
        return supportCache.getOrPut(name) { runner.hasFunction(rule.scriptContent, name) }
    }

    fun search(keyword: String, page: Int): List<Comic> {
        if (!supports(FN_SEARCH)) return emptyList()
        val json = runner.invokeBlocking(rule.scriptContent, FN_SEARCH, keyword, page)
        return parseComicList(json)
    }

    fun fillInfo(comic: Comic) {
        if (!supports(FN_DETAIL)) return
        val json = runner.invokeBlocking(rule.scriptContent, FN_DETAIL, comic.cid)
        val obj = parseObject(json) ?: return
        comic.setInfo(
            obj.optString("title", comic.title),
            obj.optString("cover", comic.cover),
            obj.optString("update", comic.update),
            obj.optString("intro", comic.intro),
            obj.optString("author", comic.author),
            finishValue(obj)
        )
    }

    fun chapterList(comic: Comic, sourceComic: Long): List<Chapter> {
        if (!supports(FN_CHAPTERS)) return emptyList()
        val json = runner.invokeBlocking(rule.scriptContent, FN_CHAPTERS, comic.cid)
        val array = parseArray(json) ?: return emptyList()
        val list = ArrayList<Chapter>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optString("title")
            val path = obj.optString("path")
            if (title.isNotEmpty() && path.isNotEmpty()) {
                list.add(Chapter(null, sourceComic, title, path))
            }
        }
        return list
    }

    fun imageList(cid: String, path: String, chapter: Chapter): List<ImageUrl> {
        if (!supports(FN_IMAGES)) return emptyList()
        val json = runner.invokeBlocking(rule.scriptContent, FN_IMAGES, cid, path)
        val array = parseArray(json) ?: return emptyList()
        val chapterId = chapter.id ?: 0L
        val list = ArrayList<ImageUrl>(array.length())
        for (i in 0 until array.length()) {
            val url = array.optString(i)
            if (url.isNotEmpty()) {
                list.add(ImageUrl(null, chapterId, i + 1, url, false))
            }
        }
        return list
    }

    fun category(argsJson: String, page: Int): List<Comic> {
        if (!supports(FN_CATEGORY)) return emptyList()
        val json = runner.invokeBlocking(rule.scriptContent, FN_CATEGORY, argsJson, page)
        return parseComicList(json)
    }

    fun checkUpdate(cid: String): String? {
        if (!supports(FN_DETAIL)) return null
        val json = runner.invokeBlocking(rule.scriptContent, FN_DETAIL, cid)
        val obj = parseObject(json) ?: return null
        return obj.optString("update").takeIf { it.isNotEmpty() }
    }

    override fun getCategory(): Category? {
        return if (supports(FN_CATEGORY)) categoryImpl else null
    }

    override fun getSearchRequest(keyword: String, page: Int): Request? = null

    override fun getSearchIterator(html: String, page: Int): SearchIterator? = null

    override fun getInfoRequest(cid: String): Request? = null

    override fun parseInfo(html: String, comic: Comic): Comic {
        fillInfo(comic)
        return comic
    }

    override fun parseChapter(html: String): List<Chapter> = emptyList()

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        return chapterList(comic, sourceComic)
    }

    override fun getImagesRequest(cid: String, path: String): Request? = null

    override fun parseImages(html: String): List<ImageUrl> = emptyList()

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        return imageList(chapter.path, chapter.path, chapter)
    }

    override fun getCheckRequest(cid: String): Request? = null

    override fun parseCheck(html: String): String? = null

    override fun parseCategory(html: String, page: Int): List<Comic> = emptyList()

    private fun parseComicList(json: String): List<Comic> {
        val array = parseArray(json) ?: return emptyList()
        val list = ArrayList<Comic>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val cid = obj.optString("cid")
            val title = obj.optString("title")
            if (cid.isEmpty() || title.isEmpty()) continue
            list.add(
                Comic(
                    rule.type,
                    cid,
                    title,
                    obj.optString("cover"),
                    obj.optString("update"),
                    obj.optString("author")
                )
            )
        }
        return list
    }

    private fun parseArray(json: String): JSONArray? {
        val text = json.trim()
        if (text.isEmpty()) return null
        return try {
            if (text.startsWith("[")) {
                JSONArray(text)
            } else {
                val obj = JSONObject(text)
                obj.optJSONArray("list")
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseObject(json: String): JSONObject? {
        val text = json.trim()
        if (text.isEmpty()) return null
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            null
        }
    }

    private fun finishValue(obj: JSONObject): Boolean {
        if (!obj.has("finish")) return false
        val value = obj.opt("finish")
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> {
                val text = value?.toString() ?: ""
                text.contains("完") || text.equals("true", true) || text == "1"
            }
        }
    }

    private class JsCategory : MangaCategory() {
        override fun isComposite(): Boolean = true

        override fun getSubject(): List<Pair<String, String>> {
            return listOf(Pair("全部", ""))
        }

        override fun getFormat(vararg args: String): String {
            val array = JSONArray()
            for (arg in args) {
                array.put(arg ?: "")
            }
            return array.toString()
        }
    }

    companion object {
        private const val FN_SEARCH = "search"
        private const val FN_DETAIL = "getDetail"
        private const val FN_CHAPTERS = "getChapterList"
        private const val FN_IMAGES = "getImageList"
        private const val FN_CATEGORY = "getCategory"
    }
}
