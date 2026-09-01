package com.haleydu.cimoc.core

import android.util.Pair
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Task
import com.haleydu.cimoc.saf.DocumentFile
import com.haleydu.cimoc.source.Locality
import com.haleydu.cimoc.utils.StringUtils
import java.util.LinkedList
import java.util.regex.Pattern

object Local {

    private var chapterPattern: Pattern? = null

    @JvmStatic
    fun scan(root: DocumentFile): List<Pair<Comic, ArrayList<Task>>> {
        val result = ArrayList<Pair<Comic, ArrayList<Task>>>()

        val info = ScanInfo(root)
        countPicture(info)
        if (info.count > 5) {
            val pair = Pair.create(buildComic(info.dir, info.cover), ArrayList<Task>())
            pair.second.add(buildTask(info.dir, info.count, true))
            result.add(pair)
        } else {
            val list = LinkedList<DocumentFile>()
            list.add(root)

            while (!list.isEmpty()) {
                val dir = list[0]

                val guessChapter = LinkedList<ScanInfo>()
                val guessComic = LinkedList<ScanInfo>()
                val guessOther = classify(guessChapter, guessComic, dir)

                if (guessChapter.size > 2 * guessComic.size) {
                    result.add(merge(dir, guessChapter, guessComic))
                } else {
                    split(guessChapter, result)
                    split(guessComic, result)
                    list.addAll(guessOther)
                }

                list.removeAt(0)
            }
        }
        return result
    }

    @JvmStatic
    fun images(dir: DocumentFile, chapter: Chapter): List<ImageUrl> {
        val files = dir.listFiles(
            DocumentFile.DocumentFileFilter { file -> file.isFile && StringUtils.endWith(file.name, "jpg", "png", "jpeg") },
            Comparator { lhs, rhs -> lhs.name.compareTo(rhs.name) }
        )
        val list = Storage.buildImageUrlFromDocumentFile(files, chapter.title, chapter.count, chapter)
        if (list.isNotEmpty()) {
            return list
        }
        throw RuntimeException()
    }

    private fun countPicture(info: ScanInfo) {
        var name: String? = null
        var other = 0
        for (file in info.dir.listFiles()) {
            if (file.isFile && StringUtils.endWith(file.name, "png", "jpg", "jpeg")) {
                ++info.count
            } else {
                ++other
            }
            if (name == null || file.name.compareTo(name) < 0) {
                name = file.name
                info.cover = file.uri.toString()
            }
            if (other > 5) {
                info.count = 0
                break
            }
        }
    }

    private fun classify(
        chapter: MutableList<ScanInfo>,
        comic: MutableList<ScanInfo>,
        dir: DocumentFile
    ): List<DocumentFile> {
        val other = LinkedList<DocumentFile>()
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                val info = ScanInfo(file)
                countPicture(info)
                if (info.count > 5) {
                    if (isNameChapter(file)) {
                        chapter.add(info)
                    } else {
                        comic.add(info)
                    }
                } else {
                    other.add(file)
                }
            }
        }
        return other
    }

    private fun isNameChapter(file: DocumentFile): Boolean {
        if (chapterPattern == null) {
            chapterPattern = Pattern.compile("^[^(\\[]{0,5}[0-9]+|[0-9]+.{0,5}$")
        }
        val matcher = chapterPattern!!.matcher(file.name)
        return matcher.find() && (matcher.group()!!.length.toFloat() / file.name.length > 0.8)
    }

    private fun buildComic(dir: DocumentFile, cover: String?): Comic {
        return Comic(
            null, Locality.TYPE, dir.uri.toString(), dir.name, cover,
            false, true, null, null, null, null, null, null, null, null, null, null, null
        )
    }

    private fun buildTask(dir: DocumentFile, count: Int, single: Boolean): Task {
        return if (single) Task(null, -1, dir.uri.toString(), "第01话", count, count)
        else Task(null, -1, dir.uri.toString(), dir.name, count, count)
    }

    private fun merge(
        dir: DocumentFile,
        list1: List<ScanInfo>,
        list2: List<ScanInfo>
    ): Pair<Comic, ArrayList<Task>> {
        val pair = Pair.create(buildComic(dir, list1[0].cover), ArrayList<Task>())
        for (info in list1) {
            pair.second.add(buildTask(info.dir, info.count, false))
        }
        for (info in list2) {
            pair.second.add(buildTask(info.dir, info.count, false))
        }
        return pair
    }

    private fun split(list: List<ScanInfo>, result: MutableList<Pair<Comic, ArrayList<Task>>>) {
        for (info in list) {
            val pair = Pair.create(buildComic(info.dir, info.cover), ArrayList<Task>())
            pair.second.add(buildTask(info.dir, info.count, true))
            result.add(pair)
        }
    }

    private class ScanInfo(var dir: DocumentFile) {
        var cover: String? = null
        var count = 0
    }
}
