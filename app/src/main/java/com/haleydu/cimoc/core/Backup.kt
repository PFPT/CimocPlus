package com.haleydu.cimoc.core

import android.content.ContentResolver
import android.util.Pair
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.TypeReference
import com.google.gson.Gson
import com.haleydu.cimoc.App
import com.haleydu.cimoc.di.AppEntryPoint
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.utils.DocumentUtils
import com.haleydu.cimoc.utils.StringUtils
import dagger.hilt.android.EntryPointAccessors
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import java.util.LinkedList

object Backup {

    private const val BACKUP = "backup"
    private const val SUFFIX_CIMOC = "cimoc"
    private const val SUFFIX_CFBF = "cfbf"
    private const val SUFFIX_CTBF = "ctbf"
    private const val SUFFIX_CSBF = "csbf"

    private const val JSON_CIMOC_KEY_COMIC_SOURCE = "s"
    private const val JSON_CIMOC_KEY_COMIC_CID = "i"
    private const val JSON_CIMOC_KEY_COMIC_TITLE = "t"
    private const val JSON_CIMOC_KEY_COMIC_COVER = "c"
    private const val JSON_CIMOC_KEY_COMIC_UPDATE = "u"
    private const val JSON_CIMOC_KEY_COMIC_FINISH = "f"
    private const val JSON_CIMOC_KEY_COMIC_LAST = "l"
    private const val JSON_CIMOC_KEY_COMIC_PAGE = "p"

    private const val JSON_KEY_VERSION = "version"
    private const val JSON_KEY_TAG_ARRAY = "tag"
    private const val JSON_KEY_TAG_TITLE = "title"
    private const val JSON_KEY_COMIC_ARRAY = "comic"
    private const val JSON_KEY_COMIC_SOURCE = "source"
    private const val JSON_KEY_COMIC_CID = "cid"
    private const val JSON_KEY_COMIC_TITLE = "title"
    private const val JSON_KEY_COMIC_COVER = "cover"
    private const val JSON_KEY_COMIC_UPDATE = "update"
    private const val JSON_KEY_COMIC_FINISH = "finish"
    private const val JSON_KEY_COMIC_LAST = "last"
    private const val JSON_KEY_COMIC_PAGE = "page"
    private const val JSON_KEY_COMIC_CHAPTER = "chapter"
    private const val JSON_KEY_COMIC_FAVORITE = "favorite"
    private const val JSON_KEY_COMIC_HISTORY = "history"

    @JvmStatic
    fun loadFavorite(root: DocumentFile): Array<String> {
        return load(root, SUFFIX_CIMOC, SUFFIX_CFBF)
    }

    @JvmStatic
    fun loadTag(root: DocumentFile): Array<String> {
        return load(root, SUFFIX_CTBF)
    }

    @JvmStatic
    fun loadSettings(root: DocumentFile): Array<String> {
        return load(root, SUFFIX_CSBF)
    }

    @JvmStatic
    fun loadClearBackup(root: DocumentFile): Array<String> {
        return load(root, SUFFIX_CIMOC, SUFFIX_CFBF, SUFFIX_CTBF, SUFFIX_CSBF)
    }

    private fun load(root: DocumentFile, vararg suffix: String): Array<String> {
        val dir = DocumentUtils.getOrCreateSubDirectory(root, BACKUP)
        if (dir != null) {
            val files = DocumentUtils.listFilesWithSuffix(dir, *suffix)
            if (files.isNotEmpty()) {
                Arrays.sort(files)
                return files
            }
        }
        throw RuntimeException()
    }

