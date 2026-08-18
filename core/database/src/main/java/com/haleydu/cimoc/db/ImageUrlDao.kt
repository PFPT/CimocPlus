package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haleydu.cimoc.model.ImageUrl

@Dao
interface ImageUrlDao {
    @Query("SELECT * FROM IMAGE_URL WHERE COMIC_CHAPTER = :comicChapter")
    fun getListImageUrl(comicChapter: Long): List<ImageUrl>

    @Query("SELECT * FROM IMAGE_URL WHERE _id = :id")
    fun load(id: Long): ImageUrl?

    @Insert
    fun insert(imageUrl: ImageUrl): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(imageUrl: ImageUrl): Long

    @Update
    fun update(imageUrl: ImageUrl)

    @Query("DELETE FROM IMAGE_URL WHERE _id = :key")
    fun deleteByKey(key: Long)
}
