package com.haleydu.cimoc.data

import com.haleydu.cimoc.db.CimocDatabase
import com.haleydu.cimoc.db.TagRefDao
import com.haleydu.cimoc.model.TagRef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRefManager @Inject constructor(
    private val database: CimocDatabase,
    private val mRefDao: TagRefDao
) {

    fun runInTx(runnable: Runnable) {
        database.runInTransaction(runnable)
    }

    fun listByTag(tid: Long): List<TagRef> = mRefDao.listByTag(tid)

    fun listByComic(cid: Long): List<TagRef> = mRefDao.listByComic(cid)

    fun load(tid: Long, cid: Long): TagRef? = mRefDao.load(tid, cid)

    fun insert(ref: TagRef): Long = mRefDao.insert(ref)

    fun insert(entities: Iterable<TagRef>) {
        val list = ArrayList<TagRef>()
        for (entity in entities) {
            list.add(entity)
        }
        mRefDao.insert(list)
    }

    fun insertInTx(entities: Iterable<TagRef>) {
        insert(entities)
    }

    fun deleteByTag(tid: Long) {
        mRefDao.deleteByTag(tid)
    }

    fun deleteByComic(cid: Long) {
        mRefDao.deleteByComic(cid)
    }

    fun delete(tid: Long, cid: Long) {
        mRefDao.delete(tid, cid)
    }
}
