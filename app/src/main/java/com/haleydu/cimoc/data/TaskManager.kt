package com.haleydu.cimoc.data

import com.haleydu.cimoc.db.CimocDatabase
import com.haleydu.cimoc.db.TaskDao
import com.haleydu.cimoc.model.Task
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskManager @Inject constructor(
    private val database: CimocDatabase,
    private val mTaskDao: TaskDao
) {

    fun list(): List<Task> = mTaskDao.list()

    fun listValid(): List<Task> = mTaskDao.listValid()

    fun list(key: Long): List<Task> = mTaskDao.list(key)

    fun insert(task: Task) {
        val id = mTaskDao.insert(task)
        task.id = id
    }

    fun insertInTx(entities: Iterable<Task>) {
        val list = ArrayList<Task>()
        for (entity in entities) {
            list.add(entity)
        }
        mTaskDao.insert(list)
    }

    fun update(task: Task) {
        mTaskDao.update(task)
    }

    fun delete(task: Task) {
        mTaskDao.delete(task)
    }

    fun delete(id: Long) {
        mTaskDao.deleteById(id)
    }

    fun deleteInTx(entities: Iterable<Task>) {
        database.runInTransaction {
            for (entity in entities) {
                mTaskDao.delete(entity)
            }
        }
    }

    fun deleteByComicId(id: Long) {
        mTaskDao.deleteByComicId(id)
    }

    fun insertIfNotExist(entities: Iterable<Task>) {
        database.runInTransaction {
            for (task in entities) {
                if (mTaskDao.load(task.key, task.path) == null) {
                    mTaskDao.insert(task)
                }
            }
        }
    }
}
