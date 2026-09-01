package com.haleydu.cimoc.core

import android.content.ContentResolver
import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.utils.DecryptionUtils
import com.haleydu.cimoc.utils.DocumentUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import java.util.LinkedList

object Download {

    private const val JSON_KEY_VERSION = "version"
    private const val JSON_KEY_TYPE = "type"
    private const val JSON_KEY_TYPE_COMIC = "comic"
    private const val JSON_KEY_TYPE_CHAPTER = "chapter"
    private const val JSON_KEY_COMIC_LIST = "list"
    private const val JSON_KEY_COMIC_SOURCE = "source"
    private const val JSON_KEY_COMIC_CID = "cid"
    private const val JSON_KEY_COMIC_TITLE = "title"
    private const val JSON_KEY_COMIC_COVER = "cover"
    private const val JSON_KEY_CHAPTER_PATH = "path"
    private const val JSON_KEY_CHAPTER_TITLE = "title"

    private const val DOWNLOAD = "download"
    private const val FILE_INDEX = "index.cdif"
    private const val NO_MEDIA = ".nomedia"

    private fun createNoMedia(root: DocumentFile?) {
        val home = DocumentUtils.getOrCreateSubDirectory(root, DOWNLOAD)
        DocumentUtils.getOrCreateFile(home, NO_MEDIA)
    }

    private fun createComicIndex(root: DocumentFile?, comic: Comic): DocumentFile? {
        val home = DocumentUtils.getOrCreateSubDirectory(root, DOWNLOAD)
        val source = DocumentUtils.getOrCreateSubDirectory(home, comic.source.toString())
        val dir = DocumentUtils.getOrCreateSubDirectory(source, comic.cid)
        if (dir != null) {
            return DocumentUtils.getOrCreateFile(dir, FILE_INDEX)
        }
        return null
    }

