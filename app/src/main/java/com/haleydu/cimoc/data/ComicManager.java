package com.haleydu.cimoc.data;
import com.haleydu.cimoc.db.CimocDatabase;
import com.haleydu.cimoc.db.ComicDao;
import com.haleydu.cimoc.model.Comic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ComicManager {

    private final CimocDatabase database;
    private ComicDao mComicDao;

    @Inject
    public ComicManager(CimocDatabase database, ComicDao comicDao) {
        this.database = database;
        mComicDao = comicDao;
    }

    public void runInTx(Runnable runnable) {
        database.runInTransaction(runnable);
    }

    public <T> T callInTx(Callable<T> callable) {
        return database.runInTransaction(callable);
    }

    public List<Comic> listDownload() {
        return mComicDao.listDownload();
    }

    public List<Comic> listLocal() {
        return mComicDao.listLocal();
    }

    public List<Comic> listFavorite() {
        return mComicDao.listFavorite();
    }

    public List<Comic> listFavoriteOrdered() {
        return mComicDao.listFavoriteOrdered();
    }

    public androidx.paging.PagingSource<Integer, Comic> pagingFavorite() {
        return mComicDao.pagingFavorite();
    }

    public androidx.paging.PagingSource<Integer, Comic> pagingHistory() {
        return mComicDao.pagingHistory();
    }

    public List<Comic> listFinish() {
        return mComicDao.listFinish();
    }

    public List<Comic> listContinue() {
        return mComicDao.listContinue();
    }

    public List<Comic> listHistory() {
        return mComicDao.listHistory();
    }

    public List<Comic> listDownloadOrdered() {
        return mComicDao.listDownloadOrdered();
    }

    public List<Comic> listFavoriteByTag(long id) {
        return mComicDao.listFavoriteByTag(id);
    }

    public List<Comic> listFavoriteNotIn(Collection<Long> collections) {
        if (collections == null || collections.isEmpty()) {
            return mComicDao.listFavoriteOrdered();
        }
        return mComicDao.listFavoriteNotIn(new ArrayList<>(collections));
    }

    public List<Comic> listFavoriteOrHistory() {
        return mComicDao.listFavoriteOrHistory();
    }

    public long countBySource(int type) {
        return mComicDao.countBySource(type);
    }

    public Comic load(long id) {
        return mComicDao.load(id);
    }

    public Comic load(int source, String cid) {
        return mComicDao.load(source, cid);
    }

    public Comic loadOrCreate(int source, String cid) {
        Comic comic = load(source, cid);
        return comic == null ? new Comic(source, cid) : comic;
    }

    public Comic loadLast() {
        return mComicDao.loadLast();
    }

    public void cancelHighlight() {
        mComicDao.cancelHighlight();
    }

    public void updateOrInsert(Comic comic) {
        if (comic.getId() == null) {
            insert(comic);
        } else {
            update(comic);
        }
    }

    public void update(Comic comic) {
        mComicDao.update(comic);
    }

    public void insertOrReplace(Comic comic) {
        long id = mComicDao.insertOrReplace(comic);
        comic.setId(id);
    }

    public void updateOrDelete(Comic comic) {
        if (comic.getFavorite() == null && comic.getHistory() == null && comic.getDownload() == null) {
            mComicDao.delete(comic);
            comic.setId(null);
        } else {
            update(comic);
        }
    }

    public void deleteByKey(long key) {
        mComicDao.deleteByKey(key);
    }

    public void insert(Comic comic) {
        long id = mComicDao.insert(comic);
        comic.setId(id);
    }

}
