package com.haleydu.cimoc.data

import com.haleydu.cimoc.db.TagDao
import com.haleydu.cimoc.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagManager @Inject constructor(
    private val mTagDao: TagDao
) {

    fun list(): List<Tag> = mTagDao.list()

    fun load(title: String?): Tag? = mTagDao.load(title)

    fun insert(tag: Tag) {
        val id = mTagDao.insert(tag)
        tag.id = id
    }

    fun update(tag: Tag) {
        mTagDao.update(tag)
    }

    fun delete(entity: Tag) {
        mTagDao.delete(entity)
    }

    companion object {
        const val TAG_CONTINUE = -101L
        const val TAG_FINISH = -100L
    }
}
