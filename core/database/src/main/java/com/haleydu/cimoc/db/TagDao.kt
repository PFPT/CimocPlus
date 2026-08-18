package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.haleydu.cimoc.model.Tag

@Dao
interface TagDao {
    @Query("SELECT * FROM TAG")
    fun list(): List<Tag>

    @Query("SELECT * FROM TAG WHERE TITLE = :title LIMIT 1")
    fun load(title: String): Tag?

    @Insert
    fun insert(tag: Tag): Long

    @Update
    fun update(tag: Tag)

    @Delete
    fun delete(tag: Tag)
}
