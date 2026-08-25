package com.haleydu.cimoc.ui.reader

import com.haleydu.cimoc.model.Chapter

object ChapterListHolder {

    @Volatile
    private var comicId: Long = -1

    @Volatile
    private var chapters: ArrayList<Chapter>? = null

    @JvmStatic
    @Synchronized
    fun put(id: Long, list: List<Chapter>) {
        comicId = id
        chapters = ArrayList(list)
    }

    @JvmStatic
    @Synchronized
    fun put(list: List<Chapter>) {
        comicId = -1
        chapters = ArrayList(list)
    }

    @JvmStatic
    @Synchronized
    fun get(@Suppress("UNUSED_PARAMETER") id: Long): ArrayList<Chapter>? {
        val stored = chapters ?: return null
        return ArrayList(stored)
    }

    @JvmStatic
    @Synchronized
    fun get(): ArrayList<Chapter>? {
        val stored = chapters ?: return null
        return ArrayList(stored)
    }

    @JvmStatic
    @Synchronized
    fun take(): ArrayList<Chapter>? {
        val result = chapters
        chapters = null
        comicId = -1
        return result
    }
}
