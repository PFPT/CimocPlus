package com.haleydu.cimoc.data

import com.haleydu.cimoc.db.CimocDatabase
import com.haleydu.cimoc.db.ImageUrlDao
import com.haleydu.cimoc.model.ImageUrl
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUrlManager @Inject constructor(
    private val database: CimocDatabase,
    private val mImageUrlDao: ImageUrlDao
) {

    fun runInTx(runnable: Runnable) {
        database.runInTransaction(runnable)
    }

    fun getListImageUrl(comicChapter: Long?): List<ImageUrl> {
        if (comicChapter == null) {
            return Collections.emptyList()
        }
        return mImageUrlDao.getListImageUrl(comicChapter)
    }

    fun load(id: Long): ImageUrl? {
        return mImageUrlDao.load(id)
    }

    fun updateOrInsert(imageUrlList: List<ImageUrl>?) {
        insertOrReplace(imageUrlList)
    }

    fun insertOrReplace(imageUrlList: List<ImageUrl>?) {
        if (imageUrlList == null || imageUrlList.isEmpty()) {
            return
        }
        try {
            database.runInTransaction {
                for (imageurl in imageUrlList) {
                    if (imageurl == null) {
                        continue
                    }
                    val id = mImageUrlDao.insertOrReplace(imageurl)
                    if (imageurl.id == null) {
                        imageurl.id = id
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    fun update(imageurl: ImageUrl) {
        mImageUrlDao.update(imageurl)
    }

    fun deleteByKey(key: Long) {
        mImageUrlDao.deleteByKey(key)
    }

    fun insert(imageurl: ImageUrl) {
        val id = mImageUrlDao.insertOrReplace(imageurl)
        if (imageurl.id == null) {
            imageurl.id = id
        }
    }
}
