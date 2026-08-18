package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haleydu.cimoc.model.Source

@Dao
interface SourceDao {
    @Query("SELECT * FROM SOURCE ORDER BY TYPE ASC")
    fun list(): List<Source>

    @Query("SELECT * FROM SOURCE WHERE ENABLE = 1 ORDER BY TYPE ASC")
    fun listEnable(): List<Source>

    @Query("SELECT * FROM SOURCE WHERE TYPE = :type LIMIT 1")
    fun load(type: Int): Source?

    @Insert
    fun insert(source: Source): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(source: Source): Long

    @Update
    fun update(source: Source)

    @Query("DELETE FROM SOURCE WHERE _id = :id")
    fun deleteById(id: Long)
}