    @JvmStatic
    fun saveComicAuto(resolver: ContentResolver, root: DocumentFile, list: List<Comic>) {
        val dir = DocumentUtils.getOrCreateSubDirectory(root, BACKUP)
        if (dir != null) {
            try {
                val result = JSONObject()
                result.put(JSON_KEY_VERSION, 1)
                result.put(JSON_KEY_COMIC_ARRAY, buildComicArray(list))
                val file = DocumentUtils.getOrCreateFile(dir, "automatic.".plus(SUFFIX_CFBF))
                DocumentUtils.writeStringToFile(resolver, file, result.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun saveComic(resolver: ContentResolver, root: DocumentFile, list: List<Comic>): Int {
        val dir = DocumentUtils.getOrCreateSubDirectory(root, BACKUP)
        if (dir != null) {
            try {
                val result = JSONObject()
                result.put(JSON_KEY_VERSION, 1)
                result.put(JSON_KEY_COMIC_ARRAY, buildComicArray(list))
                val filename = StringUtils.getDateStringWithSuffix(SUFFIX_CFBF)
                val file = DocumentUtils.getOrCreateFile(dir, filename)
                DocumentUtils.writeStringToFile(resolver, file, result.toString())
                return list.size
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return -1
    }

    @JvmStatic
    fun saveTag(resolver: ContentResolver, root: DocumentFile, list: List<Pair<Tag, List<Comic>>>): Int {
        val dir = DocumentUtils.getOrCreateSubDirectory(root, BACKUP)
        if (dir != null) {
            try {
                val result = JSONObject()
                result.put(JSON_KEY_VERSION, 2)
                result.put(JSON_KEY_TAG_ARRAY, buildTagArray(list))
                val filename = StringUtils.getDateStringWithSuffix(SUFFIX_CTBF)
                val file = DocumentUtils.getOrCreateFile(dir, filename)
                DocumentUtils.writeStringToFile(resolver, file, result.toString())
                return list.size
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return -1
    }

    @JvmStatic
    fun saveSetting(resolver: ContentResolver, root: DocumentFile, settingMap: Map<*, *>): Int {
        val dir = DocumentUtils.getOrCreateSubDirectory(root, BACKUP)
        if (dir != null) {
            try {
                val gson = Gson()
                val result = gson.toJson(settingMap)
                val filename = StringUtils.getDateStringWithSuffix(SUFFIX_CSBF)
                val file = DocumentUtils.getOrCreateFile(dir, filename)
                DocumentUtils.writeStringToFile(resolver, file, result.toString())
                return settingMap.size
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return -1
    }

    @Throws(JSONException::class)
    private fun buildTagArray(list: List<Pair<Tag, List<Comic>>>): JSONArray {
        val array = JSONArray()
        for (pair in list) {
            array.put(buildTagObject(pair.first, pair.second))
        }
        return array
    }

    @Throws(JSONException::class)
    private fun buildTagObject(tag: Tag, list: List<Comic>): JSONObject {
        val `object` = JSONObject()
        `object`.put(JSON_KEY_TAG_TITLE, tag.title)
        `object`.put(JSON_KEY_COMIC_ARRAY, buildComicArray(list))
        return `object`
    }

    @Throws(JSONException::class)
    private fun buildComicArray(list: List<Comic>): JSONArray {
        val array = JSONArray()
        for (comic in list) {
            array.put(buildComicObject(comic))
        }
        return array
    }

    @Throws(JSONException::class)
    private fun buildComicObject(comic: Comic): JSONObject {
        val `object` = JSONObject()
        `object`.put(JSON_KEY_COMIC_SOURCE, comic.source)
        `object`.put(JSON_KEY_COMIC_CID, comic.cid)
        `object`.put(JSON_KEY_COMIC_TITLE, comic.title)
        `object`.put(JSON_KEY_COMIC_COVER, comic.cover)
        `object`.put(JSON_KEY_COMIC_UPDATE, comic.update)
        `object`.put(JSON_KEY_COMIC_FINISH, comic.finish)
        `object`.put(JSON_KEY_COMIC_FAVORITE, comic.favorite)
        `object`.put(JSON_KEY_COMIC_HISTORY, comic.history)
        `object`.put(JSON_KEY_COMIC_LAST, comic.last)
        `object`.put(JSON_KEY_COMIC_PAGE, comic.page)
        `object`.put(JSON_KEY_COMIC_CHAPTER, comic.chapter)
        return `object`
    }

    private fun readBackupFile(resolver: ContentResolver, root: DocumentFile, filename: String): String? {
        val dir = DocumentUtils.getOrCreateSubDirectory(root, BACKUP)
        if (dir != null) {
            val file = dir.findFile(filename)
            return DocumentUtils.readLineFromFile(resolver, file)
        }
        return null
    }

    @JvmStatic
    fun restoreTag(resolver: ContentResolver, root: DocumentFile, filename: String): List<Pair<Tag, List<Comic>>> {
        val result = LinkedList<Pair<Tag, List<Comic>>>()
        val jsonString = readBackupFile(resolver, root, filename)
        try {
            val `object` = JSONObject(jsonString)
            when (`object`.getInt(JSON_KEY_VERSION)) {
                1 -> result.add(
                    Pair.create(
                        Tag(null, `object`.getJSONObject(JSON_KEY_TAG_ARRAY).getString(JSON_KEY_TAG_TITLE)),
                        loadComicArray(`object`.getJSONArray(JSON_KEY_COMIC_ARRAY), SUFFIX_CTBF)
                    )
                )
                2 -> result.addAll(loadTagArray(`object`.getJSONArray(JSON_KEY_TAG_ARRAY)))
            }
            return result
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun restoreComic(resolver: ContentResolver, root: DocumentFile, filename: String): List<Comic> {
        val list = LinkedList<Comic>()
        val jsonString = readBackupFile(resolver, root, filename)
        try {
            if (filename.endsWith(SUFFIX_CIMOC)) {
                list.addAll(loadComicArray(JSONArray(jsonString), SUFFIX_CIMOC))
            } else if (filename.endsWith(SUFFIX_CFBF)) {
                val `object` = JSONObject(jsonString)
                list.addAll(loadComicArray(`object`.getJSONArray(JSON_KEY_COMIC_ARRAY), SUFFIX_CFBF))
            }
            return list
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun clearBackup(root: DocumentFile): Int {
        val dir = DocumentUtils.getOrCreateSubDirectory(root, BACKUP)
        if (dir != null) dir.delete()
        return 1
    }

    @JvmStatic
    fun restoreSetting(resolver: ContentResolver, root: DocumentFile, filename: String): Map<String, *> {
        val jsonString = readBackupFile(resolver, root, filename)
        val entries = JSON.parseObject(jsonString, object : TypeReference<Map<String, *>>() {})
        if (filename.endsWith(SUFFIX_CSBF)) {
            for (entry in entries.entries) {
                EntryPointAccessors.fromApplication(App.getAppContext(), AppEntryPoint::class.java)
                    .preferenceManager()
                    .putObject(entry.key.toString(), entry.value)
            }
        }
        return entries
    }

    @Throws(JSONException::class)
    private fun loadTagArray(array: JSONArray): List<Pair<Tag, List<Comic>>> {
        val list = LinkedList<Pair<Tag, List<Comic>>>()
        for (i in 0 until array.length()) {
            val `object` = array.getJSONObject(i)
            val tag = Tag(null, `object`.getString(JSON_KEY_TAG_TITLE))
            list.add(Pair.create(tag, loadComicArray(`object`.getJSONArray(JSON_KEY_COMIC_ARRAY), SUFFIX_CFBF)))
        }
        return list
    }

    @Throws(JSONException::class)
    private fun loadComicArray(array: JSONArray, suffix: String): List<Comic> {
        val list = LinkedList<Comic>()
        when (suffix) {
            SUFFIX_CIMOC -> {
                for (i in 0 until array.length()) {
                    val `object` = array.getJSONObject(i)
                    val source = `object`.getInt(JSON_CIMOC_KEY_COMIC_SOURCE)
                    val cid = `object`.getString(JSON_CIMOC_KEY_COMIC_CID)
                    val title = `object`.getString(JSON_CIMOC_KEY_COMIC_TITLE)
                    val cover = `object`.getString(JSON_CIMOC_KEY_COMIC_COVER)
                    val update = `object`.optString(JSON_CIMOC_KEY_COMIC_UPDATE, null)
                    val finish = if (`object`.has(JSON_CIMOC_KEY_COMIC_FINISH)) `object`.getBoolean(JSON_CIMOC_KEY_COMIC_FINISH) else null
                    val last = `object`.optString(JSON_CIMOC_KEY_COMIC_LAST, null)
                    val page = if (`object`.has(JSON_CIMOC_KEY_COMIC_PAGE)) `object`.getInt(JSON_CIMOC_KEY_COMIC_PAGE) else null
                    list.add(
                        Comic(
                            null, source, cid, title, cover, false, false, update,
                            finish, null, null, null, last, page, null, null, null, null
                        )
                    )
                }
            }
            SUFFIX_CFBF, SUFFIX_CTBF -> {
                for (i in 0 until array.length()) {
                    val `object` = array.getJSONObject(i)
                    val source = `object`.getInt(JSON_KEY_COMIC_SOURCE)
                    val cid = `object`.getString(JSON_KEY_COMIC_CID)
                    val title = `object`.getString(JSON_KEY_COMIC_TITLE)
                    val cover = `object`.getString(JSON_KEY_COMIC_COVER)
                    val update = `object`.optString(JSON_KEY_COMIC_UPDATE, null)
                    val finish = if (`object`.has(JSON_KEY_COMIC_FINISH)) `object`.getBoolean(JSON_KEY_COMIC_FINISH) else null
                    val last = `object`.optString(JSON_KEY_COMIC_LAST, null)
                    val page = if (`object`.has(JSON_KEY_COMIC_PAGE)) `object`.getInt(JSON_KEY_COMIC_PAGE) else null
                    val chapter = `object`.optString(JSON_KEY_COMIC_CHAPTER, null)
                    var favorite = if (`object`.has(JSON_KEY_COMIC_FAVORITE)) `object`.getLong(JSON_KEY_COMIC_FAVORITE) else null
                    val history = if (`object`.has(JSON_KEY_COMIC_HISTORY)) `object`.getLong(JSON_KEY_COMIC_HISTORY) else null
                    if (favorite == null && history == null) {
                        favorite = System.currentTimeMillis()
                    }
                    list.add(
                        Comic(
                            null, source, cid, title, cover, false, false, update,
                            finish, favorite, history, null, last, page, chapter, null, null, null
                        )
                    )
                }
            }
        }
        return list
    }
}
