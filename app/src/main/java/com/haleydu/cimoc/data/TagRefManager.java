package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.db.CimocDatabase;
import com.haleydu.cimoc.db.TagRefDao;
import com.haleydu.cimoc.model.TagRef;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TagRefManager {

    private final CimocDatabase database;
    private TagRefDao mRefDao;

    @Inject
    public TagRefManager(CimocDatabase database, TagRefDao tagRefDao) {
        this.database = database;
        mRefDao = tagRefDao;
    }

    public void runInTx(Runnable runnable) {
        database.runInTransaction(runnable);
    }

    public List<TagRef> listByTag(long tid) {
        return mRefDao.listByTag(tid);
    }

    public List<TagRef> listByComic(long cid) {
        return mRefDao.listByComic(cid);
    }

    public TagRef load(long tid, long cid) {
        return mRefDao.load(tid, cid);
    }

    public long insert(TagRef ref) {
        return mRefDao.insert(ref);
    }

    public void insert(Iterable<TagRef> entities) {
        List<TagRef> list = new ArrayList<>();
        for (TagRef entity : entities) {
            list.add(entity);
        }
        mRefDao.insert(list);
    }

    public void insertInTx(Iterable<TagRef> entities) {
        insert(entities);
    }

    public void deleteByTag(long tid) {
        mRefDao.deleteByTag(tid);
    }

    public void deleteByComic(long cid) {
        mRefDao.deleteByComic(cid);
    }

    public void delete(long tid, long cid) {
        mRefDao.delete(tid, cid);
    }

}
