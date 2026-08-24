package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haleydu.cimoc.model.SourceRule

@Dao
interface SourceRuleDao {
    @Query("SELECT * FROM SOURCE_RULE ORDER BY TYPE ASC")
    fun list(): List<SourceRule>

    @Query("SELECT * FROM SOURCE_RULE WHERE REMOTE_URL IS NOT NULL AND REMOTE_URL != ''")
    fun listRemote(): List<SourceRule>

    @Query("SELECT * FROM SOURCE_RULE WHERE TYPE = :type LIMIT 1")
    fun load(type: Int): SourceRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(rule: SourceRule): Long

    @Update
    fun update(rule: SourceRule)

    @Query("DELETE FROM SOURCE_RULE WHERE TYPE = :type")
    fun deleteByType(type: Int)
}