    @JvmStatic
    fun updateComicIndex(resolver: ContentResolver, root: DocumentFile?, list: List<Chapter>, comic: Comic) {
        try {
            createNoMedia(root)
            val jsonString = getJsonFromComic(list, comic)
            val file = createComicIndex(root, comic)
            DocumentUtils.writeStringToFile(resolver, file, "cimoc".plus(jsonString))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(JSONException::class)
    private fun getJsonFromComic(list: List<Chapter>, comic: Comic): String {
        val `object` = JSONObject()
        `object`.put(JSON_KEY_VERSION, "1")
        `object`.put(JSON_KEY_TYPE, JSON_KEY_TYPE_COMIC)
        `object`.put(JSON_KEY_COMIC_SOURCE, comic.source)
        `object`.put(JSON_KEY_COMIC_CID, comic.cid)
        `object`.put(JSON_KEY_COMIC_TITLE, comic.title)
        `object`.put(JSON_KEY_COMIC_COVER, comic.cover)
        val array = JSONArray()
        for (chapter in list) {
            val temp = JSONObject()
            temp.put(JSON_KEY_CHAPTER_TITLE, chapter.title)
            temp.put(JSON_KEY_CHAPTER_PATH, chapter.path)
            array.put(temp)
        }
        `object`.put(JSON_KEY_COMIC_LIST, array)
        return `object`.toString()
    }

    @JvmStatic
    fun updateChapterIndex(resolver: ContentResolver, root: DocumentFile?, task: Task): DocumentFile? {
        try {
            val jsonString = getJsonFromChapter(task.title, task.path)
            val dir1 = DocumentUtils.getOrCreateSubDirectory(root, DOWNLOAD)
            val dir2 = DocumentUtils.getOrCreateSubDirectory(dir1, task.source.toString())
            val dir3 = DocumentUtils.getOrCreateSubDirectory(dir2, task.cid!!.replace("[:/(\\\\)(\\?)<>\"(\\|)(\\.)]".toRegex(), ""))
            val dir4 = DocumentUtils.getOrCreateSubDirectory(
                dir3,
                DecryptionUtils.urlDecrypt(task.path.replace("[:/(\\\\)(\\?)<>\"(\\|)(\\.)]".toRegex(), "-"))
            )
            if (dir4 != null) {
                val file = DocumentUtils.getOrCreateFile(dir4, FILE_INDEX)
                DocumentUtils.writeStringToFile(resolver, file, "cimoc".plus(jsonString))
                return dir4
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(JSONException::class)
    private fun getJsonFromChapter(title: String, path: String): String {
        val `object` = JSONObject()
        `object`.put(JSON_KEY_VERSION, "1")
        `object`.put(JSON_KEY_TYPE, JSON_KEY_TYPE_CHAPTER)
        `object`.put(JSON_KEY_CHAPTER_TITLE, title)
        `object`.put(JSON_KEY_CHAPTER_PATH, path)
        return `object`.toString()
    }

    private fun getComicDir(root: DocumentFile?, comic: Comic, title: String): DocumentFile? {
        var result = DocumentUtils.findFile(root, DOWNLOAD, comic.source.toString(), comic.cid)
        if (result == null) {
            result = DocumentUtils.findFile(root, DOWNLOAD, title, comic.title)
        }
        return result
    }

    @JvmStatic
    fun getComicIndex(resolver: ContentResolver, root: DocumentFile?, comic: Comic, title: String): List<String>? {
        val dir = getComicDir(root, comic, title)
        if (dir != null) {
            val file = dir.findFile(FILE_INDEX)
            if (file != null) {
                if (hasMagicNumber(resolver, file)) {
                    val jsonString = DocumentUtils.readLineFromFile(resolver, file)
                    if (jsonString != null) {
                        try {
                            return readPathFromJson(jsonString.substring(5))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        return null
    }

    @Throws(JSONException::class)
    private fun readPathFromJson(jsonString: String): List<String> {
        val jsonObject = JSONObject(jsonString)
        val array = if (jsonObject.has(JSON_KEY_COMIC_LIST)) jsonObject.getJSONArray(JSON_KEY_COMIC_LIST) else jsonObject.getJSONArray("c")
        val size = array.length()
        val list = ArrayList<String>(size)
        for (i in 0 until size) {
            val `object` = array.getJSONObject(i)
            list.add(if (`object`.has(JSON_KEY_CHAPTER_PATH)) `object`.getString(JSON_KEY_CHAPTER_PATH) else `object`.getString("p"))
        }
        return list
    }

    @JvmStatic
    fun getChapterDir(root: DocumentFile?, comic: Comic, chapter: Chapter, title: String): DocumentFile? {
        var result = DocumentUtils.findFile(
            root, DOWNLOAD,
            comic.source.toString(),
            comic.cid.replace("[:/(\\\\)(\\?)<>\"(\\|)(\\.)]".toRegex(), ""),
            DecryptionUtils.urlDecrypt(chapter.path!!.replace("[:/(\\\\)(\\?)<>\"(\\|)(\\.)]".toRegex(), "-"))
        )
        if (result == null) {
            result = DocumentUtils.findFile(root, DOWNLOAD, title, comic.title, chapter.title ?: "")
        }
        return result
    }

    @JvmStatic
    fun images(root: DocumentFile?, comic: Comic, chapter: Chapter, title: String): List<ImageUrl> {
        val dir = getChapterDir(root, comic, chapter, title)
        val files = dir!!.listFiles(
            DocumentFile.DocumentFileFilter { file -> !file.name.endsWith("cdif") },
            Comparator { lhs, rhs -> lhs.name.compareTo(rhs.name) }
        )
        val list = Storage.buildImageUrlFromDocumentFile(files, chapter.path ?: "", chapter.count, chapter)
        if (list.isNotEmpty()) {
            return list
        }
        throw RuntimeException()
    }

    @JvmStatic
    fun delete(root: DocumentFile?, comic: Comic, list: List<Chapter>, title: String) {
        for (chapter in list) {
            val dir = getChapterDir(root, comic, chapter, title)
            dir?.delete()
        }
    }

    @JvmStatic
    fun delete(root: DocumentFile?, comic: Comic, title: String) {
        val dir = getComicDir(root, comic, title)
        dir?.delete()
    }

    private fun getIndexJsonFromDir(resolver: ContentResolver, dir: DocumentFile): String? {
        if (dir.isDirectory) {
            val file = dir.findFile(FILE_INDEX)
            if (hasMagicNumber(resolver, file)) {
                val jsonString = DocumentUtils.readLineFromFile(resolver, file)
                if (jsonString != null) {
                    return jsonString.substring(5)
                }
            }
        }
        return null
    }

    private fun buildTaskFromDir(resolver: ContentResolver, dir: DocumentFile): Task? {
        val jsonString = getIndexJsonFromDir(resolver, dir)
        if (jsonString != null) {
            try {
                val jsonObject = JSONObject(jsonString)
                if (JSON_KEY_TYPE_CHAPTER == jsonObject.get(JSON_KEY_TYPE)) {
                    val count = DocumentUtils.countWithoutSuffix(dir, "cdif")
                    if (count != 0) {
                        val path = jsonObject.getString(JSON_KEY_CHAPTER_PATH)
                        val title = jsonObject.getString(JSON_KEY_CHAPTER_TITLE)
                        return Task(null, -1, path, title, count, count)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun buildComicFromDir(resolver: ContentResolver, dir: DocumentFile): Comic? {
        val jsonString = getIndexJsonFromDir(resolver, dir)
        if (jsonString != null) {
            try {
                val jsonObject = JSONObject(jsonString)
                if (JSON_KEY_TYPE_COMIC != jsonObject.get(JSON_KEY_TYPE)) {
                    return null
                }
                val source = jsonObject.getInt(JSON_KEY_COMIC_SOURCE)
                val title = jsonObject.getString(JSON_KEY_COMIC_TITLE)
                val cid = jsonObject.getString(JSON_KEY_COMIC_CID)
                val cover = jsonObject.getString(JSON_KEY_COMIC_COVER)
                return Comic(source, cid, title, cover, System.currentTimeMillis())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    @JvmStatic
    fun scan(resolver: ContentResolver, root: DocumentFile?): List<Pair<Comic, List<Task>>> {
        val result = LinkedList<Pair<Comic, List<Task>>>()
        val src = root ?: return result
        src.refresh()
        val downloadDir = DocumentUtils.getOrCreateSubDirectory(src, DOWNLOAD)
        if (downloadDir != null) {
            for (sourceDir in downloadDir.listFiles()) {
                if (sourceDir.isDirectory) {
                    for (comicDir in sourceDir.listFiles()) {
                        val comic = buildComicFromDir(resolver, comicDir)
                        if (comic != null) {
                            val list = LinkedList<Task>()
                            for (chapterDir in comicDir.listFiles()) {
                                val task = buildTaskFromDir(resolver, chapterDir)
                                if (task != null) {
                                    list.add(task)
                                }
                            }
                            if (list.isNotEmpty()) {
                                result.add(Pair.create(comic, list))
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    private fun hasMagicNumber(resolver: ContentResolver, file: DocumentFile?): Boolean {
        if (file != null) {
            val magic = DocumentUtils.readCharFromFile(resolver, file, 5)
            return Arrays.equals(magic, "cimoc".toCharArray())
        }
        return false
    }
}
