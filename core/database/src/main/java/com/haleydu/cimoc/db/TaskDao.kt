package com.haleydu.cimoc.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.haleydu.cimoc.model.Task

@Dao
interface TaskDao {
    @Query("SELECT * FROM TASK")
    fun list(): List<Task>

    @Query("SELECT * FROM TASK WHERE MAX != 0")
    fun listValid(): List<Task>

    @Query("SELECT * FROM TASK WHERE `KEY` = :key")
    fun list(key: Long): List<Task>

    @Query("SELECT * FROM TASK WHERE `KEY` = :key AND PATH = :path LIMIT 1")
    fun load(key: Long, path: String): Task?

    @Insert
    fun insert(task: Task): Long

    @Insert
    fun insert(entities: List<Task>)

    @Update
    fun update(task: Task)

    @Delete
    fun delete(task: Task)

    @Query("DELETE FROM TASK WHERE _id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM TASK WHERE `KEY` = :id")
    fun deleteByComicId(id: Long)
}
