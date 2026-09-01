package com.haleydu.cimoc.data

import androidx.paging.PagingSource
import com.haleydu.cimoc.db.CimocDatabase
import com.haleydu.cimoc.db.ComicDao
import com.haleydu.cimoc.model.Comic
import java.util.concurrent.Callable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicManager @Inject constructor(
    private val database: CimocDatabase,
    private val mComicDao: ComicDao
) {

    fun runInTx(runnable: Runnable) {
        database.runInTransaction(runnable)
    }

    fun <T> callInTx(callable: Callable<T>): T {
        return database.runInTransaction(callable)
    }

    fun listDownload(): List<Comic> = mComicDao.listDownload()

    fun listLocal(): List<Comic> = mComicDao.listLocal()

    fun listFavorite(): List<Comic> = mComicDao.listFavorite()

    fun listFavoriteOrdered(): List<Comic> = mComicDao.listFavoriteOrdered()

    fun pagingFavorite(): PagingSource<Int, Comic> = mComicDao.pagingFavorite()

    fun pagingHistory(): PagingSource<Int, Comic> = mComicDao.pagingHistory()

    fun listFinish(): List<Comic> = mComicDao.listFinish()

    fun listContinue(): List<Comic> = mComicDao.listContinue()

    fun listHistory(): List<Comic> = mComicDao.listHistory()

    fun listDownloadOrdered(): List<Comic> = mComicDao.listDownloadOrdered()

    fun listFavoriteByTag(id: Long): List<Comic> = mComicDao.listFavoriteByTag(id)

    fun listFavoriteNotIn(collections: Collection<Long>?): List<Comic> {
        if (collections == null || collections.isEmpty()) {
            return mComicDao.listFavoriteOrdered()
        }
        return mComicDao.listFavoriteNotIn(ArrayList(collections))
    }

    fun listFavoriteOrHistory(): List<Comic> = mComicDao.listFavoriteOrHistory()

    fun countBySource(type: Int): Long = mComicDao.countBySource(type)

    fun load(id: Long?): Comic? {
        val key = id ?: return null
        return mComicDao.load(key)
    }

    fun load(source: Int, cid: String?): Comic? = mComicDao.load(source, cid)

    fun loadOrCreate(source: Int, cid: String?): Comic {
        val comic = load(source, cid)
        return comic ?: Comic(source, cid)
    }

    fun loadLast(): Comic? = mComicDao.loadLast()

    fun cancelHighlight() {
        mComicDao.cancelHighlight()
    }

    fun updateOrInsert(comic: Comic) {
        if (comic.id == null) {
            insert(comic)
        } else {
            update(comic)
        }
    }

    fun update(comic: Comic) {
        mComicDao.update(comic)
    }

    fun insertOrReplace(comic: Comic) {
        val id = mComicDao.insertOrReplace(comic)
        comic.id = id
    }

    fun updateOrDelete(comic: Comic) {
        if (comic.favorite == null && comic.history == null && comic.download == null) {
            mComicDao.delete(comic)
            comic.id = null
        } else {
            update(comic)
        }
    }

    fun deleteByKey(key: Long) {
        mComicDao.deleteByKey(key)
    }

    fun insert(comic: Comic) {
        val id = mComicDao.insert(comic)
        comic.id = id
    }
}
