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
        for (chapter in chapterList) {
            if (chapter.id == null) {
                insert(chapter)
            } else {
                update(chapter)
            }
        }
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
