package com.haleydu.cimoc.data;
import com.haleydu.cimoc.db.CimocDatabase;
import com.haleydu.cimoc.db.TaskDao;
import com.haleydu.cimoc.model.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskManager {

    private final CimocDatabase database;
    private TaskDao mTaskDao;

    @Inject
    public TaskManager(CimocDatabase database, TaskDao taskDao) {
        this.database = database;
        mTaskDao = taskDao;
    }

    public List<Task> list() {
        return mTaskDao.list();
    }

    public List<Task> listValid() {
        return mTaskDao.listValid();
    }

    public List<Task> list(long key) {
        return mTaskDao.list(key);
    }

    public void insert(Task task) {
        long id = mTaskDao.insert(task);
        task.setId(id);
    }

    public void insertInTx(Iterable<Task> entities) {
        List<Task> list = new ArrayList<>();
        for (Task entity : entities) {
            list.add(entity);
        }
        mTaskDao.insert(list);
    }

    public void update(Task task) {
        mTaskDao.update(task);
    }

    public void delete(Task task) {
        mTaskDao.delete(task);
    }

    public void delete(long id) {
        mTaskDao.deleteById(id);
    }

    public void deleteInTx(Iterable<Task> entities) {
        database.runInTransaction(() -> {
            for (Task entity : entities) {
                mTaskDao.delete(entity);
            }
        });
    }

    public void deleteByComicId(long id) {
        mTaskDao.deleteByComicId(id);
    }

    public void insertIfNotExist(final Iterable<Task> entities) {
        database.runInTransaction(() -> {
            for (Task task : entities) {
                if (mTaskDao.load(task.getKey(), task.getPath()) == null) {
                    mTaskDao.insert(task);
                }
            }
        });
    }

}
