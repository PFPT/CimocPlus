package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.paging.PagingSource
import com.haleydu.cimoc.model.Comic

@Dao
interface ComicDao {
    @Query("SELECT * FROM COMIC WHERE DOWNLOAD IS NOT NULL")
    fun listDownload(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE LOCAL = 1")
    fun listLocal(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE FAVORITE IS NOT NULL OR HISTORY IS NOT NULL")
    fun listFavoriteOrHistory(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE FAVORITE IS NOT NULL")
    fun listFavorite(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE FAVORITE IS NOT NULL ORDER BY HIGHLIGHT DESC, FAVORITE DESC")
    fun listFavoriteOrdered(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE FAVORITE IS NOT NULL ORDER BY HIGHLIGHT DESC, FAVORITE DESC")
    fun pagingFavorite(): PagingSource<Int, Comic>

    @Query("SELECT * FROM COMIC WHERE HISTORY IS NOT NULL ORDER BY HISTORY DESC")
    fun pagingHistory(): PagingSource<Int, Comic>

    @Query("SELECT * FROM COMIC WHERE FAVORITE IS NOT NULL AND FINISH = 1 ORDER BY HIGHLIGHT DESC, FAVORITE DESC")
    fun listFinish(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE FAVORITE IS NOT NULL AND FINISH IS NOT 1 ORDER BY HIGHLIGHT DESC, FAVORITE DESC")
    fun listContinue(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE HISTORY IS NOT NULL ORDER BY HISTORY DESC")
    fun listHistory(): List<Comic>

    @Query("SELECT * FROM COMIC WHERE DOWNLOAD IS NOT NULL ORDER BY DOWNLOAD DESC")
    fun listDownloadOrdered(): List<Comic>

    @Query("SELECT COMIC.* FROM COMIC INNER JOIN TAG_REF ON COMIC._id = TAG_REF.CID WHERE TAG_REF.TID = :tid ORDER BY HIGHLIGHT DESC, FAVORITE DESC")
    fun listFavoriteByTag(tid: Long): List<Comic>

    @Query("SELECT * FROM COMIC WHERE FAVORITE IS NOT NULL AND _id NOT IN (:ids)")
    fun listFavoriteNotIn(ids: List<Long>): List<Comic>

    @Query("SELECT COUNT(*) FROM COMIC WHERE SOURCE = :type AND FAVORITE IS NOT NULL")
    fun countBySource(type: Int): Long

    @Query("SELECT * FROM COMIC WHERE _id = :id")
    fun load(id: Long): Comic?

    @Query("SELECT * FROM COMIC WHERE SOURCE = :source AND CID = :cid LIMIT 1")
    fun load(source: Int, cid: String): Comic?

    @Query("SELECT * FROM COMIC WHERE HISTORY IS NOT NULL ORDER BY HISTORY DESC LIMIT 1")
    fun loadLast(): Comic?

    @Query("UPDATE COMIC SET HIGHLIGHT = 0 WHERE HIGHLIGHT = 1")
    fun cancelHighlight()

    @Insert
    fun insert(comic: Comic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(comic: Comic): Long

    @Update
    fun update(comic: Comic)

    @Query("DELETE FROM COMIC WHERE _id = :key")
    fun deleteByKey(key: Long)

    @Delete
    fun delete(comic: Comic)
}
