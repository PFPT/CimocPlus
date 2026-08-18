package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.haleydu.cimoc.model.TagRef

@Dao
interface TagRefDao {
    @Query("SELECT * FROM TAG_REF WHERE TID = :tid")
    fun listByTag(tid: Long): List<TagRef>

    @Query("SELECT * FROM TAG_REF WHERE CID = :cid")
    fun listByComic(cid: Long): List<TagRef>

    @Query("SELECT * FROM TAG_REF WHERE TID = :tid AND CID = :cid LIMIT 1")
    fun load(tid: Long, cid: Long): TagRef?

    @Insert
    fun insert(ref: TagRef): Long

    @Insert
    fun insert(entities: List<TagRef>)

    @Query("DELETE FROM TAG_REF WHERE TID = :tid")
    fun deleteByTag(tid: Long)

    @Query("DELETE FROM TAG_REF WHERE CID = :cid")
    fun deleteByComic(cid: Long)

    @Query("DELETE FROM TAG_REF WHERE TID = :tid AND CID = :cid")
    fun delete(tid: Long, cid: Long)
}
