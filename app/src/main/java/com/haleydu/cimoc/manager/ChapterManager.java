package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.db.ChapterDao;
import com.haleydu.cimoc.db.CimocDatabase;
import com.haleydu.cimoc.model.Chapter;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChapterManager {

    private final CimocDatabase database;
    private ChapterDao mChapterDao;

    @Inject
    public ChapterManager(CimocDatabase database, ChapterDao chapterDao) {
        this.database = database;
        mChapterDao = chapterDao;
    }

    public void runInTx(Runnable runnable) {
        database.runInTransaction(runnable);
    }

    public <T> T callInTx(Callable<T> callable) {
        return database.runInTransaction(callable);
    }

    public List<Chapter> getListChapter(Long sourceComic) {
        return mChapterDao.getListChapter(sourceComic);
    }

    public List<Chapter> getChapter(String path, String title) {
        return mChapterDao.getChapter(path, title);
    }

    public Chapter load(long id) {
        return mChapterDao.load(id);
    }

    public void cancelHighlight() {
        database.comicDao().cancelHighlight();
    }

    public void updateOrInsert(List<Chapter> chapterList) {
        for (Chapter chapter : chapterList) {
            if (chapter.getId() == null) {
                insert(chapter);
            } else {
                update(chapter);
            }
        }
    }

    public void insertOrReplace(List<Chapter> chapterList) {
        for (Chapter chapter : chapterList) {
            if (chapter.getId() != null) {
                mChapterDao.insertOrReplace(chapter);
            }
        }
    }

    public void update(Chapter chapter) {
        if (chapter.getId() != null) {
            mChapterDao.update(chapter);
        }
    }

    public void deleteByKey(long key) {
        mChapterDao.deleteByKey(key);
    }

    public void insert(Chapter chapter) {
        long id = mChapterDao.insert(chapter);
        chapter.setId(id);
    }

}
