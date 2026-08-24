package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haleydu.cimoc.model.SourceHealth

@Dao
interface SourceHealthDao {
    @Query("SELECT * FROM SOURCE_HEALTH")
    fun list(): List<SourceHealth>

    @Query("SELECT * FROM SOURCE_HEALTH WHERE TYPE = :type LIMIT 1")
    fun load(type: Int): SourceHealth?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(health: SourceHealth)
}
