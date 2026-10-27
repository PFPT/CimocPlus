package com.haleydu.cimoc.data

import com.haleydu.cimoc.db.ChapterDao
import com.haleydu.cimoc.db.CimocDatabase
import com.haleydu.cimoc.model.Chapter
import java.util.concurrent.Callable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterManager @Inject constructor(
    private val database: CimocDatabase,
    private val mChapterDao: ChapterDao
) {

    fun runInTx(runnable: Runnable) {
        database.runInTransaction(runnable)
    }

    fun <T> callInTx(callable: Callable<T>): T {
        return database.runInTransaction(callable)
    }

    fun getListChapter(sourceComic: Long?): List<Chapter> {
        return mChapterDao.getListChapter(sourceComic)
    }

    fun getChapter(path: String?, title: String?): List<Chapter> {
        return mChapterDao.getChapter(path, title)
    }

    fun load(id: Long?): Chapter? {
        val key = id ?: return null
        return mChapterDao.load(key)
    }

    fun cancelHighlight() {
        database.comicDao().cancelHighlight()
    }

    fun updateOrInsert(chapterList: List<Chapter>) {
        if (chapterList.isEmpty()) return
        database.runInTransaction {
            for ((sourceComic, incoming) in chapterList.groupBy { it.sourceComic }) {
                val byPath = HashMap<String, Chapter>()
                for (chapter in mChapterDao.getListChapter(sourceComic)) {
                    val path = chapter.path
                    if (!path.isNullOrEmpty()) byPath.putIfAbsent(path, chapter)
                }
                for (chapter in incoming) {
                    val path = chapter.path?.takeIf { it.isNotEmpty() }
                    val prev = if (path != null) byPath[path] else null
                    if (prev != null) {
                        chapter.id = prev.id
                        chapter.complete = prev.complete
                        chapter.download = prev.download
                        if (prev.count > 0) chapter.count = prev.count
                        if (prev.tid != -1L) chapter.tid = prev.tid
                        if (chapter.sourceGroup.isNullOrEmpty()) chapter.sourceGroup = prev.sourceGroup
                        update(chapter)
                    } else {
                        chapter.id = null
                        if (path == null) chapter.path = null
                        insert(chapter)
                        if (path != null) byPath[path] = chapter
                    }
                }
            }
        }
    }

    fun markDownloaded(sourceComic: Long, path: String, count: Int) {
        if (path.isEmpty()) return
        var chapter = mChapterDao.getListChapter(sourceComic).firstOrNull { it.path == path }
        if (chapter == null) {
            chapter = mChapterDao.getChapter(path, null).firstOrNull { it.path == path }
        }
        if (chapter == null) return
        chapter.complete = true
        chapter.download = true
        if (count > 0) chapter.count = count
        update(chapter)
    }

    fun insertOrReplace(chapterList: List<Chapter>) {
        for (chapter in chapterList) {
            if (chapter.id != null) {
                mChapterDao.insertOrReplace(chapter)
            }
        }
    }

    fun update(chapter: Chapter) {
        if (chapter.id != null) {
            mChapterDao.update(chapter)
        }
    }

    fun deleteByKey(key: Long) {
        mChapterDao.deleteByKey(key)
    }

    fun insert(chapter: Chapter) {
        val id = mChapterDao.insert(chapter)
        chapter.id = id
    }
}
