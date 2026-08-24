package com.haleydu.cimoc.data;
import com.haleydu.cimoc.db.CimocDatabase;
import com.haleydu.cimoc.db.ImageUrlDao;
import com.haleydu.cimoc.model.ImageUrl;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageUrlManager {

    private final CimocDatabase database;
    private ImageUrlDao mImageUrlDao;

    @Inject
    public ImageUrlManager(CimocDatabase database, ImageUrlDao imageUrlDao) {
        this.database = database;
        mImageUrlDao = imageUrlDao;
    }

    public void runInTx(Runnable runnable) {
        database.runInTransaction(runnable);
    }

    public List<ImageUrl> getListImageUrl(Long comicChapter) {
        return mImageUrlDao.getListImageUrl(comicChapter);
    }

    public ImageUrl load(long id) {
        return mImageUrlDao.load(id);
    }

    public void updateOrInsert(List<ImageUrl> imageUrlList) {
        for (ImageUrl imageurl : imageUrlList) {
            if (imageurl.getId() == null) {
                insert(imageurl);
            } else {
                update(imageurl);
            }
        }
    }

    public void insertOrReplace(List<ImageUrl> imageUrlList) {
        for (ImageUrl imageurl : imageUrlList) {
            if (imageurl.getId() != null) {
                mImageUrlDao.insertOrReplace(imageurl);
            }
        }
    }

    public void update(ImageUrl imageurl) {
        mImageUrlDao.update(imageurl);
    }

    public void deleteByKey(long key) {
        mImageUrlDao.deleteByKey(key);
    }

    public void insert(ImageUrl imageurl) {
        long id = mImageUrlDao.insert(imageurl);
        imageurl.setId(id);
    }

}
