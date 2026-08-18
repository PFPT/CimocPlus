package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haleydu.cimoc.model.Chapter

@Dao
interface ChapterDao {
    @Query("SELECT * FROM CHAPTER WHERE SOURCE_COMIC = :sourceComic")
    fun getListChapter(sourceComic: Long): List<Chapter>

    @Query("SELECT * FROM CHAPTER WHERE PATH = :path AND TITLE = :title")
    fun getChapter(path: String, title: String): List<Chapter>

    @Query("SELECT * FROM CHAPTER WHERE _id = :id")
    fun load(id: Long): Chapter?

    @Insert
    fun insert(chapter: Chapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(chapter: Chapter): Long

    @Update
    fun update(chapter: Chapter)

    @Query("DELETE FROM CHAPTER WHERE _id = :key")
    fun deleteByKey(key: Long)
}
